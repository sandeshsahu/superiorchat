package com.mobile.superiorchat.camouflage.engine

import android.content.Context
import com.mobile.superiorchat.R
import com.mobile.superiorchat.camouflage.models.Profile
import com.mobile.superiorchat.utils.TelephonyUtils

/**
 * The data payload required to build a perfect camouflage notification and decoy.
 */
data class DecoyData(
    val appNameSpoof: String,
    val title: String,
    val text: String,
    val smallIconResId: Int,
    val decoyIntentAction: String,
    val isSilent: Boolean = true
)

/**
 * Unified logic engine that acts as the "Brain" of the Camouflage Engine.
 * It maps a specific [Profile] to its concrete strings, icons, and HTML paths.
 */
object Manager {

    /**
     * Resolves the given profile into a concrete data payload for the Notifier and DecoyActivity.
     */
    fun resolveCamouflage(context: Context, profile: Profile): DecoyData {
        return when (profile) {
            // AOSP Captive Portal
            is Profile.Aosp.CaptivePortal.NoInternet -> DecoyData(
                appNameSpoof = "Android System",
                title = "Sign in to network",
                text = "You may be out of data or require sign-in to access the internet.",
                smallIconResId = R.drawable.ic_settings_24dp,
                decoyIntentAction = android.provider.Settings.ACTION_WIRELESS_SETTINGS
            )

            // Carrier Services
            is Profile.Aosp.CarrierServices -> {
                val carrierName = TelephonyUtils.getCarrierName(context)
                val text = if (profile.isActive) {
                    "$carrierName - Heavy data usage detected"
                } else {
                    "$carrierName - Standard rates apply"
                }
                DecoyData(
                    appNameSpoof = "Android System",
                    title = "Carrier Services",
                    text = text,
                    smallIconResId = R.drawable.ic_settings_24dp,
                    decoyIntentAction = android.provider.Settings.ACTION_WIRELESS_SETTINGS
                )
            }

            // AOSP Settings (Placeholder for future)
            is Profile.Aosp.Settings.StorageFull -> DecoyData(
                appNameSpoof = "Settings",
                title = "Storage space running out",
                text = "Some system functions may not work.",
                smallIconResId = R.drawable.ic_settings_24dp,
                decoyIntentAction = android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            )

            // Samsung (Placeholder)
            is Profile.Samsung.OneUi.DeviceCare -> DecoyData(
                appNameSpoof = "Device Care",
                title = "Battery optimized",
                text = "Background apps were put to sleep to save battery.",
                smallIconResId = R.drawable.ic_settings_24dp,
                decoyIntentAction = android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            )

            // Xiaomi (Placeholder)
            is Profile.Xiaomi.Settings.SyncError -> DecoyData(
                appNameSpoof = "Xiaomi Cloud",
                title = "Sync error",
                text = "Failed to sync gallery.",
                smallIconResId = R.drawable.ic_settings_24dp,
                decoyIntentAction = android.provider.Settings.ACTION_SYNC_SETTINGS
            )
        }
    }
}
