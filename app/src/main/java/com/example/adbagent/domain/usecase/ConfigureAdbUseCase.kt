package com.example.adbagent.domain.usecase

import android.content.Context
import android.net.wifi.WifiManager
import com.example.adbagent.data.datasource.local.datastore.DataStore
import kotlinx.coroutines.flow.first

class ConfigureAdbUseCase(private val dataStore: DataStore, private val ctx: Context) {

    suspend operator fun invoke(): Result<String> {
        val rsa = dataStore.rsa.first()
        val port = dataStore.port.first()
        if (rsa.isBlank()) return Result.failure(Exception("RSA key empty"))

        val addKey = "echo \"${
            rsa.replace(
                "\"",
                "\\\""
            )
        }\" >> /data/misc/adb/adb_keys && chmod 600 /data/misc/adb/adb_keys && chown shell:shell /data/misc/adb/adb_keys"
        val startAdb = "setprop service.adb.tcp.port $port && stop adbd && start adbd"

        if (!RootShell.exec("$addKey && $startAdb")) return Result.failure(Exception("Root exec failed"))

        val ip = getWifiIp(ctx) ?: return Result.failure(Exception("No Wi-Fi IP"))
        return Result.success("$ip:$port")
    }

    private fun getWifiIp(ctx: Context): String? {
        val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ipInt = wm.connectionInfo.ipAddress
        return if (ipInt == 0) null else "%d.%d.%d.%d".format(
            ipInt and 0xff,
            ipInt shr 8 and 0xff,
            ipInt shr 16 and 0xff,
            ipInt shr 24 and 0xff
        )
    }
}