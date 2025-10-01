package com.thatwaz.guesstheemoji.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.ui.ads.AdBannerPlaceholder
import com.thatwaz.guesstheemoji.ui.game.GameViewModel
import com.thatwaz.guesstheemoji.ui.game.PuzzleScreen
import com.thatwaz.guesstheemoji.ui.home.HomeScreen
import com.thatwaz.guesstheemoji.ui.settings.SettingsScreen
import com.thatwaz.guesstheemoji.ui.theme.GuessTheEmojiTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = Prefs(this)

        // Ensure system bars are NOT drawn behind content
        WindowCompat.setDecorFitsSystemWindows(window, true)

        setContent {
            GuessTheEmojiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val nav = rememberNavController()
                    val vm: GameViewModel =
                        viewModel(factory = SimpleVmFactory { GameViewModel(Prefs(this)) })

                    NavHost(navController = nav, startDestination = "home") {
                        composable("home") { HomeScreen(onPlay = { nav.navigate("game") }, onSettings = { nav.navigate("settings") }) }
                        composable("game") {
                            PuzzleScreen(
                                vm = vm,
                                showInterstitial = { /* InterstitialManager.show() */ },
                                BannerAd = { AdBannerPlaceholder() }
                            )
                        }
                        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }, onRemoveAds = { }) }
                    }
                }
            }
        }

    }
}
