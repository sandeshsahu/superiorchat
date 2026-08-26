package com.mobile.superiorchat.camouflage.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import com.mobile.superiorchat.MainActivity
import com.mobile.superiorchat.camouflage.engine.TileUnlockState
import com.mobile.superiorchat.utils.AppLog

class TileActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val currentTime = System.currentTimeMillis()
        val unlockTime = TileUnlockState.lastUnlockTimestamp

        if (unlockTime > 0L && (currentTime - unlockTime < 5000)) {
            // Valid sequence within the last 5 seconds!
            AppLog.log(com.mobile.superiorchat.utils.LogCategory.SYSTEM, "Valid unlock sequence detected. Launching MainActivity.", com.mobile.superiorchat.utils.LogLevel.DEBUG)
            TileUnlockState.resetSession() // Reset the lock and tap counts completely
            
            val isCallActive = com.mobile.superiorchat.core.call.CallManager.callState.value != com.mobile.superiorchat.core.call.CallState.IDLE

            if (isCallActive) {
                // Maximize existing PiP call to fullscreen without terminating the call
                val mainIntent = Intent(this, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("ACTION_MAXIMIZE_PIP", true)
                }
                startActivity(mainIntent)
                finish()
                return
            }

            val targetClass = if (com.mobile.superiorchat.core.AppGraph.prefs.isFakeCrashEnabled) {
                com.mobile.superiorchat.TransparentActivity::class.java
            } else {
                com.mobile.superiorchat.MainActivity::class.java
            }
            val mainIntent = Intent(this, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(mainIntent)
        } else {
            // Snooper detected, or expired timestamp
            AppLog.log(com.mobile.superiorchat.utils.LogCategory.SYSTEM, "Invalid unlock attempt. Launching decoy settings.", com.mobile.superiorchat.utils.LogLevel.DEBUG)
            val decoyIntent = Intent(this, DecoyActivity::class.java).apply {
                putExtra(DecoyActivity.EXTRA_INTENT_ACTION, getString(com.mobile.superiorchat.R.string.camo_intent_action))
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(decoyIntent)
        }
        
        // Immediately kill the Gatekeeper
        finishAndRemoveTask()
    }
}
