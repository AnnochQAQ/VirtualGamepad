package io.github.miuzarte.scrcpyforandroid.gamepad

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

/**
 * 已保存设备的持久化 (DataStore)
 */
private val Context.deviceDataStore by preferencesDataStore(name = "HandyPadDevices")

data class SavedDevice(
    val id: String,
    val name: String,
    val host: String,
    val port: Int,
)

class DeviceStore(private val context: Context) {

    private val key = stringPreferencesKey("devices_json")

    val devices: Flow<List<SavedDevice>> = context.deviceDataStore.data.map { prefs ->
        decode(prefs[key] ?: "[]")
    }

    suspend fun add(device: SavedDevice) {
        context.deviceDataStore.edit { prefs ->
            val current = decode(prefs[key] ?: "[]").toMutableList()
            current.removeAll { it.id == device.id }
            current.add(device)
            prefs[key] = encode(current)
        }
    }

    suspend fun remove(id: String) {
        context.deviceDataStore.edit { prefs ->
            val current = decode(prefs[key] ?: "[]").filterNot { it.id == id }
            prefs[key] = encode(current)
        }
    }

    private fun encode(list: List<SavedDevice>): String {
        val arr = JSONArray()
        list.forEach { d ->
            arr.put(
                JSONObject()
                    .put("id", d.id)
                    .put("name", d.name)
                    .put("host", d.host)
                    .put("port", d.port),
            )
        }
        return arr.toString()
    }

    private fun decode(json: String): List<SavedDevice> = runCatching {
        val arr = JSONArray(json)
        buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                add(
                    SavedDevice(
                        id = o.optString("id"),
                        name = o.optString("name"),
                        host = o.optString("host"),
                        port = o.optInt("port", 5555),
                    ),
                )
            }
        }
    }.getOrDefault(emptyList())
}

