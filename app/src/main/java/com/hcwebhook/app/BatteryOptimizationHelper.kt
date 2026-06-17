package com.hcwebhook.app

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * Helpers for checking and requesting battery-optimization exemption.
 *
 * Samsung, Xiaomi, Huawei, OnePlus, and other OEMs add their own battery
 * management layers on top of Android's stock Doze/App-Standby. This object
 * provides:
 *  - A reliable check for whether the app is already whitelisted.
 *  - The standard Android system dialog to request exemption.
 *  - OEM-specific deep-link intents that take the user directly to the
 *    relevant screen in their device-care / battery app.
 */
object BatteryOptimizationHelper {

    /**
     * Returns true when the OS has granted the app the
     * REQUEST_IGNORE_BATTERY_OPTIMIZATIONS exemption.
     */
    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    /**
     * Returns an intent that opens the standard Android system dialog asking
     * the user to exempt this app from battery optimizations.
     *
     * Requires the REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission to be
     * declared in the manifest (already present via the implicit grant that
     * comes with targeting Android 6+).
     */
    fun buildRequestExemptionIntent(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    /**
     * Returns an OEM-specific intent that navigates directly into the
     * manufacturer's battery / device-care settings for per-app control, or
     * null when no known OEM shortcut exists for the device.
     *
     * Callers should check [Intent.resolveActivity] before starting to avoid
     * ActivityNotFoundException on devices where the component has been renamed.
     */
    fun buildOemDeepLinkIntent(): Intent? {
        return when (Build.MANUFACTURER.lowercase()) {

            "samsung" -> {
                // Samsung One UI → Device Care → Battery → Background usage limits
                // Works on One UI 2.x+ (Android 10+)
                Intent().apply {
                    component = ComponentName(
                        "com.samsung.android.lool",
                        "com.samsung.android.sm.battery.ui.BatteryActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            "xiaomi", "redmi", "poco" -> {
                // MIUI → Battery saver → No restrictions
                Intent("miui.intent.action.POWER_HIDE_MODE_APP_LIST").apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            "huawei", "honor" -> {
                // EMUI / HarmonyOS → Phone Manager → App launch
                Intent().apply {
                    component = ComponentName(
                        "com.huawei.systemmanager",
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            "oneplus" -> {
                // OxygenOS → Battery → Battery optimisation
                Intent().apply {
                    component = ComponentName(
                        "com.oneplus.brickmode",
                        "com.oneplus.brickmode.BrickModeActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            "oppo", "realme" -> {
                // ColorOS → Phone Manager → Privacy permissions → Start in background
                Intent().apply {
                    component = ComponentName(
                        "com.coloros.safecenter",
                        "com.coloros.privacypermissionsentry.PermissionTopActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            "vivo" -> {
                // FunTouch OS → iManager → Battery
                Intent().apply {
                    component = ComponentName(
                        "com.vivo.permissionmanager",
                        "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                    )
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
            }

            else -> null
        }
    }

    /**
     * Returns a user-facing label for the OEM deep-link button, e.g.
     * "Open Device Care" for Samsung, "Open Phone Manager" for Xiaomi.
     */
    fun oemDeepLinkLabel(): String? {
        return when (Build.MANUFACTURER.lowercase()) {
            "samsung" -> "Open Device Care"
            "xiaomi", "redmi", "poco" -> "Open Battery Saver"
            "huawei", "honor" -> "Open Phone Manager"
            "oneplus" -> "Open Battery Settings"
            "oppo", "realme" -> "Open Privacy Permissions"
            "vivo" -> "Open iManager"
            else -> null
        }
    }

    /**
     * Returns true if the device is from a known OEM that ships aggressive
     * battery management beyond stock Android.
     */
    fun isKnownAggressiveOem(): Boolean {
        return when (Build.MANUFACTURER.lowercase()) {
            "samsung", "xiaomi", "redmi", "poco",
            "huawei", "honor", "oneplus", "oppo",
            "realme", "vivo" -> true
            else -> false
        }
    }
}
