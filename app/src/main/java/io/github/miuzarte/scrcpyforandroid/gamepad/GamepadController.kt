package io.github.miuzarte.scrcpyforandroid.gamepad

import io.github.miuzarte.scrcpyforandroid.scrcpy.GamepadInputHandler
import io.github.miuzarte.scrcpyforandroid.services.AppRuntime

class GamepadController {
    private var pressedBits: Int = 0
    private val pressedIds = androidx.compose.runtime.mutableStateSetOf<String>()

    private var leftX = 32768
    private var leftY = 32768
    private var rightX = 32768
    private var rightY = 32768
    private var leftTrigger = 0
    private var rightTrigger = 0

    val currentPressedIds: Set<String> get() = pressedIds

    private val handler: GamepadInputHandler?
        get() = AppRuntime.gamepadHandler

    fun onButtonDown(button: VirtualButtonDef) {
        pressedIds += button.id
        button.hidBits.forEach { pressedBits = pressedBits or it }
        button.axisTargets.forEach { setAxis(it, pressed = true) }
        push()
    }

    fun onButtonUp(button: VirtualButtonDef) {
        pressedIds -= button.id
        button.hidBits.forEach { pressedBits = pressedBits and it.inv() }
        button.axisTargets.forEach { setAxis(it, pressed = false) }
        push()
    }

    private fun setAxis(target: AxisTarget, pressed: Boolean) {
        when (target) {
            AxisTarget.LEFT_TRIGGER -> leftTrigger = if (pressed) 32767 else 0
            AxisTarget.RIGHT_TRIGGER -> rightTrigger = if (pressed) 32767 else 0
        }
    }

    fun onJoystick(axis: JoystickAxis, value: Int) {
        val v = value.coerceIn(0, 65535)
        when (axis) {
            JoystickAxis.LEFT_X -> leftX = v
            JoystickAxis.LEFT_Y -> leftY = v
            JoystickAxis.RIGHT_X -> rightX = v
            JoystickAxis.RIGHT_Y -> rightY = v
        }
        push()
    }

    fun releaseAll() {
        pressedIds.clear()
        pressedBits = 0
        leftX = 32768; leftY = 32768; rightX = 32768; rightY = 32768
        leftTrigger = 0; rightTrigger = 0
        push()
    }

    private fun push() {
        handler?.onVirtualButtons(pressedBits)
        handler?.onVirtualJoystick(leftX, leftY, rightX, rightY, leftTrigger, rightTrigger)
    }
}
