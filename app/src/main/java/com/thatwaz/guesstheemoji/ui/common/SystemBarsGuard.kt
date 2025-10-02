package com.thatwaz.guesstheemoji.ui.common


import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

/** Reasserts “not fullscreen”, shows bars, and sets icon contrast on every recomposition. */
@Composable
fun SystemBarsGuard() {
    val view = LocalView.current
    val activity = LocalContext.current.findActivity() ?: return
    val window = activity.window
    val statusBarColor = MaterialTheme.colorScheme.surface
    val lightIcons = statusBarColor.luminance() < 0.5f // dark surface -> light icons

    SideEffect {
        // Never draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Make bars visible (some SDKs hide them)
        val controller = WindowInsetsControllerCompat(window, view)
        controller.show(
            WindowInsetsCompat.Type.statusBars() or WindowInsetsCompat.Type.navigationBars()
        )
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT

        // Clear any lingering fullscreen flags
        window.clearFlags(
            android.view.WindowManager.LayoutParams.FLAG_FULLSCREEN or
                    android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN)

        // Give the status bar a solid surface color with readable icons
        window.statusBarColor = statusBarColor.toArgb()
        controller.isAppearanceLightStatusBars = !lightIcons
    }
}