package com.example.adbagent.domain.usecase

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.wifi.WifiManager
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
        val rsa  = dataStore.rsa.first()
        val port = dataStore.port.first()

        if (rsa.isNotBlank() && !addKey(rsa)) {
            return Result.failure(Exception("Root exec failed while adding key"))
        }
        if (!startAdb()) {
            return Result.failure(Exception("Root exec failed while starting ADB"))
        }
        val ip = getWifiIp() ?: return Result.failure(Exception("No Wi‑Fi IP"))
        return Result.success("$ip:$port")
    }

    /**
     * Adds RSA public key to /data/misc/adb/adb_keys (root required).
     */
    fun addKey(rsaKey: String): Boolean {
        val cmd = buildString {
            append("echo \"")
            append(rsaKey.replace("\"", "\\\""))
            append("\" >> /data/misc/adb/adb_keys && ")
            append("chmod 600 /data/misc/adb/adb_keys && ")
            append("chown shell:shell /data/misc/adb/adb_keys")
        }
        return RootShell.exec(cmd)
    }

    /**
     * Starts ADB in TCP mode on the given (or persisted) port.
     */
    suspend fun startAdb(portOverride: String? = null): Boolean {
        val p = portOverride?.takeIf { it.isNotBlank() } ?: dataStore.port.first().toString()
        val cmd = "setprop service.adb.tcp.port $p && stop adbd && start adbd"
        return RootShell.exec(cmd)
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
}