package com.mobile.superiorchat.camouflage.models

/**
 * Defines the available camouflage states.
 */
enum class CamoState {
    IDLE,
    ACTIVE_MESSAGE,
    NO_INTERNET,
    API_UNREACHABLE
}

/**
 * Defines the available camouflages.
 */
sealed class Profile {

    // ----------------------------------------------------
    // AOSP (Android Open Source Project / Stock Android)
    // ----------------------------------------------------
    sealed class Aosp : Profile() {
        /**
         * Carrier Services decoy.
         */
        data class CarrierServices(val state: CamoState = CamoState.IDLE) : Aosp()
    }

    // ----------------------------------------------------
    // Custom Apps (Standalone disguise apps)
    // ----------------------------------------------------
    sealed class CustomApp : Profile() {
        /**
         * Weather App decoy.
         */
        data class WeatherApp(
            val state: CamoState = CamoState.IDLE,
            val currentTemp: String = "--",
            val condition: String = "Unknown",
            val location: String = "Local",
            val humidity: String = "--"
        ) : CustomApp()
    }
}
