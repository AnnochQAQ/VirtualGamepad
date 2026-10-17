package io.github.miuzarte.scrcpyforandroid.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.List

import androidx.compose.material.icons.rounded.List

import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import io.github.miuzarte.scrcpyforandroid.gamepad.DeviceStore
import io.github.miuzarte.scrcpyforandroid.gamepad.SavedDevice
import io.github.miuzarte.scrcpyforandroid.scrcpy.ClientOptions
import io.github.miuzarte.scrcpyforandroid.services.AppRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val store = remember { DeviceStore(context) }
    val devices by store.devices.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    var showAdd by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }
    var showLogs by remember { mutableStateOf(false) }

    var showLogs by remember { mutableStateOf(false) }

    var connectingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var connectedTo by remember { mutableStateOf<SavedDevice?>(null) }

    connectedTo?.let { _ ->
        GamepadScreen(onExit = { connectedTo = null })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HandyPad") },
                actions = {
                    IconButton(onClick = { showLogs = true }) {
                        Icon(Icons.Rounded.List, contentDescription = "日志")
                    }
                    IconButton(onClick = { showAbout = true }) {
                        Icon(Icons.Rounded.Info, contentDescription = "关于")
                    }
                },

            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }) {
                Icon(Icons.Rounded.Add, contentDescription = "添加设备")
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (devices.isEmpty()) {
                Text(
                    text = "还没有设备，点右下角 + 添加",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(devices, key = { it.id }) { dev ->
                        val isConnecting = connectingId == dev.id
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(),
                            elevation = CardDefaults.elevatedCardElevation(defaultElevation = 3.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = connectingId == null) {
                                        error = null
                                        connectingId = dev.id
                                        scope.launch {
                                            try {
                                                withContext(Dispatchers.IO) {
                                                    kotlinx.coroutines.withTimeout(30_000L) {
                                                        val session = AppRuntime.peekSessionOrNull()
                                                            ?: error("会话未初始化")
                                                        val ctrl = session.services.connectionController
                                                        ctrl.connectWithTimeout(dev.host, dev.port, 10_000L)
                                                        ctrl.handleAdbConnected(dev.host, dev.port)
                                                        val scrcpy = AppRuntime.scrcpy ?: error("scrcpy 未初始化")
                                                        if (!scrcpy.isStarted()) {
                                                            scrcpy.start(
                                                                ClientOptions(
                                                                    video = false,
                                                                    audio = false,
                                                                    control = true,
                                                                    gamepad = true,
                                                                    cleanup = false,
                                                                ).fix().validate(),
                                                            )
                                                        }
                                                    }
                                                }
                                                connectedTo = dev
                                            } catch (e: Exception) {
                                                error = (e.message ?: e.javaClass.simpleName) +
                                                    " (" + e.javaClass.simpleName + ")"
                                            } finally {
                                                connectingId = null
                                            }
                                        }
                                    }
                                    .padding(20.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            dev.name,
                                            style = MaterialTheme.typography.titleLarge,
                                        )
                                        Text(
                                            "${dev.host}:${dev.port}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        if (isConnecting) {
                                            Text(
                                                "连接中…",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    IconButton(onClick = {
                                        scope.launch { store.remove(dev.id) }
                                    }) {
                                        Icon(Icons.Rounded.Delete, contentDescription = "删除")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            error?.let { msg ->
                Card(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 96.dp, end = 96.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Text(
                        text = "连接失败: $msg",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddDeviceDialog(
            onDismiss = { showAdd = false },
            onConfirm = { dev ->
                scope.launch { store.add(dev) }
                showAdd = false
            },
        )
    }

    if (showAbout) {
        AboutDialog(onDismiss = { showAbout = false })
    }

    if (showLogs) {
        LogDialog(onDismiss = { showLogs = false })
    }


    if (showLogs) {
        LogDialog(onDismiss = { showLogs = false })
    }

}
