package com.mobile.superiorchat.camouflage.models

/**
 * Defines the deeply nested hierarchy of available camouflages.
 * Organized by OEM -> AppCategory -> SpecificProfile.
 */
sealed class Profile {

    // ----------------------------------------------------
    // AOSP (Android Open Source Project / Stock Android)
    // ----------------------------------------------------
    sealed class Aosp : Profile() {
        
        sealed class CaptivePortal : Aosp() {
            /**
             * The generic "Sign in to network" / "No Internet" notification.
             */
            object NoInternet : CaptivePortal()
        }
        
        /**
         * Carrier Services decoy.
         * If isActive is false, displays "Standard rates apply".
         * If isActive is true, displays "Heavy data usage detected".
         */
        data class CarrierServices(val isActive: Boolean = false) : Aosp()

        sealed class Settings : Aosp() {
            /**
             * E.g., "Storage almost full" or "System update available"
             */
            object StorageFull : Settings()
        }
    }

    // ----------------------------------------------------
    // SAMSUNG
    // ----------------------------------------------------
    sealed class Samsung : Profile() {
        sealed class OneUi : Samsung() {
            object DeviceCare : OneUi()
        }
    }

    // ----------------------------------------------------
    // XIAOMI / MIUI
    // ----------------------------------------------------
    sealed class Xiaomi : Profile() {
        sealed class Settings : Xiaomi() {
            object SyncError : Settings()
        }
    }
}
