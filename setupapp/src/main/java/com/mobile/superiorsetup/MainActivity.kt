package com.mobile.superiorsetup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mobile.superiorsetup.theme.SuperiorChatTheme
import com.mobile.superiorsetup.ui.SetupUI

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SuperiorChatTheme {
                SetupUI()
            }
        }
    }
}

