package com.hcwebhook.app

import android.app.Activity
import android.os.Build
import android.widget.Toast

object FlavorUtils {
    val isPlayStore = true

    private const val PLAY_STORE_PACKAGE = "com.android.vending"

    fun verifyPlayStoreInstallation(activity: Activity) {
        if (isInstalledFromPlayStore(activity)) return

        Toast.makeText(
            activity,
            "Please install this version from the Play Store.",
            Toast.LENGTH_LONG
        ).show()
        activity.finishAffinity()
    }

    private fun isInstalledFromPlayStore(activity: Activity): Boolean {
        val packageManager = activity.packageManager
        val packageName = activity.packageName

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val installSourceInfo = packageManager.getInstallSourceInfo(packageName)
            installSourceInfo.installingPackageName == PLAY_STORE_PACKAGE ||
                installSourceInfo.initiatingPackageName == PLAY_STORE_PACKAGE
        } else {
            @Suppress("DEPRECATION")
            packageManager.getInstallerPackageName(packageName) == PLAY_STORE_PACKAGE
        }
    }
}
