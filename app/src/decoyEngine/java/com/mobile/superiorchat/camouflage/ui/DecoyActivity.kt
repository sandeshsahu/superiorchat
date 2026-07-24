package com.mobile.superiorchat.camouflage.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity

class DecoyActivity : ComponentActivity() {

    companion object {
        const val EXTRA_INTENT_ACTION = "extra_intent_action"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val action = intent.getStringExtra(EXTRA_INTENT_ACTION) ?: Settings.ACTION_SETTINGS

        try {
            val settingsIntent = Intent(action).apply {
                if (action.startsWith(packageName)) {
                    setPackage(packageName)
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            if (settingsIntent.resolveActivity(packageManager) != null) {
                startActivity(settingsIntent)
            } else {
                throw Exception("Intent not resolvable on this flavor")
            }
        } catch (e: Exception) {
            // Fallback to main settings if the specific intent doesn't exist on this OEM or flavor
            val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(fallbackIntent)
            Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show()
        }
        
        // Immediately kill this invisible activity and obliterate it from recent tasks
        finishAndRemoveTask()
    }
}
