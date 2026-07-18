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
}
