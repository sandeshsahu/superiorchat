package com.mobile.superiorchat.camouflage.engine

import com.mobile.superiorchat.R
import com.mobile.superiorchat.camouflage.models.CamouflageProfile

/**
 * The data payload required to build a perfect camouflage notification and decoy.
 */
data class CamouflageData(
    val appNameSpoof: String,
    val title: String,
    val text: String,
    val smallIconResId: Int,
    val decoyIntentAction: String,
    val isSilent: Boolean = true
)

/**
 * Unified logic engine that acts as the "Brain" of the Camouflage Engine.
 * It maps a specific [CamouflageProfile] to its concrete strings, icons, and HTML paths.
 */
object CamouflageManager {

    /**
     * Resolves the given profile into a concrete data payload for the Notifier and DecoyActivity.
     */
    fun resolveCamouflage(profile: CamouflageProfile): CamouflageData {
        return when (profile) {
            // AOSP Captive Portal
            is CamouflageProfile.Aosp.CaptivePortal.NoInternet -> CamouflageData(
                appNameSpoof = "Android System",
                title = "Sign in to network",
                text = "You may be out of data or require sign-in to access the internet.",
                smallIconResId = R.drawable.camo_aosp_captiveportal_no_internet,
                decoyIntentAction = android.provider.Settings.ACTION_WIRELESS_SETTINGS
            )

            // AOSP Settings (Placeholder for future)
            is CamouflageProfile.Aosp.Settings.StorageFull -> CamouflageData(
                appNameSpoof = "Settings",
                title = "Storage space running out",
                text = "Some system functions may not work.",
                smallIconResId = R.drawable.camo_aosp_captiveportal_no_internet, // TODO: Replace with real gear icon
                decoyIntentAction = android.provider.Settings.ACTION_INTERNAL_STORAGE_SETTINGS
            )

            // Samsung (Placeholder)
            is CamouflageProfile.Samsung.OneUi.DeviceCare -> CamouflageData(
                appNameSpoof = "Device Care",
                title = "Battery optimized",
                text = "Background apps were put to sleep to save battery.",
                smallIconResId = R.drawable.camo_aosp_captiveportal_no_internet,
                decoyIntentAction = android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS
            )

            // Xiaomi (Placeholder)
            is CamouflageProfile.Xiaomi.Settings.SyncError -> CamouflageData(
                appNameSpoof = "Xiaomi Cloud",
                title = "Sync error",
                text = "Failed to sync gallery.",
                smallIconResId = R.drawable.camo_aosp_captiveportal_no_internet,
                decoyIntentAction = android.provider.Settings.ACTION_SYNC_SETTINGS
            )
        }
    }
}
