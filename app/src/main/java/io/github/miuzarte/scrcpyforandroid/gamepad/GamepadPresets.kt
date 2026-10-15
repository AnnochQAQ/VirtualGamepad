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

    /**
     * 默认布局 (用户实机调优 + 形状 + ABXY 等距)
     */
    fun default(): GamepadLayout {
        val buttons = mutableListOf<VirtualButtonDef>()

        // 左上: ZL (长条) / L (长条)
        buttons += VirtualButtonDef("zl", "ZL", listOf(LB), Anchor.TOP_LEFT, 61f, 40f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 70f, heightDp = 36f)
        buttons += VirtualButtonDef("l", "L", listOf(LB), Anchor.TOP_LEFT, 61f, 103f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 90f, heightDp = 36f)

        // 右上: ZR (长条) / R (长条)
        buttons += VirtualButtonDef("zr", "ZR", listOf(RB), Anchor.TOP_RIGHT, 61f, 40f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 70f, heightDp = 36f)
        buttons += VirtualButtonDef("r", "R", listOf(RB), Anchor.TOP_RIGHT, 61f, 103f,
            shape = ButtonShape.ROUNDED_RECT, widthDp = 90f, heightDp = 36f)

        // 十字键 (左中, 菱形, 等距 40)
        buttons += VirtualButtonDef("dpad_up", "▲", listOf(DUP), Anchor.TOP_LEFT, 204f, 200f, sizeDp = 44f)
        buttons += VirtualButtonDef("dpad_down", "▼", listOf(DDOWN), Anchor.TOP_LEFT, 204f, 280f, sizeDp = 44f)
        buttons += VirtualButtonDef("dpad_left", "◀", listOf(DLEFT), Anchor.TOP_LEFT, 164f, 240f, sizeDp = 44f)
        buttons += VirtualButtonDef("dpad_right", "▶", listOf(DRIGHT), Anchor.TOP_LEFT, 244f, 240f, sizeDp = 44f)

        // ABXY (右中, 菱形, 等距 40) —— 中心 (210, 250)
        buttons += VirtualButtonDef("face_top", "X", listOf(X), Anchor.TOP_RIGHT, 210f, 210f, sizeDp = 54f)
        buttons += VirtualButtonDef("face_right", "A", listOf(A), Anchor.TOP_RIGHT, 250f, 250f, sizeDp = 54f)
        buttons += VirtualButtonDef("face_bottom", "B", listOf(B), Anchor.TOP_RIGHT, 210f, 290f, sizeDp = 54f)
        buttons += VirtualButtonDef("face_left", "Y", listOf(Y), Anchor.TOP_RIGHT, 170f, 250f, sizeDp = 54f)

        // L3 / R3
        buttons += VirtualButtonDef("l3", "L3", listOf(L3), Anchor.BOTTOM_LEFT, 103f, 205f, sizeDp = 38f)
        buttons += VirtualButtonDef("r3", "R3", listOf(R3), Anchor.BOTTOM_RIGHT, 103f, 205f, sizeDp = 38f)

        // 底部中
        buttons += VirtualButtonDef("minus", "−", listOf(MINUS), Anchor.BOTTOM_CENTER, -70f, 60f, sizeDp = 46f)
        buttons += VirtualButtonDef("plus", "+", listOf(PLUS), Anchor.BOTTOM_CENTER, 0f, 60f, sizeDp = 46f)
        buttons += VirtualButtonDef("home", "⌂", listOf(HOME), Anchor.BOTTOM_CENTER, 70f, 60f, sizeDp = 46f)

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

