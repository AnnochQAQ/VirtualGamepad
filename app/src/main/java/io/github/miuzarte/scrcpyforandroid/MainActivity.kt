package io.github.miuzarte.scrcpyforandroid

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.miuzarte.scrcpyforandroid.i18n.LocalizedActivity
import io.github.miuzarte.scrcpyforandroid.pages.MainScreen
import io.github.miuzarte.scrcpyforandroid.scrcpy.Scrcpy
import io.github.miuzarte.scrcpyforandroid.services.AppRuntime
import io.github.miuzarte.scrcpyforandroid.services.AppScreenOn

class MainActivity: LocalizedActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRuntime.init(applicationContext)
        AppScreenOn.register(window)

        AppRuntime.obtainSession(Scrcpy.SessionConfig())

        enableEdgeToEdge()

        setContent {
            MainScreen()
        }
    }

    override fun onDestroy() {
        AppScreenOn.unregister(window)
        if (isFinishing) AppRuntime.releaseSession()
        super.onDestroy()
    }

    override fun dispatchKeyEvent(event: android.view.KeyEvent): Boolean {
        val handler = AppRuntime.gamepadHandler
        if (handler != null && handler.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: android.view.MotionEvent): Boolean {
        val handler = AppRuntime.gamepadHandler
        if (handler != null && handler.handleGenericMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }
}