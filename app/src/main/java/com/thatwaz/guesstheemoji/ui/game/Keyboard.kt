package com.thatwaz.guesstheemoji.ui.game

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun HangmanKeyboard(
    guessed: Set<Char>,
    wrong: Set<Char>,
    enabled: Boolean,
    onLetter: (Char) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    // Contrast that works in light/dark
    val baseBg = cs.surfaceVariant
    val baseFg = cs.onSurface
    val hitBg  = cs.primaryContainer
    val hitFg  = cs.onPrimaryContainer
    val missBg = cs.errorContainer
    val missFg = cs.onErrorContainer

    val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                row.forEach { c ->
                    val used = guessed.contains(c.lowercaseChar())
                    val miss = wrong.contains(c.lowercaseChar())

                    val bg = when {
                        !enabled -> baseBg
                        !used    -> baseBg
                        miss     -> missBg
                        else     -> hitBg
                    }
                    val fg = when {
                        !used    -> baseFg
                        miss     -> missFg
                        else     -> hitFg
                    }

                    // ✅ KEY FIXES:
                    // - defaultMinSize(0.dp) removes Material3's built-in min width
                    // - contentPadding zero keeps text centered in small keys
                    // - fontSize 16.sp stays readable down to ~30dp key width
                    OutlinedButton(
                        onClick = { onLetter(c) },
                        enabled = enabled && !used,
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = bg,
                            contentColor = fg,
                            disabledContainerColor = bg,
                            disabledContentColor = fg.copy(alpha = 0.95f)
                        ),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            width = 1.dp, brush = SolidColor(cs.outlineVariant)
                        ),
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .weight(1f)                         // evenly fill the row
                            .height(44.dp)                      // compact height
                            .defaultMinSize(0.dp, 0.dp)         // <-- remove min width/height
                    ) {
                        Text(
                            text = c.toString(),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}







