package io.github.miuzarte.scrcpyforandroid.pages

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.miuzarte.scrcpyforandroid.gamepad.Anchor
import io.github.miuzarte.scrcpyforandroid.gamepad.ButtonShape
import io.github.miuzarte.scrcpyforandroid.gamepad.GamepadController
import io.github.miuzarte.scrcpyforandroid.gamepad.GamepadLayout
import io.github.miuzarte.scrcpyforandroid.gamepad.GamepadLayoutStore
import io.github.miuzarte.scrcpyforandroid.gamepad.GamepadPresets
import io.github.miuzarte.scrcpyforandroid.gamepad.HidBits
import io.github.miuzarte.scrcpyforandroid.gamepad.JoystickAxis
import io.github.miuzarte.scrcpyforandroid.gamepad.LayoutCollection
import io.github.miuzarte.scrcpyforandroid.gamepad.NamedLayout
import io.github.miuzarte.scrcpyforandroid.gamepad.VirtualButtonDef
import io.github.miuzarte.scrcpyforandroid.gamepad.VirtualJoystickDef
import io.github.miuzarte.scrcpyforandroid.gamepad.VirtualJoystick
import io.github.miuzarte.scrcpyforandroid.gamepad.namedLayoutFromJson
import io.github.miuzarte.scrcpyforandroid.gamepad.namedLayoutToJson
import io.github.miuzarte.scrcpyforandroid.scrcpy.GamepadInputHandler
import io.github.miuzarte.scrcpyforandroid.services.AppRuntime
import io.github.miuzarte.scrcpyforandroid.services.GamepadService

import kotlinx.coroutines.launch

private fun anchorCenterDp(
    anchor: Anchor, wDp: Float, hDp: Float,
    ox: Float, oy: Float, scale: Float,
): Pair<Float, Float> {
    val baseX = when {
        anchor.isLeft -> 0f
        anchor.isRight -> wDp
        else -> wDp / 2f
    }
    val baseY = when {
        anchor.isTop -> 0f
        anchor.isBottom -> hDp
        else -> hDp / 2f
    }
    val signX = if (anchor.isRight) -1f else 1f
    val signY = if (anchor.isBottom) -1f else 1f
    return (baseX + signX * ox * scale) to (baseY + signY * oy * scale)
}

/** 按键尺寸 (宽, 高) */
private fun buttonSizeDp(def: VirtualButtonDef): Pair<Dp, Dp> = when (def.shape) {
    ButtonShape.CIRCLE -> def.sizeDp.dp to def.sizeDp.dp
    ButtonShape.ROUNDED_RECT -> def.widthDp.dp to def.heightDp.dp
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamepadScreen(onExit: () -> Unit) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val store = remember { GamepadLayoutStore(context) }
    var collection by remember { mutableStateOf(LayoutCollection(emptyList(), "")) }

    LaunchedEffect(store) { store.collection.collect { collection = it } }

    var editMode by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showLayoutManager by remember { mutableStateOf(false) }
    var showAddButton by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    var currentWidthDp by remember { mutableStateOf(0f) }
    val sheetState = rememberModalBottomSheetState()

    val draftButtons = remember { mutableStateListOf<VirtualButtonDef>() }
    val draftJoysticks = remember { mutableStateListOf<VirtualJoystickDef>() }

    val activeLayout = collection.active

    fun saveEdit() {
        val cur = activeLayout ?: return
        val updated = cur.copy(
            layout = GamepadLayout(
                skin = cur.layout.skin,
                buttons = draftButtons.toList(),
                joysticks = draftJoysticks.toList(),
                designWidth = if (currentWidthDp > 0f) currentWidthDp else cur.layout.designWidth,
            ),
        )
        val newCollection = collection.copy(
            layouts = collection.layouts.map { if (it.id == cur.id) updated else it },
        )
        scope.launch { store.save(newCollection) }
        collection = newCollection
        editMode = false
    }

    // handler 由前台 Service 持有; 进入手柄页时启动 Service
    // scrcpy 未就绪时不启动, 否则 Service 会创建出 handler = null 且不会重试
    LaunchedEffect(AppRuntime.scrcpy) {
        if (AppRuntime.scrcpy != null) GamepadService.start(context)
    }
    var handler by remember { mutableStateOf(AppRuntime.gamepadService?.handler) }
    LaunchedEffect(AppRuntime.gamepadService) {
        handler = AppRuntime.gamepadService?.handler
    }
    val controller = remember(handler) { GamepadController(handler) }

    DisposableEffect(controller) {
        onDispose { controller.releaseAll() }
    }

    DisposableEffect(activity) {
        val act = activity
        val prev = act?.requestedOrientation
        act?.requestedOrientation = android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        act?.window?.let { w ->
            androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, false)
            androidx.core.view.WindowInsetsControllerCompat(w, w.decorView).apply {
                hide(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        }
        onDispose {
            controller.releaseAll()
            act?.requestedOrientation = prev ?: android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            act?.window?.let { w ->
                androidx.core.view.WindowInsetsControllerCompat(w, w.decorView).show(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                androidx.core.view.WindowCompat.setDecorFitsSystemWindows(w, true)
            }
        }
    }

    // 返回层级: 布局管理 → 设置弹层 → 手柄页 → 主页
    androidx.activity.compose.BackHandler(enabled = true) {
        when {
            showLayoutManager -> showLayoutManager = false
            showSettings -> showSettings = false
            editMode -> editMode = false
            else -> showSettings = true
        }
    }

    if (showLayoutManager) {
        LayoutManagerScreen(
            collection = collection,
            onBack = { showLayoutManager = false },
            onEdit = { nl ->
                draftButtons.clear(); draftButtons.addAll(nl.layout.buttons)
                draftJoysticks.clear(); draftJoysticks.addAll(nl.layout.joysticks)
                collection = collection.copy(activeId = nl.id)
                showLayoutManager = false
                editMode = true
            },
            onCollectionChange = { newCol -> scope.launch { store.save(newCol) }; collection = newCol },
        )
        return
    }

    if (activeLayout == null) {
        Box(Modifier.fillMaxSize().background(Color(0xFF101010)), contentAlignment = Alignment.Center) {
            Text("没有布局", color = Color.White)
        }
        return
    }

    val shownButtons = if (editMode) draftButtons.toList() else activeLayout.layout.buttons
    val shownJoysticks = if (editMode) draftJoysticks.toList() else activeLayout.layout.joysticks

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Color(0xFF101010))) {
        val wDp = maxWidth.value
        val hDp = maxHeight.value
        currentWidthDp = wDp
        if (wDp <= hDp) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("请横屏使用", color = Color.White, fontSize = 24.sp)
            }
            return@BoxWithConstraints
        }

        val scale = if (activeLayout.layout.designWidth > 0f) wDp / activeLayout.layout.designWidth else 1f

        shownButtons.forEachIndexed { idx, def ->
            var editing by remember(def.id) { mutableStateOf(false) }
            val (cx, cy) = anchorCenterDp(def.anchor, wDp, hDp, def.offsetX, def.offsetY, scale)
            val (bw, bh) = buttonSizeDp(def)

            EditableButton(
                def = def, editMode = editMode,
                pressed = controller.currentPressedIds.contains(def.id),
                cx = cx, cy = cy, bw = bw, bh = bh,
                density = density.density,
                onDown = { controller.onButtonDown(def) },
                onUp = { controller.onButtonUp(def) },
                onOpenEditor = { editing = true },
                onMove = { dxDp, dyDp ->
                    val current = draftButtons[idx]
                    val signX = if (current.anchor.isRight) -1f else 1f
                    val signY = if (current.anchor.isBottom) -1f else 1f
                    draftButtons[idx] = current.copy(
                        offsetX = current.offsetX + dxDp * signX / scale,
                        offsetY = current.offsetY + dyDp * signY / scale,
                    )
                },
            )
            if (editing) {
                ElementEditDialog(
                    "编辑按键", def.label,
                    isButton = true,
                    shape = def.shape,
                    sizeDp = def.sizeDp,
                    widthDp = def.widthDp,
                    heightDp = def.heightDp,
                    hidBits = def.hidBits,
                    onDismiss = { editing = false },
                    onConfirm = { l, shape, size, w, h, b ->
                        draftButtons[idx] = draftButtons[idx].copy(
                            label = l, shape = shape, sizeDp = size, widthDp = w, heightDp = h, hidBits = b,
                        )
                        editing = false
                    },
                )
            }
        }

        shownJoysticks.forEachIndexed { idx, jdef ->
            var editing by remember(jdef.id) { mutableStateOf(false) }
            val (cx, cy) = anchorCenterDp(jdef.anchor, wDp, hDp, jdef.offsetX, jdef.offsetY, scale)

            EditableJoystick(
                def = jdef, editMode = editMode,
                cx = cx, cy = cy,
                density = density.density,
                onAxis = { axis, v -> controller.onJoystick(axis, v) },
                onOpenEditor = { editing = true },
                onMove = { dxDp, dyDp ->
                    val current = draftJoysticks[idx]
                    val signX = if (current.anchor.isRight) -1f else 1f
                    val signY = if (current.anchor.isBottom) -1f else 1f
                    draftJoysticks[idx] = current.copy(
                        offsetX = current.offsetX + dxDp * signX / scale,
                        offsetY = current.offsetY + dyDp * signY / scale,
                    )
                },
            )
            if (editing) {
                ElementEditDialog(
                    "编辑摇杆", jdef.label,
                    isButton = false,
                    shape = ButtonShape.CIRCLE,
                    sizeDp = jdef.sizeDp,
                    widthDp = jdef.sizeDp,
                    heightDp = jdef.sizeDp,
                    hidBits = emptyList(),
                    onDismiss = { editing = false },
                    onConfirm = { l, _, s, _, _, _ ->
                        draftJoysticks[idx] = draftJoysticks[idx].copy(label = l, sizeDp = s)
                        editing = false
                    },
                )
            }
        }

        Row(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!editMode) TransparentButton("设置") { showSettings = true }
            else {
                TransparentButton("添加按键") { showAddButton = true }
                TransparentButton("保存") { saveEdit() }
                TransparentButton("取消") { editMode = false }
            }
        }
    }

    if (showAddButton) {
        var label by remember { mutableStateOf("") }
        val selected = remember { mutableStateListOf<Int>() }
        AlertDialog(
            onDismissRequest = { showAddButton = false },
            title = { Text("添加按键") },
            text = {
                Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(label, { label = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Text("触发的键值:")
                    HidBits.options.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(selected.contains(opt.bit), { c -> if (c) selected.add(opt.bit) else selected.remove(opt.bit) })
                            Text(opt.label)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (selected.isNotEmpty()) {
                        draftButtons.add(
                            VirtualButtonDef(java.util.UUID.randomUUID().toString(), label.ifBlank { selected.size.toString() }, selected.toList(), Anchor.CENTER, 0f, 0f, sizeDp = 56f),
                        )
                    }
                    showAddButton = false
                }) { Text("添加") }
            },
            dismissButton = { TextButton(onClick = { showAddButton = false }) { Text("取消") } },
        )
    }

    if (showLogs) {
        LogDialog(onDismiss = { showLogs = false })
    }

    if (showSettings) {
        ModalBottomSheet(onDismissRequest = { showSettings = false }, sheetState = sheetState) {
            Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("设置")
                TextButton(onClick = { showSettings = false; showLayoutManager = true }) { Text("布局管理") }
                TextButton(onClick = { showLogs = true }) { Text("日志") }
                TextButton(onClick = { showSettings = false; onExit() }) { Text("退出手柄") }

            }
        }
    }
}

@Composable
private fun TransparentButton(text: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.background(Color(0x88000000), CircleShape)) {
        Text(text, color = Color.White)
    }
}

@Composable
private fun EditableButton(
    def: VirtualButtonDef, editMode: Boolean, pressed: Boolean,
    cx: Float, cy: Float, bw: Dp, bh: Dp, density: Float,
    onDown: () -> Unit, onUp: () -> Unit, onOpenEditor: () -> Unit,
    onMove: (Float, Float) -> Unit,
) {
    val xDp = (cx.dp - bw / 2)
    val yDp = (cy.dp - bh / 2)
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "btnScale")
    val shape = if (def.shape == ButtonShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .width(bw)
            .height(bh)
            .scale(scale)
            .background(if (pressed) Color(0xFF4CAF50) else Color(0x66FFFFFF), shape)
            .then(
                Modifier.pointerInput(def.id, editMode) {
                    if (editMode) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount.x / density, dragAmount.y / density)
                        }
                    } else {
                        detectTapGestures(onPress = { onDown(); tryAwaitRelease(); onUp() })
                    }
                },
            )
            .then(
                if (editMode) {
                    Modifier.pointerInput(def.id) {
                        detectTapGestures(onTap = { onOpenEditor() })
                    }
                } else Modifier,
            ),
        contentAlignment = Alignment.Center,
    ) { Text(def.label, color = Color.Black, fontSize = (minOf(bw.value, bh.value) / 3f).sp) }
}

@Composable
private fun EditableJoystick(
    def: VirtualJoystickDef, editMode: Boolean,
    cx: Float, cy: Float, density: Float,
    onAxis: (JoystickAxis, Int) -> Unit, onOpenEditor: () -> Unit,
    onMove: (Float, Float) -> Unit,
) {
    val xDp = (cx - def.sizeDp / 2f).dp
    val yDp = (cy - def.sizeDp / 2f).dp

    Box(
        modifier = Modifier
            .offset(x = xDp, y = yDp)
            .then(
                Modifier.pointerInput(def.id, editMode) {
                    if (editMode) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onMove(dragAmount.x / density, dragAmount.y / density)
                        }
                    }
                },
            )
            .then(
                if (editMode) {
                    Modifier.pointerInput(def.id) {
                        detectTapGestures(onTap = { onOpenEditor() })
                    }
                } else Modifier,
            ),
    ) {
        if (editMode) {
            Box(
                modifier = Modifier.size(def.sizeDp.dp).background(Color(0x33FFFFFF), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(def.label, color = Color.White, fontSize = 16.sp) }
        } else {
            VirtualJoystick(def = def, onAxis = onAxis)
        }
    }
}

@Composable
private fun ElementEditDialog(
    title: String, label: String,
    isButton: Boolean,
    shape: ButtonShape,
    sizeDp: Float, widthDp: Float, heightDp: Float,
    hidBits: List<Int>,
    onDismiss: () -> Unit,
    onConfirm: (String, ButtonShape, Float, Float, Float, List<Int>) -> Unit,
) {
    var editLabel by remember { mutableStateOf(label) }
    var editShape by remember { mutableStateOf(shape) }
    var editSize by remember { mutableStateOf(sizeDp) }
    var editWidth by remember { mutableStateOf(widthDp) }
    var editHeight by remember { mutableStateOf(heightDp) }
    val selected = remember { mutableStateListOf<Int>().apply { addAll(hidBits) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(editLabel, { editLabel = it }, label = { Text("名称") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (isButton) {
                    Text("形状")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { editShape = ButtonShape.CIRCLE },
                            modifier = Modifier.background(
                                if (editShape == ButtonShape.CIRCLE) Color(0x33000000) else Color.Transparent,
                            ),
                        ) { Text("圆形") }
                        TextButton(
                            onClick = { editShape = ButtonShape.ROUNDED_RECT },
                            modifier = Modifier.background(
                                if (editShape == ButtonShape.ROUNDED_RECT) Color(0x33000000) else Color.Transparent,
                            ),
                        ) { Text("长条") }
                    }
                }

                if (editShape == ButtonShape.CIRCLE) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("大小")
                        Slider(editSize, { editSize = it }, valueRange = 32f..120f, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("宽")
                        Slider(editWidth, { editWidth = it }, valueRange = 40f..160f, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("高")
                        Slider(editHeight, { editHeight = it }, valueRange = 24f..80f, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    }
                }

                if (hidBits.isNotEmpty() || isButton) {
                    Text("触发的键值:")
                    HidBits.options.forEach { opt ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(selected.contains(opt.bit), { c -> if (c) selected.add(opt.bit) else selected.remove(opt.bit) })
                            Text(opt.label)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(editLabel, editShape, editSize, editWidth, editHeight, selected.toList())
            }) { Text("确定") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun LayoutManagerScreen(
    collection: LayoutCollection,
    onBack: () -> Unit,
    onEdit: (NamedLayout) -> Unit,
    onCollectionChange: (LayoutCollection) -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var renaming by remember { mutableStateOf<NamedLayout?>(null) }
    var deleting by remember { mutableStateOf<NamedLayout?>(null) }
    var resetting by remember { mutableStateOf<NamedLayout?>(null) }
    var exporting by remember { mutableStateOf<NamedLayout?>(null) }
    var showImport by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().background(Color(0xFF101010)).verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("布局管理", color = Color.White, fontSize = 20.sp, modifier = Modifier.weight(1f))
            TextButton(onClick = onBack) { Text("返回", color = Color.White) }
        }

        collection.layouts.forEach { nl ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((if (nl.id == collection.activeId) "● " else "") + nl.name, modifier = Modifier.weight(1f), color = Color.White)
                TextButton(onClick = { onEdit(nl) }) { Text("编辑", color = Color.White) }
                TextButton(onClick = { onCollectionChange(collection.copy(activeId = nl.id)) }) { Text("切换", color = Color.White) }
                TextButton(onClick = { renaming = nl }) { Text("改名", color = Color.White) }
                TextButton(onClick = { exporting = nl }) { Text("导出", color = Color.White) }
                if (!nl.isPreset) TextButton(onClick = { deleting = nl }) { Text("删除", color = Color.White) }
                TextButton(onClick = { resetting = nl }) { Text("重置", color = Color.White) }
            }
        }

        TextButton(onClick = {
            val newLayout = GamepadPresets.blank("新布局 ${collection.layouts.size + 1}")
            onCollectionChange(collection.copy(layouts = collection.layouts + newLayout, activeId = newLayout.id))
        }) { Text("新建空白布局", color = Color.White) }

        TextButton(onClick = {
            val preset = NamedLayout("preset_default_${System.currentTimeMillis()}", "默认布局 ${collection.layouts.size + 1}", GamepadPresets.default(), false)
            onCollectionChange(collection.copy(layouts = collection.layouts + preset, activeId = preset.id))
        }) { Text("新建默认布局", color = Color.White) }

        TextButton(onClick = { showImport = true }) { Text("导入 JSON", color = Color.White) }
    }

    exporting?.let { nl ->
        val json = remember(nl.id) { namedLayoutToJson(nl).toString(2) }
        AlertDialog(
            onDismissRequest = { exporting = null },
            title = { Text("导出布局: ${nl.name}") },
            text = { OutlinedTextField(json, {}, readOnly = true, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) },
            confirmButton = { TextButton(onClick = { clipboard.setText(AnnotatedString(json)); exporting = null }) { Text("复制") } },
            dismissButton = { TextButton(onClick = { exporting = null }) { Text("关闭") } },
        )
    }

    if (showImport) {
        var text by remember { mutableStateOf("") }
        var err by remember { mutableStateOf<String?>(null) }
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("导入布局") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    OutlinedTextField(text, { text = it }, label = { Text("JSON") }, modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()))
                    err?.let { Text(it, color = Color.Red) }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nl = runCatching { namedLayoutFromJson(org.json.JSONObject(text)) }.getOrNull()
                    if (nl == null) { err = "解析失败" } else {
                        onCollectionChange(collection.copy(layouts = collection.layouts + nl, activeId = nl.id))
                        showImport = false
                    }
                }) { Text("导入") }
            },
            dismissButton = { TextButton(onClick = { showImport = false }) { Text("取消") } },
        )
    }

    renaming?.let { nl ->
        var name by remember(nl.id) { mutableStateOf(nl.name) }
        AlertDialog(
            onDismissRequest = { renaming = null },
            title = { Text("改名") },
            text = { OutlinedTextField(name, { name = it }) },
            confirmButton = {
                TextButton(onClick = {
                    onCollectionChange(collection.copy(layouts = collection.layouts.map { if (it.id == nl.id) it.copy(name = name) else it }))
                    renaming = null
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { renaming = null }) { Text("取消") } },
        )
    }

    deleting?.let { nl ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除布局") },
            text = { Text("删除 \"${nl.name}\"？") },
            confirmButton = {
                TextButton(onClick = {
                    onCollectionChange(collection.copy(layouts = collection.layouts.filterNot { it.id == nl.id }))
                    deleting = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }

    resetting?.let { nl ->
        AlertDialog(
            onDismissRequest = { resetting = null },
            title = { Text("重置布局") },
            text = { Text("把 \"${nl.name}\" 重置成默认?") },
            confirmButton = {
                TextButton(onClick = {
                    onCollectionChange(collection.copy(layouts = collection.layouts.map { if (it.id == nl.id) it.copy(layout = GamepadPresets.default()) else it }))
                    resetting = null
                }) { Text("重置") }
            },
            dismissButton = { TextButton(onClick = { resetting = null }) { Text("取消") } },
        )
    }
}
