package com.thatwaz.guesstheemoji.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.ui.ads.AdBannerPlaceholder
import com.thatwaz.guesstheemoji.ui.ads.FakeInterstitial
import com.thatwaz.guesstheemoji.ui.game.GameViewModel
import com.thatwaz.guesstheemoji.ui.game.PuzzleScreen
import com.thatwaz.guesstheemoji.ui.home.HomeScreen
import com.thatwaz.guesstheemoji.ui.settings.SettingsScreen
import com.thatwaz.guesstheemoji.ui.theme.GuessTheEmojiTheme


class MainActivity : ComponentActivity() {
    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = Prefs(this)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        setContent {
            GuessTheEmojiTheme {
                val nav = rememberNavController()
                val vm: GameViewModel = viewModel(factory = SimpleVmFactory { GameViewModel(prefs) })

                var showFakeAd by remember { mutableStateOf(false) }

                // Global overlay so it appears over any screen
                FakeInterstitial(
                    visible = showFakeAd,
                    onClose = {
                        showFakeAd = false
                        vm.onInterstitialShown(System.currentTimeMillis()) // reset counter/cooldown
                    }
                )

                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            onPlay = { nav.navigate("game") },
                            onSettings = { nav.navigate("settings") }
                        )
                    }
                    composable("game") {
                        PuzzleScreen(
                            vm = vm,
                            showInterstitial = { showFakeAd = true }, // ← trigger test ad
                            BannerAd = { AdBannerPlaceholder() }
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onBack = { nav.popBackStack() },
                            onRemoveAds = { /* disabled in MVP */ }
                        )
                    }
                }
            }
        }
    }
}

//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//
//        val prefs = Prefs(this)
//
//        // Ensure system bars are NOT drawn behind content
//        WindowCompat.setDecorFitsSystemWindows(window, true)
//
//        setContent {
//            GuessTheEmojiTheme {
//                Surface(
//                    modifier = Modifier.fillMaxSize(),
//                    color = MaterialTheme.colorScheme.background
//                ) {
//                    val nav = rememberNavController()
//                    val vm: GameViewModel =
//                        viewModel(factory = SimpleVmFactory { GameViewModel(Prefs(this)) })
//
//                    NavHost(navController = nav, startDestination = "home") {
//                        composable("home") { HomeScreen(onPlay = { nav.navigate("game") }, onSettings = { nav.navigate("settings") }) }
//                        composable("game") {
//                            PuzzleScreen(
//                                vm = vm,
//                                showInterstitial = { /* InterstitialManager.show() */ },
//                                BannerAd = { AdBannerPlaceholder() }
//                            )
//                        }
//                        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }, onRemoveAds = { }) }
//                    }
//                }
//            }
//        }
//
//    }
//}
