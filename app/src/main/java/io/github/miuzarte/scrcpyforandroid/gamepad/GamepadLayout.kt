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

enum class ButtonShape { CIRCLE, ROUNDED_RECT }

enum class GamepadSkin(val displayName: String) { NINTENDO("Nintendo") }

enum class AxisTarget { LEFT_TRIGGER, RIGHT_TRIGGER }

data class VirtualButtonDef(
    val id: String,
    val label: String,
    val hidBits: List<Int>,
    val axisTargets: List<AxisTarget> = emptyList(),
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

sealed class Binding {
    data class Bit(val bit: Int) : Binding()
    data class Axis(val target: AxisTarget) : Binding()
}

data class BindingOption(val label: String, val binding: Binding)

object HidBindings {
    val options: List<BindingOption> = listOf(
        BindingOption("A", Binding.Bit(0x0001)),
        BindingOption("B", Binding.Bit(0x0002)),
        BindingOption("X", Binding.Bit(0x0008)),
        BindingOption("Y", Binding.Bit(0x0010)),
        BindingOption("LB", Binding.Bit(0x0040)),
        BindingOption("RB", Binding.Bit(0x0080)),
        BindingOption("LT", Binding.Axis(AxisTarget.LEFT_TRIGGER)),
        BindingOption("RT", Binding.Axis(AxisTarget.RIGHT_TRIGGER)),
        BindingOption("Back", Binding.Bit(0x0400)),
        BindingOption("Start", Binding.Bit(0x0800)),
        BindingOption("Home", Binding.Bit(0x1000)),
        BindingOption("L3", Binding.Bit(0x2000)),
        BindingOption("R3", Binding.Bit(0x4000)),
        BindingOption("DpadUp", Binding.Bit(0x10000)),
        BindingOption("DpadDown", Binding.Bit(0x20000)),
        BindingOption("DpadLeft", Binding.Bit(0x40000)),
        BindingOption("DpadRight", Binding.Bit(0x80000)),
    )
}
