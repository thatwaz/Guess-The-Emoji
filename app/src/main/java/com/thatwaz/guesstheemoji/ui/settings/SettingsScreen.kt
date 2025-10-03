package com.thatwaz.guesstheemoji.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.thatwaz.guesstheemoji.BuildConfig


@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onRemoveAds: () -> Unit
) {
    val ctx = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)

        Spacer(Modifier.height(12.dp))
        // WIP banner
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.medium
        ) {
            Column(Modifier.padding(12.dp)) {
                Text("Work in progress", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(4.dp))
                Text(
                    "This is an early version. We’d love your feedback and puzzle ideas!",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Feedback
        Button(
            onClick = {
                sendEmail(
                    ctx,
                    to = "brettwaz23@gmail.com",
                    subject = "Guess the Emoji – Feedback",
                    body = feedbackTemplate()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Send Feedback") }

        Spacer(Modifier.height(12.dp))

        // Submit a puzzle idea
        OutlinedButton(
            onClick = {
                sendEmail(
                    ctx,
                    to = "brettwaz23@gmail.com",
                    subject = "Guess the Emoji – Puzzle Idea",
                    body = puzzleTemplate()
                )
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Submit a Puzzle Idea") }

        Spacer(Modifier.height(24.dp))

        // Disabled Remove Ads (MVP)
        Button(
            onClick = { /* disabled */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Remove Ads")
                Text(
                    "(Coming Soon)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Disabled Rate app (Coming Soon)
        Button(
            onClick = { /* disabled */ },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Rate this App")
                Text(
                    "(Coming Soon)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Back") }
    }
}


/* ---------- Helpers ---------- */

private fun sendEmail(
    context: Context,
    to: String,
    subject: String,
    body: String
) {
    val uri = Uri.parse("mailto:")
        .buildUpon()
        .appendQueryParameter("to", to)
        .appendQueryParameter("subject", subject)
        .appendQueryParameter("body", body)
        .build()

    val intent = Intent(Intent.ACTION_SENDTO, uri)
    try {
        context.startActivity(Intent.createChooser(intent, "Send email"))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No email app installed", Toast.LENGTH_SHORT).show()
    }
}

private fun feedbackTemplate(): String = buildString {
    appendLine("Hi, I have some feedback:")
    appendLine()
    appendLine("• What I liked:")
    appendLine("• What confused me:")
    appendLine("• What I’d change:")
    appendLine()
    appendLine("---")
    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
    appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
    appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
}

private fun puzzleTemplate(): String = buildString {
    appendLine("Here’s a puzzle idea:")
    appendLine()
    appendLine("Emojis: ")
    appendLine("Answer: ")
    appendLine("Category (Movies/TV, Food & Drink, Songs & Music, Phrases & Idioms, Animals & Nature): ")
}

private fun openPlayStore(context: Context, packageName: String) {
    val marketIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("market://details?id=$packageName")
    )
    val webIntent = Intent(
        Intent.ACTION_VIEW,
        Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
    )
    try {
        context.startActivity(marketIntent)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(webIntent)
    }
}


