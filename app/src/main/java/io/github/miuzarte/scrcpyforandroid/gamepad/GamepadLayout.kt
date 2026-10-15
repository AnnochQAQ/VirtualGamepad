package io.github.miuzarte.scrcpyforandroid.gamepad

enum class Anchor {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    CENTER_LEFT, CENTER, CENTER_RIGHT,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT;

    val isLeft: Boolean get() = this == TOP_LEFT || this == CENTER_LEFT || this == BOTTOM_LEFT
    val isRight: Boolean get() = this == TOP_RIGHT || this == CENTER_RIGHT || this == BOTTOM_RIGHT
    val isTop: Boolean get() = this == TOP_LEFT || this == TOP_CENTER || this == TOP_RIGHT
    val isBottom: Boolean get() = this == BOTTOM_LEFT || this == BOTTOM_CENTER || this == BOTTOM_RIGHT
}

/** 按键形状 */
enum class ButtonShape { CIRCLE, ROUNDED_RECT }

enum class GamepadSkin(val displayName: String) { NINTENDO("Nintendo") }

/**
 * 虚拟按键
 *
 * CIRCLE: 用 sizeDp (直径)
 * ROUNDED_RECT: 用 widthDp / heightDp
 */
data class VirtualButtonDef(
    val id: String,
    val label: String,
    val hidBits: List<Int>,
    val anchor: Anchor,
    val offsetX: Float,
    val offsetY: Float,
    val shape: ButtonShape = ButtonShape.CIRCLE,
    val sizeDp: Float = 56f,
    val widthDp: Float = 70f,
    val heightDp: Float = 36f,
)

data class VirtualJoystickDef(
    val id: String,
    val label: String,
    val anchor: Anchor,
    val offsetX: Float,
    val offsetY: Float,
    val sizeDp: Float,
    val axisX: JoystickAxis,
    val axisY: JoystickAxis,
)

enum class JoystickAxis { LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y }

data class GamepadLayout(
    val skin: GamepadSkin,
    val buttons: List<VirtualButtonDef>,
    val joysticks: List<VirtualJoystickDef>,
    val designWidth: Float = 0f,
)

data class NamedLayout(
    val id: String,
    val name: String,
    val layout: GamepadLayout,
    val isPreset: Boolean = false,
)

data class LayoutCollection(
    val layouts: List<NamedLayout>,
    val activeId: String,
) {
    val active: NamedLayout? get() = layouts.firstOrNull { it.id == activeId } ?: layouts.firstOrNull()
}

data class HidBitOption(val label: String, val bit: Int)

object HidBits {
    val options: List<HidBitOption> = listOf(
        HidBitOption("A", 0x0001),
        HidBitOption("B", 0x0002),
        HidBitOption("X", 0x0008),
        HidBitOption("Y", 0x0010),
        HidBitOption("LB", 0x0040),
        HidBitOption("RB", 0x0080),
        HidBitOption("Back/-", 0x0400),
        HidBitOption("Start/+", 0x0800),
        HidBitOption("Home", 0x1000),
        HidBitOption("L3", 0x2000),
        HidBitOption("R3", 0x4000),
        HidBitOption("DpadUp", 0x10000),
        HidBitOption("DpadDown", 0x20000),
        HidBitOption("DpadLeft", 0x40000),
        HidBitOption("DpadRight", 0x80000),
    )
}

