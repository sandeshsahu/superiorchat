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
            TileUnlockState.lastUnlockTimestamp = 0L // Reset the lock
            
            val mainIntent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(mainIntent)
        } else {
            // Snooper detected, or expired timestamp
            AppLog.log(com.mobile.superiorchat.utils.LogCategory.SYSTEM, "Invalid unlock attempt. Launching decoy settings.", com.mobile.superiorchat.utils.LogLevel.DEBUG)
            val decoyIntent = Intent(this, DecoyActivity::class.java).apply {
                putExtra(DecoyActivity.EXTRA_INTENT_ACTION, Settings.ACTION_WIRELESS_SETTINGS)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(decoyIntent)
        }
        
        // Immediately kill the Gatekeeper
        finishAndRemoveTask()
    }
}
