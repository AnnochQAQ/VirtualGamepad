package io.github.miuzarte.scrcpyforandroid.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class Credit(val name: String, val desc: String)

private val credits = listOf(
    Credit("scrcpy", "Genymobile/scrcpy"),
    Credit("Scrcpy for Android", "Miuzarte/ScrcpyForAndroid（本项目的基础）"),
    Credit("miuix", "compose-miuix-ui/miuix"),
    Credit("AndroidX / Jetpack Compose", "Google"),
    Credit("Kotlin", "JetBrains"),
    Credit("BoringSSL", "vvb2060/ndk.boringssl"),
    Credit("libcxx", "lsposed/libcxx"),
    Credit("Bouncy Castle", "bcpkix"),
    Credit("Conscrypt", "conscrypt-android"),
    Credit("Reorderable", "sh.calvin.reorderable"),
    Credit("TinyPinyin", "promeg/tinypinyin"),
)

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("关于") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Text("VirtualGamepad")
                Text("虚拟手柄 / 远程控制工具")
                Text("")
                Text("用到的开源项目:")
                credits.forEach { c ->
                    Text("• ${c.name} — ${c.desc}")
                }
                Text("")
                Text("开发者: DeepSeek")
                Text("摸鱼: Amnoch")
                Text("")
                Text("Apache License 2.0")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}
