package com.mobile.superiorchat.bot

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import com.mobile.superiorchat.camouflage.engine.CamouflageNotifier
import com.mobile.superiorchat.camouflage.models.CamouflageProfile

class Notifier(private val context: Context, private val scope: CoroutineScope) {

    fun routeUpdate(update: Update): String? {
        // Completely ignore the message payload. Trigger the Fake Wi-Fi disguise!
        CamouflageNotifier.showTestCamouflage(
            context,
            CamouflageProfile.Aosp.CaptivePortal.NoInternet
        )
        return null
    }
}
