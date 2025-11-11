package com.thatwaz.guesstheemoji.ui.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class InterstitialController(private val activity: Activity) {
    private var ad: InterstitialAd? = null

    fun load(context: Context) {
        InterstitialAd.load(
            context,
            AdsIds.interstitialId(),
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded }
                override fun onAdFailedToLoad(error: LoadAdError) { ad = null }
            }
        )
    }

    fun tryShow(onDismiss: () -> Unit = {}) {
        val current = ad ?: return
        current.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                ad = null
                onDismiss()
            }
            override fun onAdFailedToShowFullScreenContent(e: com.google.android.gms.ads.AdError) {
                ad = null
                onDismiss()
            }
        }
        current.show(activity)
    }
}
