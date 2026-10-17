package io.github.miuzarte.scrcpyforandroid.gamepad

object GamepadPresets {

    private const val A = 0x0001
    private const val B = 0x0002
    private const val X = 0x0008
    private const val Y = 0x0010
    private const val LB = 0x0040
    private const val RB = 0x0080
    private const val MINUS = 0x0400
    private const val PLUS = 0x0800
    private const val HOME = 0x1000
    private const val L3 = 0x2000
    private const val R3 = 0x4000
    private const val DUP = 0x10000
    private const val DDOWN = 0x20000
    private const val DLEFT = 0x40000
    private const val DRIGHT = 0x80000

    fun default(): GamepadLayout {
        val buttons = mutableListOf<VirtualButtonDef>()

        buttons += VirtualButtonDef("zl", "ZL", emptyList(), listOf(AxisTarget.LEFT_TRIGGER),
            Anchor.TOP_LEFT, 61f, 40f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 70f, heightDp = 36f)
        buttons += VirtualButtonDef("l", "L", listOf(LB), emptyList(), Anchor.TOP_LEFT, 61f, 103f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 90f, heightDp = 36f)

        buttons += VirtualButtonDef("zr", "ZR", emptyList(), listOf(AxisTarget.RIGHT_TRIGGER),
            Anchor.TOP_RIGHT, 61f, 40f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 70f, heightDp = 36f)
        buttons += VirtualButtonDef("r", "R", listOf(RB), emptyList(), Anchor.TOP_RIGHT, 61f, 103f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 90f, heightDp = 36f)

        buttons += VirtualButtonDef("dpad_up", "▲", listOf(DUP), emptyList(), Anchor.TOP_LEFT, 204f, 200f, sizeDp = 44f)
        buttons += VirtualButtonDef("dpad_down", "▼", listOf(DDOWN), emptyList(), Anchor.TOP_LEFT, 204f, 280f, sizeDp = 44f)
        buttons += VirtualButtonDef("dpad_left", "◀", listOf(DLEFT), emptyList(), Anchor.TOP_LEFT, 164f, 240f, sizeDp = 44f)
        buttons += VirtualButtonDef("dpad_right", "▶", listOf(DRIGHT), emptyList(), Anchor.TOP_LEFT, 244f, 240f, sizeDp = 44f)

        buttons += VirtualButtonDef("face_top", "X", listOf(X), emptyList(), Anchor.TOP_RIGHT, 210f, 210f, sizeDp = 54f)
        buttons += VirtualButtonDef("face_right", "Y", listOf(Y), emptyList(), Anchor.TOP_RIGHT, 250f, 250f, sizeDp = 54f)
        buttons += VirtualButtonDef("face_bottom", "B", listOf(B), emptyList(), Anchor.TOP_RIGHT, 210f, 290f, sizeDp = 54f)
        buttons += VirtualButtonDef("face_left", "A", listOf(A), emptyList(), Anchor.TOP_RIGHT, 170f, 250f, sizeDp = 54f)

        buttons += VirtualButtonDef("l3", "L3", listOf(L3), emptyList(), Anchor.BOTTOM_LEFT, 103f, 205f, sizeDp = 38f)
        buttons += VirtualButtonDef("r3", "R3", listOf(R3), emptyList(), Anchor.BOTTOM_RIGHT, 103f, 205f, sizeDp = 38f)

        buttons += VirtualButtonDef("minus", "−", listOf(MINUS), emptyList(), Anchor.BOTTOM_CENTER, -70f, 60f, sizeDp = 46f)
        buttons += VirtualButtonDef("plus", "+", listOf(PLUS), emptyList(), Anchor.BOTTOM_CENTER, 0f, 60f, sizeDp = 46f)
        buttons += VirtualButtonDef("home", "⌂", listOf(HOME), emptyList(), Anchor.BOTTOM_CENTER, 70f, 60f, sizeDp = 46f)

        val joysticks = listOf(
            VirtualJoystickDef("left_stick", "L", Anchor.BOTTOM_LEFT, 67f, 85f, 135f, JoystickAxis.LEFT_X, JoystickAxis.LEFT_Y),
            VirtualJoystickDef("right_stick", "R", Anchor.BOTTOM_RIGHT, 67f, 85f, 135f, JoystickAxis.RIGHT_X, JoystickAxis.RIGHT_Y),
        )

        return GamepadLayout(GamepadSkin.NINTENDO, buttons, joysticks, designWidth = 0f)
    }

    fun blank(name: String): NamedLayout {
        val layout = GamepadLayout(
            skin = GamepadSkin.NINTENDO,
            buttons = emptyList(),
            joysticks = listOf(
                VirtualJoystickDef("left_stick", "L", Anchor.BOTTOM_LEFT, 67f, 85f, 135f, JoystickAxis.LEFT_X, JoystickAxis.LEFT_Y),
                VirtualJoystickDef("right_stick", "R", Anchor.BOTTOM_RIGHT, 67f, 85f, 135f, JoystickAxis.RIGHT_X, JoystickAxis.RIGHT_Y),
            ),
            designWidth = 0f,
        )
        return NamedLayout(java.util.UUID.randomUUID().toString(), name, layout, false)
    }
}
