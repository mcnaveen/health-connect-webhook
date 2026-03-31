package com.hcwebhook.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object FlavorUtils {
    fun verifyPlayStoreInstallation(activity: Activity) {
        try {
            val installer = activity.packageManager.getInstallerPackageName(activity.packageName)
            // 'com.android.vending' is the Google Play Store
            if (installer != "com.android.vending") {
                Toast.makeText(
                    activity,
                    "Unlicensed Play Store build. Please download the free FOSS version from GitHub or purchase on Play Store.",
                    Toast.LENGTH_LONG
                ).show()
                
                // Redirect user to the GitHub releases page for the FOSS version
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/mcnaveen/health-connect-webhook/releases"))
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                activity.startActivity(intent)
                
                // Close the app so they can't use the unpaid Play Store version
                activity.finishAffinity()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
