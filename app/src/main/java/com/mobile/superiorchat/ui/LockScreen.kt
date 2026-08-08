package com.mobile.superiorchat.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LockScreen(
    pinLength: Int,
    onUnlock: (String) -> UnlockResult
) {
    var pin by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Shake animation for error
    val shakeOffset = remember { Animatable(0f) }

    LaunchedEffect(pin) {
        if (pin.length >= 4 && !isError) {
            val result = onUnlock(pin)
            if (result == UnlockResult.INVALID && pin.length >= pinLength) { // Auto-verify on max length if invalid
                isError = true
                coroutineScope.launch {
                    shakeOffset.animateTo(15f, animationSpec = tween(50))
                    shakeOffset.animateTo(-15f, animationSpec = tween(50))
                    shakeOffset.animateTo(15f, animationSpec = tween(50))
                    shakeOffset.animateTo(0f, animationSpec = tween(50))
                    delay(300)
                    pin = ""
                    isError = false
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = PrimaryLight,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Enter PIN",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Dynamic Animated PIN Dots (no fixed total dot placeholders)
        Row(
            modifier = Modifier
                .height(24.dp)
                .offset(x = shakeOffset.value.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 0 until pin.length) {
                val color = if (isError) ErrorRed else PrimaryLight
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(64.dp))
        
        // Numpad
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0 until 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 0 until 3) {
                        val number = row * 3 + col + 1
                        NumpadButton(number.toString()) {
                            if (pin.length < 6 && !isError) pin += number.toString()
                        }
                    }
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Empty space for layout balance
                Box(modifier = Modifier.size(72.dp))
                
                NumpadButton("0") {
                    if (pin.length < 6 && !isError) pin += "0"
                }
                
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .bounceClick {
                            if (pin.isNotEmpty() && !isError) pin = pin.dropLast(1)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                        contentDescription = "Backspace",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun NumpadButton(number: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(SurfaceLevel1)
            .bounceClick { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number,
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
