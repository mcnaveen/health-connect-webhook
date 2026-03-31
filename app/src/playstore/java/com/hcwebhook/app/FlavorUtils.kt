package com.hcwebhook.app

import android.app.Activity
import android.widget.Toast

object FlavorUtils {
    val isPlayStore = true

    fun verifyPlayStoreInstallation(activity: Activity) {
        try {
            val installer = activity.packageManager.getInstallerPackageName(activity.packageName)
            // 'com.android.vending' is the Google Play Store
            if (installer != "com.android.vending") {
                Toast.makeText(
                    activity,
                    "Unlicensed Play Store build. Please purchase on Play Store.",
                    Toast.LENGTH_LONG
                ).show()

                // Close the app so they can't use the unpaid Play Store version
                activity.finishAffinity()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
