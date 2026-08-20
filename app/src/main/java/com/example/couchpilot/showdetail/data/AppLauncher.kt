package com.example.couchpilot.showdetail.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppLauncher @Inject constructor() {

    private val providerPackageMap = mapOf(
        "BBC iPlayer" to "uk.co.bbc.iplayer",
        "ITVX" to "com.itv.hub.android",
        "Channel 4" to "com.channel4.ondemand",
        "My5" to "com.five.android",
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
