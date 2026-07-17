package com.mobile.superiorchat.camouflage.engine

import android.content.Context
import com.mobile.superiorchat.R
import com.mobile.superiorchat.camouflage.models.Profile
import com.mobile.superiorchat.camouflage.models.CarrierState
import com.mobile.superiorchat.utils.TelephonyUtils

/**
 * Data class representing the concrete resources needed to build a camouflage.
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
            // Carrier Services
            is Profile.Aosp.CarrierServices -> {
                val carrierName = TelephonyUtils.getCarrierName(context)
                val text = when (profile.state) {
                    CarrierState.IDLE -> "$carrierName - Standard rates apply"
                    CarrierState.ACTIVE_MESSAGE -> "$carrierName - Heavy data usage detected"
                    CarrierState.NO_INTERNET -> "$carrierName - Internet not connected"
                    CarrierState.API_UNREACHABLE -> "$carrierName - You may be out of data, please check your plan"
                }
                
                DecoyData(
                    appNameSpoof = "Android System",
                    title = "Carrier Services",
                    text = text,
                    smallIconResId = R.drawable.ic_settings_24dp,
                    decoyIntentAction = android.provider.Settings.ACTION_WIRELESS_SETTINGS
                )
            }
        }
    }
}
