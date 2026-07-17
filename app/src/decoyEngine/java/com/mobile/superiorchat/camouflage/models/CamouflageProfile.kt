package com.mobile.superiorchat.camouflage.models

/**
 * Defines the deeply nested hierarchy of available camouflages.
 * Organized by OEM -> AppCategory -> SpecificProfile.
 */
sealed class CamouflageProfile {

    // ----------------------------------------------------
    // AOSP (Android Open Source Project / Stock Android)
    // ----------------------------------------------------
    sealed class Aosp : CamouflageProfile() {
        
        sealed class CaptivePortal : Aosp() {
            /**
             * The generic "Sign in to network" / "No Internet" notification.
             */
            object NoInternet : CaptivePortal()
        }

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
    sealed class Samsung : CamouflageProfile() {
        sealed class OneUi : Samsung() {
            object DeviceCare : OneUi()
        }
    }

    // ----------------------------------------------------
    // XIAOMI / MIUI
    // ----------------------------------------------------
    sealed class Xiaomi : CamouflageProfile() {
        sealed class Settings : Xiaomi() {
            object SyncError : Settings()
        }
    }
}
