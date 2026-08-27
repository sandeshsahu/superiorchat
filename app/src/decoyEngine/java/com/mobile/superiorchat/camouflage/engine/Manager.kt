package com.mobile.superiorchat.camouflage.engine

import android.content.Context
import android.app.Activity
import android.content.Intent
import android.provider.Settings
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

    private fun Context.getDynamicString(name: String, vararg formatArgs: Any): String {
        val id = resources.getIdentifier(name, "string", packageName)
        return if (id != 0) getString(id, *formatArgs) else name
    }

    private fun Context.getDynamicStringArray(name: String): Array<String> {
        val id = resources.getIdentifier(name, "array", packageName)
        return if (id != 0) resources.getStringArray(id) else emptyArray()
    }

    /**
     * Resolves the given profile into a concrete data payload for the Notifier and DecoyActivity.
     */
    fun resolveCamouflage(context: Context, profile: Profile): DecoyData {
        return when (profile) {
            is Profile.Aosp.CarrierServices -> {
                val carrierName = TelephonyUtils.getCarrierName(context)
                val text = when (profile.state) {
                    CamoState.IDLE -> context.getDynamicString("camo_state_idle", carrierName)
                    CamoState.ACTIVE_MESSAGE, CamoState.ACTIVE_CALL -> context.getDynamicString("camo_state_active", carrierName)
                    CamoState.NO_INTERNET -> context.getDynamicString("camo_state_no_internet", carrierName)
                    CamoState.API_UNREACHABLE -> context.getDynamicString("camo_state_api_unreachable", carrierName)
                    CamoState.UNINITIALIZED -> context.getDynamicString("camo_state_uninitialized", carrierName)
                }
                
                DecoyData(
                    appNameSpoof = context.getDynamicString("camo_app_name"),
                    title = context.getDynamicString("camo_title"),
                    text = text,
                    smallIconResId = R.drawable.ic_camo_notif,
                    decoyIntentAction = context.getDynamicString("camo_intent_action"),
                    isSilent = (profile.state != CamoState.ACTIVE_MESSAGE && profile.state != CamoState.ACTIVE_CALL)
                )
            }
            is Profile.CustomApp.WeatherApp -> {
                val title = when (profile.state) {
                    CamoState.IDLE -> context.getDynamicString("camo_title_idle", profile.location)
                    CamoState.ACTIVE_MESSAGE -> context.getDynamicString("camo_title_active", profile.location)
                    CamoState.ACTIVE_CALL -> context.getDynamicString("camo_title_active_call", profile.location)
                    CamoState.NO_INTERNET -> context.getDynamicString("camo_title_no_internet")
                    CamoState.API_UNREACHABLE -> context.getDynamicString("camo_title_api_unreachable")
                    CamoState.UNINITIALIZED -> context.getDynamicString("camo_title_uninitialized")
                }

                val text = when (profile.state) {
                    CamoState.IDLE -> context.getDynamicString("camo_state_idle", profile.condition, profile.currentTemp, profile.humidity)
                    CamoState.ACTIVE_MESSAGE -> context.getDynamicString("camo_state_active", profile.condition, profile.currentTemp, profile.humidity)
                    CamoState.ACTIVE_CALL -> context.getDynamicString("camo_state_active_call", profile.condition, profile.currentTemp)
                    CamoState.NO_INTERNET -> context.getDynamicString("camo_state_no_internet", profile.location, profile.condition, profile.currentTemp)
                    CamoState.API_UNREACHABLE -> context.getDynamicString("camo_state_api_unreachable", profile.condition, profile.currentTemp, profile.location)
                    CamoState.UNINITIALIZED -> context.getDynamicString("camo_state_uninitialized")
                }
                
                DecoyData(
                    appNameSpoof = context.getDynamicString("camo_app_name"),
                    title = title,
                    text = text,
                    smallIconResId = R.drawable.ic_camo_notif,
                    decoyIntentAction = context.getDynamicString("camo_intent_action"),
                    isSilent = (profile.state != CamoState.ACTIVE_MESSAGE && profile.state != CamoState.ACTIVE_CALL)
                )
            }
            is Profile.Aosp.PlaySupport -> {
                val titles = context.getDynamicStringArray("camo_idle_titles")
                val texts = context.getDynamicStringArray("camo_idle_texts")
                val randomIndex = if (titles.isNotEmpty()) (System.currentTimeMillis() / 10000 % titles.size).toInt() else 0
                
                val title = when (profile.state) {
                    CamoState.IDLE -> if (titles.isNotEmpty()) titles[randomIndex] else "Play Support"
                    CamoState.ACTIVE_MESSAGE, CamoState.ACTIVE_CALL -> context.getDynamicString("camo_title_active")
                    CamoState.NO_INTERNET -> context.getDynamicString("camo_title_no_internet")
                    CamoState.API_UNREACHABLE -> context.getDynamicString("camo_title_api_unreachable")
                    CamoState.UNINITIALIZED -> context.getDynamicString("camo_title_uninitialized")
                }

                val text = when (profile.state) {
                    CamoState.IDLE -> if (texts.isNotEmpty()) texts[randomIndex] else "Idle"
                    CamoState.ACTIVE_MESSAGE, CamoState.ACTIVE_CALL -> context.getDynamicString("camo_state_active")
                    CamoState.NO_INTERNET -> context.getDynamicString("camo_state_no_internet")
                    CamoState.API_UNREACHABLE -> context.getDynamicString("camo_state_api_unreachable")
                    CamoState.UNINITIALIZED -> context.getDynamicString("camo_state_uninitialized")
                }
                val intentAction = if (profile.state == CamoState.ACTIVE_MESSAGE || profile.state == CamoState.ACTIVE_CALL) {
                    context.getDynamicString("camo_intent_action_active")
                } else {
                    context.getDynamicString("camo_intent_action")
                }
                
                DecoyData(
                    appNameSpoof = context.getDynamicString("camo_app_name"),
                    title = title,
                    text = text,
                    smallIconResId = R.drawable.ic_qs_tile, // We keep static drawable references if they exist, or should we decouple this too? R.drawable.ic_qs_tile must exist in all flavors if used statically.
                    decoyIntentAction = intentAction,
                    isSilent = (profile.state != CamoState.ACTIVE_MESSAGE && profile.state != CamoState.ACTIVE_CALL)
                )
            }
        }
    }

    // launchDecoy method has been removed as the universal decoy UI is now handled in AppNav.kt via DecoyGalleryScreen
}
