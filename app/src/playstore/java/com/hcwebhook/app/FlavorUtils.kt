package com.hcwebhook.app

import android.app.Activity
import android.provider.Settings
import android.widget.Toast
import com.google.android.vending.licensing.AESObfuscator
import com.google.android.vending.licensing.LicenseChecker
import com.google.android.vending.licensing.LicenseCheckerCallback
import com.google.android.vending.licensing.ServerManagedPolicy

object FlavorUtils {
    val isPlayStore = true

    // Random salt — do not change after publishing or cached licenses will break
    private val SALT = byteArrayOf(
        -46, 65, 30, -128, -103, -57, 74, -64, 51, 88,
        -95, -45, 77, -117, -36, -113, -11, 32, -64, 89
    )

    fun verifyPlayStoreInstallation(activity: Activity) {
        val deviceId = Settings.Secure.getString(activity.contentResolver, Settings.Secure.ANDROID_ID)
        val obfuscator = AESObfuscator(SALT, activity.packageName, deviceId)
        val policy = ServerManagedPolicy(activity, obfuscator)
        val checker = LicenseChecker(activity, policy, BuildConfig.PLAY_LICENSE_KEY)

        checker.checkAccess(object : LicenseCheckerCallback {
            override fun allow(reason: Int) {
                checker.onDestroy()
            }

            override fun dontAllow(reason: Int) {
                activity.runOnUiThread {
                    Toast.makeText(
                        activity,
                        "Unlicensed. Please purchase on the Play Store.",
                        Toast.LENGTH_LONG
                    ).show()
                    activity.finishAffinity()
                }
                checker.onDestroy()
            }

            override fun applicationError(errorCode: Int) {
                // Don't block the app for transient errors (e.g. no network)
                checker.onDestroy()
            }
        })
    }
}
