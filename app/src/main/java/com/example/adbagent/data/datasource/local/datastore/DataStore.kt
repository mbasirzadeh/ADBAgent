package com.example.adbagent.data.datasource.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStore(private val ctx: Context) {

    companion object {
        private val Context.dataStore by preferencesDataStore("adb_settings")

        //keys
        val RSA = stringPreferencesKey("rsa_key")
        val PORT = intPreferencesKey("port_key")
    }

    val rsa: Flow<String> = ctx.dataStore.data.map { it[RSA] ?: "" }
    val port: Flow<Int> = ctx.dataStore.data.map { it[PORT] ?: 5555 }

    suspend fun saveRSA(rsa: String) {
        ctx.dataStore.edit { it[RSA] = rsa.trim() }
    }

    suspend fun savePort(port: Int) {
        ctx.dataStore.edit { it[PORT] = port }
    }

}