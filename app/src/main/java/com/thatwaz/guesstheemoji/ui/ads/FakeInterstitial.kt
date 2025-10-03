package com.thatwaz.guesstheemoji.ui.ads

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

@Composable
fun FakeInterstitial(visible: Boolean, onClose: () -> Unit) {
    if (!visible) return
    AlertDialog(
        onDismissRequest = { /* non-cancelable */ },
        title = { Text("Interstitial Ad (Test)") },
        text = { Text("This is a placeholder ad for closed testing.") },
        confirmButton = { TextButton(onClick = onClose) { Text("Close") } }
    )
}
