package com.mobile.superiorchat.camouflage.models

/**
 * Defines the available camouflage states for Carrier Services.
 */
enum class CarrierState {
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
        data class CarrierServices(val state: CarrierState = CarrierState.IDLE) : Aosp()
    }
}
