package com.example.couchpilot.showdetail.data

import android.content.Context
import android.content.Intent
import android.net.Uri
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
        "NOW" to "com.bskyb.nowtv.beta"
    )

    fun launchProviderApp(context: Context, providerName: String) {
        // Unmapped provider - nothing meaningful to open, so bail out here rather than fall
        // through to a "market://details?id=null" / ".../details?id=null" garbage link below.
        val packageName = providerPackageMap[providerName] ?: return

        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent != null) {
            context.startActivity(intent)
            return
        }

        // Fallback: Open Play Store
        val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))

        try {
            context.startActivity(marketIntent)
        } catch (e: Exception) {
            context.startActivity(webIntent)
        }
    }
    
    fun isAppInstalled(context: Context, providerName: String): Boolean {
        val packageName = providerPackageMap[providerName] ?: return false
        return context.packageManager.getLaunchIntentForPackage(packageName) != null
    }
}
