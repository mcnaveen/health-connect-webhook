package com.hcwebhook.app

import android.app.Activity

object FlavorUtils {
    val isPlayStore = false

    @Suppress("UNUSED_PARAMETER")
    fun verifyPlayStoreInstallation(activity: Activity) {
        // No-op for FOSS version. It can be installed from anywhere.
    }
}
