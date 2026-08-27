package com.mobile.superiorchat.camouflage.models

/**
 * Defines the available camouflage states.
 */
enum class CamoState {
    IDLE,
    ACTIVE_MESSAGE,
    ACTIVE_CALL,
    NO_INTERNET,
    API_UNREACHABLE,
    UNINITIALIZED
}

/**
 * Defines the available camouflages.
 */
sealed class Profile {
    abstract val state: CamoState

    // ----------------------------------------------------
    // AOSP (Android Open Source Project / Stock Android)
    // ----------------------------------------------------
    sealed class Aosp : Profile() {
        /**
         * Carrier Services decoy.
         */
        data class CarrierServices(override val state: CamoState = CamoState.IDLE) : Aosp()
        
        /**
         * Google Play background service decoy.
         */
        data class PlaySupport(override val state: CamoState = CamoState.IDLE) : Aosp()
    }

    // ----------------------------------------------------
    // Custom Apps (Standalone disguise apps)
    // ----------------------------------------------------
    sealed class CustomApp : Profile() {
        /**
         * Weather App decoy.
         */
        data class WeatherApp(
            override val state: CamoState = CamoState.IDLE,
            val currentTemp: String = "--",
            val condition: String = "Unknown",
            val location: String = "Local",
            val humidity: String = "--"
        ) : CustomApp()
    }
}
