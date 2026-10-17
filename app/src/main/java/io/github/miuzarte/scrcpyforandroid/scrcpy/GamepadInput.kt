package io.github.miuzarte.scrcpyforandroid.scrcpy

import android.util.Log
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.miuzarte.scrcpyforandroid.services.EventLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

object GamepadHid {
    const val VENDOR_ID = 0x045e
    const val PRODUCT_ID = 0x028e
    const val NAME = "Scrcpy Virtual Gamepad"
    const val REPORT_SIZE = 15
    const val FIRST_ID = 3

    private const val BUTTONS_MASK = 0xFFFF
    const val BIT_DPAD_UP = 0x10000
    const val BIT_DPAD_DOWN = 0x20000
    const val BIT_DPAD_LEFT = 0x40000
    const val BIT_DPAD_RIGHT = 0x80000
    const val DPAD_BITS_MASK = BIT_DPAD_UP or BIT_DPAD_DOWN or BIT_DPAD_LEFT or BIT_DPAD_RIGHT

    fun isGameController(device: InputDevice?): Boolean {
        if (device == null) return false
        return device.supportsSource(InputDevice.SOURCE_GAMEPAD) ||
                device.supportsSource(InputDevice.SOURCE_JOYSTICK)
    }

    val reportDescriptor: ByteArray = intArrayOf(
        0x05, 0x01,
        0x09, 0x05,
        0xA1, 0x01,
        0xA1, 0x00,
        0x05, 0x01,
        0x09, 0x30,
        0x09, 0x31,
        0x09, 0x33,
        0x09, 0x34,
        0x15, 0x00,
        0x27, 0xFF, 0xFF, 0x00, 0x00,
        0x75, 0x10,
        0x95, 0x04,
        0x81, 0x02,
        0x05, 0x01,
        0x09, 0x32,
        0x09, 0x35,
        0x15, 0x00,
        0x26, 0xFF, 0x7F,
        0x75, 0x10,
        0x95, 0x02,
        0x81, 0x02,
        0x05, 0x09,
        0x19, 0x01,
        0x29, 0x10,
        0x15, 0x00,
        0x25, 0x01,
        0x95, 0x10,
        0x75, 0x01,
        0x81, 0x02,
        0x05, 0x01,
        0x09, 0x39,
        0x15, 0x01,
        0x25, 0x08,
        0x75, 0x04,
        0x95, 0x01,
        0x81, 0x42,
        0xC0,
        0xC0,
    ).map { it.toByte() }.toByteArray()

    data class Slot(
        var buttons: Int = 0,
        var axisLeftX: Int = 0x8000,
        var axisLeftY: Int = 0x8000,
        var axisRightX: Int = 0x8000,
        var axisRightY: Int = 0x8000,
        var axisLeftTrigger: Int = 0,
        var axisRightTrigger: Int = 0,
    )

    fun buttonBit(keyCode: Int): Int? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A -> 0x0001
        KeyEvent.KEYCODE_BUTTON_B -> 0x0002
        KeyEvent.KEYCODE_BUTTON_X -> 0x0008
        KeyEvent.KEYCODE_BUTTON_Y -> 0x0010
        KeyEvent.KEYCODE_BUTTON_L1 -> 0x0040
        KeyEvent.KEYCODE_BUTTON_R1 -> 0x0080
        KeyEvent.KEYCODE_BUTTON_SELECT -> 0x0400
        KeyEvent.KEYCODE_BACK -> 0x0400
        KeyEvent.KEYCODE_BUTTON_START -> 0x0800
        KeyEvent.KEYCODE_BUTTON_MODE -> 0x1000
        KeyEvent.KEYCODE_BUTTON_THUMBL -> 0x2000
        KeyEvent.KEYCODE_BUTTON_THUMBR -> 0x4000
        KeyEvent.KEYCODE_DPAD_UP -> BIT_DPAD_UP
        KeyEvent.KEYCODE_DPAD_DOWN -> BIT_DPAD_DOWN
        KeyEvent.KEYCODE_DPAD_LEFT -> BIT_DPAD_LEFT
        KeyEvent.KEYCODE_DPAD_RIGHT -> BIT_DPAD_RIGHT
        else -> null
    }

    fun dpadValue(buttons: Int): Int {
        val up = buttons and BIT_DPAD_UP != 0
        val down = buttons and BIT_DPAD_DOWN != 0
        val left = buttons and BIT_DPAD_LEFT != 0
        val right = buttons and BIT_DPAD_RIGHT != 0
        return when {
            up && left -> 8
            up && right -> 2
            up -> 1
            down && left -> 6
            down && right -> 4
            down -> 5
            left -> 7
            right -> 3
            else -> 0
        }
    }

    fun buildReport(slot: Slot): ByteArray {
        val data = ByteArray(REPORT_SIZE)
        writeLe16(data, 0, slot.axisLeftX)
        writeLe16(data, 2, slot.axisLeftY)
        writeLe16(data, 4, slot.axisRightX)
        writeLe16(data, 6, slot.axisRightY)
        writeLe16(data, 8, slot.axisLeftTrigger)
        writeLe16(data, 10, slot.axisRightTrigger)
        writeLe16(data, 12, slot.buttons and BUTTONS_MASK)
        data[14] = dpadValue(slot.buttons).toByte()
        return data
    }

    fun rescale(value: Float, min: Float, max: Float, outMax: Int): Int {
        if (max <= min) return outMax / 2
        val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
        return (normalized * outMax).roundToInt().coerceIn(0, outMax)
    }

    private fun writeLe16(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value ushr 8) and 0xFF).toByte()
    }
}

class GamepadInputHandler(
    private val scope: CoroutineScope,
    private val deviceName: () -> String,
    private val onUhidCreate: suspend (
        id: Int,
        vendorId: Int,
        productId: Int,
        name: String,
        reportDesc: ByteArray,
    ) -> Unit,
    private val onUhidInput: suspend (id: Int, data: ByteArray) -> Unit,
    private val onUhidDestroy: suspend (id: Int) -> Unit,
    private val onDebugChanged: ((String) -> Unit)? = null,
) {
    private class DeviceState {
        val slot = GamepadHid.Slot()
        var created = false
    }

    private val states = HashMap<Int, DeviceState>()
    private val idByDevice = HashMap<Int, Int>()
    private var nextId = GamepadHid.FIRST_ID

    private val sendMutexes = HashMap<Int, Mutex>()

    private fun mutexFor(deviceId: Int): Mutex =
        synchronized(sendMutexes) { sendMutexes.getOrPut(deviceId) { Mutex() } }

    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (!GamepadHid.isGameController(InputDevice.getDevice(event.deviceId))) return false
        val bit = GamepadHid.buttonBit(event.keyCode) ?: return false
        val deviceId = event.deviceId
        val state = ensureDevice(deviceId) ?: return true
        when (event.action) {
            KeyEvent.ACTION_DOWN -> state.slot.buttons = state.slot.buttons or bit
            KeyEvent.ACTION_UP -> state.slot.buttons = state.slot.buttons and bit.inv()
            else -> return true
        }
        send(deviceId, state)
        return true
    }

    fun handleGenericMotionEvent(event: MotionEvent): Boolean {
        if (!GamepadHid.isGameController(InputDevice.getDevice(event.deviceId))) return false
        val deviceId = event.deviceId
        val state = ensureDevice(deviceId) ?: return true
        val device = InputDevice.getDevice(deviceId)
        val slot = state.slot

        slot.axisLeftX = readAxis(device, event, MotionEvent.AXIS_X, 65535) ?: slot.axisLeftX
        slot.axisLeftY = readAxis(device, event, MotionEvent.AXIS_Y, 65535) ?: slot.axisLeftY
        slot.axisRightX = readAxis(device, event, MotionEvent.AXIS_RX, 65535)
            ?: readAxis(device, event, MotionEvent.AXIS_Z, 65535)
                    ?: slot.axisRightX
        slot.axisRightY = readAxis(device, event, MotionEvent.AXIS_RY, 65535)
            ?: readAxis(device, event, MotionEvent.AXIS_RZ, 65535)
                    ?: slot.axisRightY
        slot.axisLeftTrigger = readAxis(device, event, MotionEvent.AXIS_LTRIGGER, 32767)
            ?: slot.axisLeftTrigger
        slot.axisRightTrigger = readAxis(device, event, MotionEvent.AXIS_RTRIGGER, 32767)
            ?: slot.axisRightTrigger

        val hatX = readRawAxis(device, event, MotionEvent.AXIS_HAT_X)
        val hatY = readRawAxis(device, event, MotionEvent.AXIS_HAT_Y)
        if (hatX != null || hatY != null) {
            val hx = hatX ?: 0f
            val hy = hatY ?: 0f
            slot.buttons = slot.buttons and GamepadHid.DPAD_BITS_MASK.inv()
            if (hy < -0.5f) slot.buttons = slot.buttons or GamepadHid.BIT_DPAD_UP
            if (hy > 0.5f) slot.buttons = slot.buttons or GamepadHid.BIT_DPAD_DOWN
            if (hx < -0.5f) slot.buttons = slot.buttons or GamepadHid.BIT_DPAD_LEFT
            if (hx > 0.5f) slot.buttons = slot.buttons or GamepadHid.BIT_DPAD_RIGHT
        }

        send(deviceId, state)
        return true
    }

    fun onVirtualButtons(bits: Int) {
        val state = ensureDevice(VIRTUAL_DEVICE_ID) ?: return
        state.slot.buttons = bits
        send(VIRTUAL_DEVICE_ID, state)
    }

    fun onVirtualJoystick(
        leftX: Int,
        leftY: Int,
        rightX: Int,
        rightY: Int,
        leftTrigger: Int = 0,
        rightTrigger: Int = 0,
    ) {
        val state = ensureDevice(VIRTUAL_DEVICE_ID) ?: return
        state.slot.axisLeftX = leftX.coerceIn(0, 65535)
        state.slot.axisLeftY = leftY.coerceIn(0, 65535)
        state.slot.axisRightX = rightX.coerceIn(0, 65535)
        state.slot.axisRightY = rightY.coerceIn(0, 65535)
        state.slot.axisLeftTrigger = leftTrigger.coerceIn(0, 32767)
        state.slot.axisRightTrigger = rightTrigger.coerceIn(0, 32767)
        send(VIRTUAL_DEVICE_ID, state)
    }

    fun destroy() {
        val ids = idByDevice.values.toList()
        idByDevice.clear()
        states.clear()
        synchronized(sendMutexes) { sendMutexes.clear() }
        for (id in ids) {
            scope.launch {
                try {
                    onUhidDestroy(id)
                } catch (t: Throwable) {
                    Log.w(TAG, "uhidDestroy failed", t)
                }
            }
        }
    }

    private fun ensureDevice(deviceId: Int): DeviceState? {
        var state = states[deviceId]
        if (state == null) {
            val id = nextId++
            if (id > 0xFFFF) {
                return null
            }
            idByDevice[deviceId] = id
            state = DeviceState()
            states[deviceId] = state
        }
        return state
    }

    private fun send(deviceId: Int, state: DeviceState) {
        val id = idByDevice[deviceId] ?: return
        val report = GamepadHid.buildReport(state.slot)
        EventLogger.logEvent("send: deviceId=$deviceId id=$id created=${state.created}", Log.DEBUG)
        scope.launch {
            mutexFor(deviceId).withLock {
                EventLogger.logEvent("send: acquired lock deviceId=$deviceId id=$id created=${state.created}", Log.DEBUG)
                if (!state.created) {
                    try {
                        onUhidCreate(
                            id,
                            GamepadHid.VENDOR_ID,
                            GamepadHid.PRODUCT_ID,
                            deviceName(),
                            GamepadHid.reportDescriptor,
                        )
                        state.created = true
                        EventLogger.logEvent("uhidCreate ok id=$id", Log.DEBUG)
                    } catch (t: Throwable) {
                        Log.w(TAG, "uhidCreate failed", t)
                        EventLogger.logEvent("uhidCreate failed id=$id: ${t.message}", Log.WARN, t)
                    }
                }
                if (state.created) {
                    try {
                        onUhidInput(id, report)
                        EventLogger.logEvent("uhidInput ok id=$id", Log.DEBUG)
                    } catch (t: Throwable) {
                        Log.w(TAG, "uhidInput failed", t)
                        EventLogger.logEvent("uhidInput failed id=$id: ${t.message}", Log.WARN, t)
                    }
                }
            }
        }
        notifyDebug()
    }

    private fun notifyDebug() {
        val callback = onDebugChanged ?: return
        callback(buildDebug())
    }

    private fun buildDebug(): String {
        if (states.isEmpty()) return ""
        return states.entries
            .sortedBy { it.key }
            .joinToString("\n") { (deviceId, st) ->
                val id = idByDevice[deviceId] ?: return@joinToString ""
                val s = st.slot
                buildString {
                    append("GP#$id")
                    append(" btn=0x").append((s.buttons and 0xFFFF).toString(16).padStart(4, '0'))
                    append(" L=(").append(s.axisLeftX).append(',').append(s.axisLeftY).append(')')
                    append(" R=(").append(s.axisRightX).append(',').append(s.axisRightY).append(')')
                    append(" LT=").append(s.axisLeftTrigger)
                    append(" RT=").append(s.axisRightTrigger)
                    append(" DPAD=").append(GamepadHid.dpadValue(s.buttons))
                }
            }
    }

    private fun readAxis(
        device: InputDevice?,
        event: MotionEvent,
        axis: Int,
        outMax: Int,
    ): Int? {
        val range = device?.let { runCatching { it.getMotionRange(axis) }.getOrNull() }
            ?: return null
        val value = event.getAxisValue(axis)
        val min = range.min
        val max = range.max
        return GamepadHid.rescale(value, min, max, outMax)
    }

    private fun readRawAxis(
        device: InputDevice?,
        event: MotionEvent,
        axis: Int,
    ): Float? {
        val present = device?.let { runCatching { it.getMotionRange(axis) }.isSuccess } ?: false
        return if (present) event.getAxisValue(axis) else null
    }

    private companion object {
        const val TAG = "GamepadInput"
        const val VIRTUAL_DEVICE_ID = -100
    }
}
