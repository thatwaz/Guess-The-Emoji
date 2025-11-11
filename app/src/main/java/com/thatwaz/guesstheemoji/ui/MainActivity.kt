package com.thatwaz.guesstheemoji.ui

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.thatwaz.guesstheemoji.data.Prefs
import com.thatwaz.guesstheemoji.ui.ads.BannerAdView
import com.thatwaz.guesstheemoji.ui.ads.InterstitialController   // 👈 add this import
import com.thatwaz.guesstheemoji.ui.game.GameViewModel
import com.thatwaz.guesstheemoji.ui.game.PuzzleScreen
import com.thatwaz.guesstheemoji.ui.home.HomeScreen
import com.thatwaz.guesstheemoji.ui.settings.SettingsScreen
import com.thatwaz.guesstheemoji.ui.theme.GuessTheEmojiTheme

class MainActivity : ComponentActivity() {

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Lock portrait
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        // Initialize AdMob + whitelist THIS device for test ads
        initAdsForSafeTesting(
            testDeviceIds = listOf("A5208C8EAF5CC09EE0840870C49EB895")
        )

        val prefs = Prefs(this)
        val activity = this@MainActivity                       // 👈 pass activity to controller

        setContent {
            GuessTheEmojiTheme {
                val nav = rememberNavController()
                val vm: GameViewModel = viewModel(factory = SimpleVmFactory { GameViewModel(prefs) })

                // 👇 Real AdMob interstitial controller
                val interstitial = remember { InterstitialController(activity) }
                LaunchedEffect(Unit) { interstitial.load(activity) }

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
                            showInterstitial = {
                                interstitial.tryShow {
                                    // on dismiss: mark shown and preload next
                                    vm.onInterstitialShown(System.currentTimeMillis())
                                    interstitial.load(activity)
                                }
                            },
                            BannerAd = { BannerAdView() }      // real banner composable
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





