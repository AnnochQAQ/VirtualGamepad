package io.github.miuzarte.scrcpyforandroid.gamepad

import io.github.miuzarte.scrcpyforandroid.scrcpy.GamepadInputHandler

class GamepadController(
    private val handler: GamepadInputHandler?,
) {
    private var pressedBits: Int = 0
    private val pressedIds = androidx.compose.runtime.mutableStateSetOf<String>()


    private var leftX = 32768
    private var leftY = 32768
    private var rightX = 32768
    private var rightY = 32768

    val currentPressedIds: Set<String> get() = pressedIds

    fun onButtonDown(button: VirtualButtonDef) {
        pressedIds += button.id
        button.hidBits.forEach { pressedBits = pressedBits or it }
        push()
    }

    fun onButtonUp(button: VirtualButtonDef) {
        pressedIds -= button.id
        button.hidBits.forEach { pressedBits = pressedBits and it.inv() }
        push()
    }

    fun onJoystick(axis: JoystickAxis, value: Int) {
        val v = value.coerceIn(0, 65535)
        when (axis) {
            JoystickAxis.LEFT_X -> leftX = v
            JoystickAxis.LEFT_Y -> leftY = v
            JoystickAxis.RIGHT_X -> rightX = v
            JoystickAxis.RIGHT_Y -> rightY = v
        }
        pushJoystick()
    }

    fun releaseAll() {
        pressedIds.clear()
        pressedBits = 0
        leftX = 32768; leftY = 32768; rightX = 32768; rightY = 32768
        push()
        pushJoystick()
    }

    private fun push() {
        handler?.onVirtualButtons(pressedBits)
    }

    private fun pushJoystick() {
        handler?.onVirtualJoystick(leftX, leftY, rightX, rightY)
    }
}

