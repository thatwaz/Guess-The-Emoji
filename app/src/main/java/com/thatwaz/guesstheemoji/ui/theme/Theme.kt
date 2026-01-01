package com.thatwaz.guesstheemoji.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.thatwaz.guesstheemoji.ui.common.SystemBarsGuard

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF9C27B0),       // candy purple
    secondary = Color(0xFFE91E63),     // hot pink
    tertiary = Color(0xFF80DEEA)       // teal accent if needed
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF7B1FA2),       // slightly deeper purple for legibility
    secondary = Color(0xFFD81B60),
    tertiary = Color(0xFF26C6DA)
)

@Composable
fun GuessTheEmojiTheme(
    forceDark: Boolean? = null,           // ✅ null = follow system, true/false = force
    dynamicColor: Boolean = false,         // keep your existing option
    content: @Composable () -> Unit
) {
    val darkTheme = forceDark ?: isSystemInDarkTheme()

    SystemBarsGuard()

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
