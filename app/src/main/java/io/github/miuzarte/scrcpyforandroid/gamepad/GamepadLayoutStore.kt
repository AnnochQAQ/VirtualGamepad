package io.github.miuzarte.scrcpyforandroid.gamepad

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.gamepadLayoutStore by preferencesDataStore(name = "HandyPadLayout")

class GamepadLayoutStore(private val context: Context) {

    private val key = stringPreferencesKey("collection_json")

    val collection: Flow<LayoutCollection> = context.gamepadLayoutStore.data.map { prefs ->
        val json = prefs[key]
        if (json.isNullOrBlank()) defaultCollection() else decode(json) ?: defaultCollection()
    }

    suspend fun save(collection: LayoutCollection) {
        context.gamepadLayoutStore.edit { prefs -> prefs[key] = encode(collection) }
    }

    private fun defaultCollection(): LayoutCollection {
        val preset = NamedLayout("preset_nintendo", "Default", GamepadPresets.default(), true)
        return LayoutCollection(listOf(preset), preset.id)
    }

    private fun encode(collection: LayoutCollection): String {
        val root = JSONObject()
        root.put("activeId", collection.activeId)
        val arr = JSONArray()
        collection.layouts.forEach { arr.put(namedLayoutToJson(it)) }
        root.put("layouts", arr)
        return root.toString()
    }

    private fun decode(json: String): LayoutCollection? = runCatching {
        val root = JSONObject(json)
        val activeId = root.optString("activeId")
        val arr = root.optJSONArray("layouts") ?: JSONArray()
        val layouts = buildList {
            for (i in 0 until arr.length()) add(namedLayoutFromJson(arr.optJSONObject(i) ?: continue))
        }
        LayoutCollection(layouts, activeId)
    }.getOrNull()
}

internal fun namedLayoutToJson(nl: NamedLayout): JSONObject {
    val o = JSONObject()
    o.put("id", nl.id)
    o.put("name", nl.name)
    o.put("isPreset", nl.isPreset)
    o.put("skin", nl.layout.skin.name)
    o.put("designWidth", nl.layout.designWidth.toDouble())
    val bArr = JSONArray()
    nl.layout.buttons.forEach { b ->
        val axisArr = JSONArray()
        b.axisTargets.forEach { axisArr.put(it.name) }
        bArr.put(JSONObject()
            .put("id", b.id).put("label", b.label)
            .put("hidBits", JSONArray(b.hidBits))
            .put("axisTargets", axisArr)
            .put("anchor", b.anchor.name)
            .put("offsetX", b.offsetX.toDouble())
            .put("offsetY", b.offsetY.toDouble())
            .put("shape", b.shape.name)
            .put("sizeDp", b.sizeDp.toDouble())
            .put("widthDp", b.widthDp.toDouble())
            .put("heightDp", b.heightDp.toDouble()))
    }
    o.put("buttons", bArr)
    val jArr = JSONArray()
    nl.layout.joysticks.forEach { j ->
        jArr.put(JSONObject()
            .put("id", j.id).put("label", j.label)
            .put("anchor", j.anchor.name)
            .put("offsetX", j.offsetX.toDouble())
            .put("offsetY", j.offsetY.toDouble())
            .put("sizeDp", j.sizeDp.toDouble())
            .put("axisX", j.axisX.name).put("axisY", j.axisY.name))
    }
    o.put("joysticks", jArr)
    return o
}

internal fun namedLayoutFromJson(o: JSONObject): NamedLayout {
    val skin = GamepadSkin.valueOf(o.optString("skin", GamepadSkin.NINTENDO.name))
    val buttons = buildList {
        val arr = o.optJSONArray("buttons") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val b = arr.optJSONObject(i) ?: continue
            val bitsArr = b.optJSONArray("hidBits") ?: JSONArray()
            val bits = buildList { for (k in 0 until bitsArr.length()) add(bitsArr.optInt(k)) }
            val axisArr = b.optJSONArray("axisTargets") ?: JSONArray()
            val axisTargets = buildList {
                for (k in 0 until axisArr.length()) {
                    runCatching { AxisTarget.valueOf(axisArr.optString(k)) }.getOrNull()?.let { add(it) }
                }
            }
            add(VirtualButtonDef(
                b.optString("id"), b.optString("label"), bits, axisTargets,
                Anchor.valueOf(b.optString("anchor", Anchor.CENTER.name)),
                b.optDouble("offsetX", 0.0).toFloat(),
                b.optDouble("offsetY", 0.0).toFloat(),
                ButtonShape.valueOf(b.optString("shape", ButtonShape.CIRCLE.name)),
                b.optDouble("sizeDp", 56.0).toFloat(),
                b.optDouble("widthDp", 70.0).toFloat(),
                b.optDouble("heightDp", 36.0).toFloat(),
            ))
        }
    }
    val joysticks = buildList {
        val arr = o.optJSONArray("joysticks") ?: JSONArray()
        for (i in 0 until arr.length()) {
            val j = arr.optJSONObject(i) ?: continue
            add(VirtualJoystickDef(
                j.optString("id"), j.optString("label"),
                Anchor.valueOf(j.optString("anchor", Anchor.BOTTOM_LEFT.name)),
                j.optDouble("offsetX", 67.0).toFloat(),
                j.optDouble("offsetY", 85.0).toFloat(),
                j.optDouble("sizeDp", 135.0).toFloat(),
                JoystickAxis.valueOf(j.optString("axisX", JoystickAxis.LEFT_X.name)),
                JoystickAxis.valueOf(j.optString("axisY", JoystickAxis.LEFT_Y.name)),
            ))
        }
    }
    return NamedLayout(
        o.optString("id", java.util.UUID.randomUUID().toString()),
        o.optString("name", "Imported"),
        GamepadLayout(skin, buttons, joysticks, o.optDouble("designWidth", 0.0).toFloat()),
        false,
    )
}
