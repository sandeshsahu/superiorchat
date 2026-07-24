package com.mobile.superiorchat.camouflage.engine

import android.content.Context
import com.mobile.superiorchat.R
import com.mobile.superiorchat.camouflage.models.Profile
import com.mobile.superiorchat.camouflage.models.CamoState
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
            is Profile.Aosp.CarrierServices -> {
                val carrierName = TelephonyUtils.getCarrierName(context)
                val text = when (profile.state) {
                    CamoState.IDLE -> context.getString(R.string.camo_state_idle, carrierName)
                    CamoState.ACTIVE_MESSAGE -> context.getString(R.string.camo_state_active, carrierName)
                    CamoState.NO_INTERNET -> context.getString(R.string.camo_state_no_internet, carrierName)
                    CamoState.API_UNREACHABLE -> context.getString(R.string.camo_state_api_unreachable, carrierName)
                    CamoState.UNINITIALIZED -> context.getString(R.string.camo_state_uninitialized, carrierName)
                }
                
                DecoyData(
                    appNameSpoof = context.getString(R.string.camo_app_name),
                    title = context.getString(R.string.camo_title),
                    text = text,
                    smallIconResId = R.drawable.ic_camo_notif,
                    decoyIntentAction = context.getString(R.string.camo_intent_action)
                )
            }
            is Profile.CustomApp.WeatherApp -> {
                val title = when (profile.state) {
                    CamoState.IDLE -> context.getString(R.string.camo_title_idle, profile.location)
                    CamoState.ACTIVE_MESSAGE -> context.getString(R.string.camo_title_active, profile.location)
                    CamoState.NO_INTERNET -> context.getString(R.string.camo_title_no_internet)
                    CamoState.API_UNREACHABLE -> context.getString(R.string.camo_title_api_unreachable)
                    CamoState.UNINITIALIZED -> context.getString(R.string.camo_title_uninitialized)
                }

                val text = when (profile.state) {
                    CamoState.IDLE -> context.getString(R.string.camo_state_idle, profile.condition, profile.currentTemp, profile.humidity)
                    CamoState.ACTIVE_MESSAGE -> context.getString(R.string.camo_state_active, profile.condition, profile.currentTemp, profile.humidity)
                    CamoState.NO_INTERNET -> context.getString(R.string.camo_state_no_internet, profile.location, profile.condition, profile.currentTemp)
                    CamoState.API_UNREACHABLE -> context.getString(R.string.camo_state_api_unreachable, profile.condition, profile.currentTemp, profile.location)
                    CamoState.UNINITIALIZED -> context.getString(R.string.camo_state_uninitialized)
                }
                
                DecoyData(
                    appNameSpoof = context.getString(R.string.camo_app_name),
                    title = title,
                    text = text,
                    smallIconResId = R.drawable.ic_camo_notif,
                    decoyIntentAction = context.getString(R.string.camo_intent_action),
                    isSilent = profile.state != CamoState.ACTIVE_MESSAGE
                )
            }
        }
    }
}
