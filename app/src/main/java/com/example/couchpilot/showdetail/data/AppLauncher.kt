package com.example.couchpilot.showdetail.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLauncher @Inject constructor() {

    // Verified against real Play Store listings (roadmap's "Known caveat" from Phase 6 - these
    // were never checked against real installed APKs before). BBC iPlayer, ITVX and My5 were all
    // wrong (uk.co.bbc.iplayer / com.itv.hub.android / com.five.android all 404 on Play Store) -
    // fixed to their real ids. Channel 4, Netflix, Disney Plus, Amazon Prime Video and NOW were
    // already correct.
    private val providerPackageMap = mapOf(
        "BBC iPlayer" to "bbc.iplayer.android",
        "ITVX" to "air.ITVMobilePlayer",
        "Channel 4" to "com.channel4.ondemand",
        "My5" to "com.channel5.my5",
        "Netflix" to "com.netflix.mediaclient",
        "Disney Plus" to "com.disney.disneyplus",
        "Amazon Prime Video" to "com.amazon.avod.thirdpartyclient",
        "NOW" to "com.bskyb.nowtv.beta",
        "Sky Go" to "com.bskyb.skygo"
    )

    private val providerWebMap = mapOf(
        "BBC iPlayer" to "https://www.bbc.co.uk/iplayer/search?q=",
        "ITVX" to "https://www.itv.com/search?q=",
        "Channel 4" to "https://www.channel4.com/search?q=",
        "My5" to "https://www.channel5.com/search?q=",
        "Netflix" to "https://www.netflix.com/search?q=",
        "Disney Plus" to "https://www.disneyplus.com/search?q=",
        "Amazon Prime Video" to "https://www.primevideo.com/search/?phrase=",
        "NOW" to "https://www.nowtv.com/search?q=",
        "Sky Go" to "https://www.sky.com/watch/search?q="
    )

    fun launchProviderApp(
        context: Context,
        providerName: String,
        showName: String? = null,
        fallbackUrl: String? = null
    ) {
        // Unmapped provider - nothing meaningful to open
        val packageName = providerPackageMap[providerName]

        if (packageName != null) {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                context.startActivity(intent)
                return
            }
        }

        // Fallback: Open in web browser if available, else Play Store
        val baseUrl = providerWebMap[providerName]
        val webUrl = when {
            baseUrl != null && showName != null -> baseUrl + URLEncoder.encode(showName, "UTF-8")
            baseUrl != null -> baseUrl
            fallbackUrl != null -> fallbackUrl
            packageName != null -> "https://play.google.com/store/apps/details?id=$packageName"
            else -> return
        }

        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(webUrl))
        context.startActivity(webIntent)
    }
    
    fun isAppInstalled(context: Context, providerName: String): Boolean {
        val packageName = providerPackageMap[providerName] ?: return false
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }
}
