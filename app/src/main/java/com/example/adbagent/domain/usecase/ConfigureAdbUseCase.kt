package com.example.adbagent.domain.usecase

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import com.example.adbagent.data.datasource.local.datastore.DataStore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

class ConfigureAdbUseCase(
    private val ctx: Context,
    private val dataStore: DataStore
) {

    suspend operator fun invoke(): Result<String> {
        val rsa = dataStore.rsa.first();
        val port = dataStore.port.first()
        // 1) اگر کلید داریم و روت در دسترس است → کلید را کپی کنیم.
        if (rsa.isNotBlank()) {
            if (!addKeyIfPossible(rsa))
                return Result.failure(Exception("Cannot add RSA key (need root)"))
        }
        // 2) adbd را در هر حالت ممکن روشن کنیم.
        if (!startAdb())
            return Result.failure(Exception("Could not start ADB (need root or cable)"))

        val ip = getWifiIp() ?: return Result.failure(Exception("No Wi‑Fi IP"))
        return Result.success("$ip:$port")
    }

    /**
     * Adds RSA public key to /data/misc/adb/adb_keys (root required).
     */
    fun addKeyIfPossible(rsaKey: String): Boolean {
        val cmd = "echo \"${
            rsaKey.replace(
                "\"",
                "\\\""
            )
        }\" >> /data/misc/adb/adb_keys && chmod 600 /data/misc/adb/adb_keys && chown shell:shell /data/misc/adb/adb_keys"
        return RootShell.exec(cmd) || !hasSystemPermissions() // اگر روت نیست، صرفاً رد می‌شویم.
    }

    @SuppressLint("PrivateApi")
    suspend fun startAdb(): Boolean {
        // 1) root attempt
        val port = dataStore.port.first()
        val rootOk = RootShell.exec("setprop service.adb.tcp.port $port && stop adbd && start adbd")
        if (rootOk) return true

        // 2) non‑root system‑permission path
        if (!hasSystemPermissions()) return false

        // 2‑A) فعال‌کردن ADB USB → 1
        Settings.Global.putInt(ctx.contentResolver, Settings.Global.ADB_ENABLED, 1)

        // 2‑B) اگر API 31+ و AdbManager در دسترس بود، Wi‑Fi ADB را روشن کنیم.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                val adbMgrCls = Class.forName("android.debug.AdbManager")
                val getService =
                    Context::class.java.getMethod("getSystemService", Class::class.java)
                val adbMgr = getService.invoke(ctx, adbMgrCls)
                val enable = adbMgrCls.getMethod("enableAdbWifi")
                enable.invoke(adbMgr)
            }
        }
        // نمی‌توانیم تضمین کنیم همان پورت ثابت شود؛ اما adbd در حالت Wireless فعال خواهد شد.
        return true
    }

    /**
     * Returns the current Wi‑Fi IP address or null if disconnected.
     */
    fun getWifiIp(): String? {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wm.connectionInfo.ipAddress
        if (ipInt == 0) return null
        return listOf(0, 8, 16, 24)
            .joinToString(".") { idx -> ((ipInt shr idx) and 0xFF).toString() }
    }

    /** Emits current Wi‑Fi IP whenever NETWORK_STATE_CHANGED_ACTION is broadcast. */
    fun wifiIpFlow(): Flow<String?> = callbackFlow {
        val filter = IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, i: Intent?) {
                trySend(getWifiIp())
            }
        }
        ctx.registerReceiver(receiver, filter)
        // emit initial value
        trySend(getWifiIp())
        awaitClose { ctx.unregisterReceiver(receiver) }
    }.distinctUntilChanged()

    private fun hasSystemPermissions(): Boolean =
        ctx.checkSelfPermission("android.permission.MANAGE_DEBUGGING") == PackageManager.PERMISSION_GRANTED &&
                ctx.checkSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == PackageManager.PERMISSION_GRANTED

}