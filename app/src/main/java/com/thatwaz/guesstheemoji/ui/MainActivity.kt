package com.thatwaz.guesstheemoji.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.thatwaz.guesstheemoji.data.Keys
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.ui.ads.BannerAdView
import com.thatwaz.guesstheemoji.ui.ads.InterstitialController
import com.thatwaz.guesstheemoji.ui.game.GameViewModel
import com.thatwaz.guesstheemoji.ui.game.PuzzleScreen
import com.thatwaz.guesstheemoji.ui.game.ScoresScreen
import com.thatwaz.guesstheemoji.ui.home.HomeScreen
import com.thatwaz.guesstheemoji.ui.settings.SettingsScreen
import com.thatwaz.guesstheemoji.ui.theme.GuessTheEmojiTheme

class MainActivity : ComponentActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        initAdsForSafeTesting(
            testDeviceIds = listOf("A5208C8EAF5CC09EE0840870C49EB895")
        )

        val prefs = Prefs(this)
        val activity = this@MainActivity

        setContent {
            val p by prefs.flow.collectAsState(initial = null)
            val themeMode = p?.get(Keys.THEME_MODE) ?: 0 // 0=system, 1=light, 2=dark

            val forceDark: Boolean? = when (themeMode) {
                1 -> false // force light
                2 -> true  // force dark
                else -> null // follow system
            }

            GuessTheEmojiTheme(forceDark = forceDark) {

            val nav = rememberNavController()
                val vm: GameViewModel =
                    viewModel(factory = SimpleVmFactory { GameViewModel(prefs) })

                // safer: tie remember to the activity instance
                val interstitial = remember(activity) { InterstitialController(activity) }
                LaunchedEffect(Unit) { interstitial.load(activity) }

                NavHost(navController = nav, startDestination = "home") {

                    composable("home") {
                        val s by vm.ui.collectAsState()

                        HomeScreen(
                            hasActiveRun = s.hasActiveRun,

                            onContinue = {
                                vm.continueGame()
                                nav.navigate("game")
                            },

                            onNewGame = {
                                vm.startNewRun()          // ✅ use startNewRun (immediate in-memory reset)
                                nav.navigate("game") {
                                    launchSingleTop = true
                                }
                            },

                            onScores = { nav.navigate("scores") },
                            onSettings = { nav.navigate("settings") }
                        )
                    }


                    composable("scores") {
                        ScoresScreen(
                            prefs = prefs,
                            onBack = { nav.popBackStack() },
                            onHome = {
                                nav.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            },
                            onPlayAgain = {
                                vm.startNewRun()
                                nav.navigate("game") {
                                    popUpTo("scores") { inclusive = false }
                                    launchSingleTop = true
                                    restoreState = false
                                }
                            }

                        )
                    }





                    composable("game") {
                        PuzzleScreen(
                            vm = vm,
                            showInterstitial = { onDismiss ->
                                interstitial.tryShow {
                                    interstitial.load(activity)
                                    onDismiss()
                                }
                            },
                            bannerAd = { BannerAdView() },
                            onShowScores = { nav.navigate("scores") },
                            onShowHome = {
                                nav.navigate("home") {
                                    popUpTo("home") { inclusive = false }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            prefs = prefs,
                            onBack = { nav.popBackStack() },
                            onRemoveAds = { /* disabled in MVP */ }
                        )
                    }
                }
            }
        }
    }

    /**
     * Initialize AdMob and force this device to receive TEST ADS.
     * Protects you from invalid/self clicks in dev & QA, even if prod IDs slip in.
     */
    private fun initAdsForSafeTesting(testDeviceIds: List<String>) {
        MobileAds.initialize(this)
        if (testDeviceIds.isNotEmpty()) {
            val config = RequestConfiguration.Builder()
                .setTestDeviceIds(testDeviceIds)
                .build()
            MobileAds.setRequestConfiguration(config)
        }
    }
}






