package com.mobile.superiorutils

import android.Manifest
import com.mobile.superiorutils.utils.LogCategory
import com.mobile.superiorutils.utils.LogLevel
import com.mobile.superiorutils.utils.AppLog
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.mobile.superiorutils.theme.Background
import com.mobile.superiorutils.theme.SuperiorChatTheme
import com.mobile.superiorutils.ui.AppScreen
import com.mobile.superiorutils.ui.MainViewModel

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ -> }

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val isSecretLaunch = intent.getBooleanExtra("isSecretLaunch", false)
        val isWifiLauncher = intent.component?.className == "com.mobile.superiorutils.WifiLauncher"

        // Redirect to camouflage ONLY if the app is launched via the WifiLauncher alias
        if (isWifiLauncher && !isSecretLaunch) {
            val wifiIntent = android.content.Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)
            startActivity(wifiIntent)
            finish()
            return
        }

        AppLog.log(LogCategory.SYSTEM, "MainActivity UI Initialized")
        
        com.mobile.superiorutils.core.ServiceCore.ensureRunning(this)

        setContent {
            SuperiorChatTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Background
                ) {
                    AppScreen(
                        viewModel = viewModel,
                        requestPostNotifications = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                AppLog.log(LogCategory.SYSTEM, "Requesting POST_NOTIFICATIONS permission")
                                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }
            }
        }
    }
}
