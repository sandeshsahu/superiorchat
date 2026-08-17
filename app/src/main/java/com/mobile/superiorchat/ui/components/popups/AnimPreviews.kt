package com.mobile.superiorchat.ui.components.popups

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Phone

import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.unit.sp
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick

@Composable
fun FakeCrashAnimPreview() {
    val infiniteTransition = rememberInfiniteTransition(label = "anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val isPressing = progress in 0.5f..2.0f
    val pressProgress = if (progress < 0.5f) 0f else if (progress > 2.0f) 1f else (progress - 0.5f) / 1.5f
    
    val touchScale = if (isPressing) 1f + (pressProgress * 0.2f) else 1f
    val touchAlpha = if (isPressing) 0.8f else 0f
    
    val rippleAlpha = if (isPressing) (1f - pressProgress) * 0.5f else 0f
    val rippleScale = if (isPressing) 1f + pressProgress else 1f
    
    val showSuccess = progress in 2.2f..3.8f

    val appName = androidx.compose.ui.res.stringResource(id = com.mobile.superiorchat.R.string.app_name)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PREVIEW", fontSize = 11.sp, color = PrimaryLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = SurfaceLevel2,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Column {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .drawBehind {
                                if (pressProgress > 0f && progress < 2.2f) {
                                    drawRect(
                                        color = Color.White.copy(alpha = 0.15f),
                                        size = Size(size.width * pressProgress, size.height)
                                    )
                                }
                                if (progress in 2.0f..2.2f) {
                                    drawRect(
                                        color = PrimaryLight.copy(alpha = 0.3f),
                                        size = size
                                    )
                                }
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$appName keeps stopping",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "A system error caused the application to stop responding.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text("Close app", fontSize = 13.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                    }
                }
                
                if (isPressing || progress in 2.0f..2.2f) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = 60.dp, y = (-4).dp)
                    ) {
                        // Ripple

                        Box(
                            modifier = Modifier
                                .size(40.dp * rippleScale)
                                .align(Alignment.Center)
                                .background(PrimaryLight.copy(alpha = rippleAlpha), androidx.compose.foundation.shape.CircleShape)
                        )
                        // Touch Icon
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = if (progress > 2.0f) 0f else touchAlpha),
                            modifier = Modifier
                                .size(32.dp * touchScale)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
            
            // Success Overlay
            androidx.compose.animation.AnimatedVisibility(
                visible = showSuccess,
                enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f),
                exit = fadeOut(tween(200)),
                modifier = Modifier.matchParentSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xE6000000)), // Dark blur effect
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            color = PrimaryLight,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "App Opened",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            }
        }
    }
}

@Composable
fun TileAccessAnimPreview(isDisabledMode: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "anim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 9f, // 9 seconds loop for slower, readable animation
        animationSpec = infiniteRepeatable(
            animation = tween(9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    // Timeline in seconds:
    // 0.0 - 1.0: Idle
    // 1.0 - 1.3: Press 1 (Enable)
    // 1.3 - 2.3: Idle ON
    // 2.3 - 2.6: Press 2 (Disable)
    // 2.6 - 3.6: Idle OFF
    // 3.6 - 3.9: Press 3 (Enable)
    // 3.9 - 4.9: Idle ON
    // 4.9 - 6.4: Hold (1.5 seconds)
    // 6.4 - 9.0: Success / Failure (2.6 seconds)

    val isPressing = (progress in 1.0f..1.3f) || 
                     (progress in 2.3f..2.6f) || 
                     (progress in 3.6f..3.9f) || 
                     (progress in 4.9f..6.4f)
                     
    // Tile is ON from 1.15 to 2.45, and from 3.75 onwards (unless disabled)
    val isTileOn = if (isDisabledMode) false else {
        (progress in 1.15f..2.45f) || (progress >= 3.75f)
    }
    
    val showResult = progress >= 6.4f

    // Smooth press progress for scaling the tile
    val tileScale by animateFloatAsState(
        targetValue = if (isPressing) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 600f),
        label = "tileScale"
    )

    // Touch pointer alpha
    val pointerAlpha by animateFloatAsState(
        targetValue = if (isPressing) 1f else 0f,
        animationSpec = tween(250),
        label = "pointerAlpha"
    )
    
    // Pointer scale (simulate finger pushing down)
    val pointerScale by animateFloatAsState(
        targetValue = if (isPressing) 0.9f else 1.1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "pointerScale"
    )

    // Ripple effect
    val rippleScale = if (isPressing) {
        val pressStart = when {
            progress < 2.0f -> 1.0f
            progress < 3.0f -> 2.3f
            progress < 4.0f -> 3.6f
            else -> 4.9f
        }
        val p = (progress - pressStart) / 0.3f
        if (p < 1f) p else 1f
    } else 0f

    val rippleAlpha = if (isPressing) 0.3f * (1f - rippleScale) else 0f

    val context = LocalContext.current
    val qsTileResId = remember { context.resources.getIdentifier("ic_qs_tile", "drawable", context.packageName) }
    
    val qsTileNameId = remember { context.resources.getIdentifier("qs_tile_name", "string", context.packageName) }
    val appName = if (qsTileNameId != 0) androidx.compose.ui.res.stringResource(id = qsTileNameId) else androidx.compose.ui.res.stringResource(id = com.mobile.superiorchat.R.string.app_name)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Visibility, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PREVIEW", fontSize = 11.sp, color = PrimaryLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFF161616), // Dark QS panel background simulation
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            val currentStepText = when {
                progress < 1.0f -> "READY"
                progress < 2.3f -> "1. ENABLE"
                progress < 3.6f -> "2. DISABLE"
                progress < 4.9f -> "3. ENABLE"
                progress < 6.4f -> "4. HOLD"
                else -> if (isDisabledMode) "BLOCKED" else "UNLOCKED"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                // Step Counter Badge
                Surface(
                    color = PrimaryLight.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(bottomEnd = 12.dp),
                    modifier = Modifier.align(Alignment.TopStart)
                ) {
                    Text(
                        text = currentStepText,
                        color = PrimaryLight,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                // The Tile
                Box(modifier = Modifier.scale(tileScale)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                            Surface(
                                color = if (isTileOn) PrimaryLight else SurfaceLevel2,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    if (qsTileResId != 0) {
                                        Icon(
                                            painter = androidx.compose.ui.res.painterResource(id = qsTileResId),
                                            contentDescription = null,
                                            tint = if (isTileOn) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.Info,
                                            contentDescription = null,
                                            tint = if (isTileOn) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Ripple precisely constrained over the circle
                            if (isPressing) {
                                Box(
                                    modifier = Modifier
                                        .size(100.dp * rippleScale)
                                        .background(Color.White.copy(alpha = rippleAlpha), androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text(
                            text = appName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            maxLines = 2,
                            lineHeight = 14.sp
                        )
                    }
                }
                
                // Finger pointer
                if (pointerAlpha > 0f) {
                    Icon(
                        imageVector = Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = pointerAlpha),
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = 4.dp) // Slightly below center, aiming at the lower half of the circle
                            .scale(pointerScale)
                            .size(52.dp)
                    )
                }
                
                // Result Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showResult,
                    enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xE6000000)), // Dark blur effect
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = if (isDisabledMode) MaterialTheme.colorScheme.error else PrimaryLight,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDisabledMode) Icons.Filled.Warning else Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = if (isDisabledMode) MaterialTheme.colorScheme.onError else MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = if (isDisabledMode) "No Response" else "App Unlocked",
                                color = if (isDisabledMode) MaterialTheme.colorScheme.error else PrimaryLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DialerAccessAnimPreview(dialerCode: String = "9131") {
    val fullCode = "*#*#$dialerCode#*#*"
    
    val infiniteTransition = rememberInfiniteTransition(label = "dialerAnim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f, // 6 seconds loop
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val typingStart = 0.5f
    val typingEnd = 4.5f // 4 seconds to type
    val charDuration = (typingEnd - typingStart) / fullCode.length
    
    val currentStep = if (progress < typingStart) {
        0
    } else if (progress >= typingEnd) {
        fullCode.length
    } else {
        ((progress - typingStart) / charDuration).toInt()
    }
    
    val timeInStep = if (progress in typingStart..typingEnd) {
        (progress - typingStart) % charDuration
    } else 0f
    
    val stepProgress = if (charDuration > 0) timeInStep / charDuration else 0f
    
    val isPressed = stepProgress > 0.5f && stepProgress < 0.9f && progress in typingStart..typingEnd
    val activeKey = if (isPressed) fullCode.getOrNull(currentStep)?.toString() else null
    
    val displayLength = if (progress < typingStart) 0 else if (progress >= typingEnd) fullCode.length else currentStep + (if (stepProgress > 0.6f) 1 else 0)
    val displayedText = fullCode.substring(0, displayLength)
    
    val showSuccess = progress >= typingEnd + 0.4f // Short delay after typing finishes

    fun getKeyPos(key: String): androidx.compose.ui.geometry.Offset {
        val col = when (key) { "1", "4", "7", "*" -> 0; "2", "5", "8", "0" -> 1; else -> 2 }
        val row = when (key) { "1", "2", "3" -> 0; "4", "5", "6" -> 1; "7", "8", "9" -> 2; else -> 3 }
        return androidx.compose.ui.geometry.Offset(col.toFloat(), row.toFloat())
    }

    val pointerOffsetX: Float
    val pointerOffsetY: Float

    if (progress < typingStart) {
        val startPos = getKeyPos(fullCode[0].toString())
        pointerOffsetX = startPos.x
        pointerOffsetY = startPos.y
    } else if (progress >= typingEnd) {
        val endPos = getKeyPos(fullCode.last().toString())
        pointerOffsetX = endPos.x
        pointerOffsetY = endPos.y
    } else {
        val currentKey = fullCode[currentStep].toString()
        val currentPos = getKeyPos(currentKey)
        
        if (currentStep == 0) {
            pointerOffsetX = currentPos.x
            pointerOffsetY = currentPos.y
        } else {
            if (stepProgress < 0.45f) {
                val prevKey = fullCode[currentStep - 1].toString()
                val prevPos = getKeyPos(prevKey)
                val t = stepProgress / 0.45f
                val smoothT = t * t * (3f - 2f * t)
                pointerOffsetX = prevPos.x + (currentPos.x - prevPos.x) * smoothT
                pointerOffsetY = prevPos.y + (currentPos.y - prevPos.y) * smoothT
            } else {
                pointerOffsetX = currentPos.x
                pointerOffsetY = currentPos.y
            }
        }
    }
    
    val handScale = if (progress in typingStart..typingEnd) {
        when {
            stepProgress < 0.5f -> 1.1f
            stepProgress < 0.6f -> {
                val t = (stepProgress - 0.5f) / 0.1f
                1.1f - (1.1f - 0.85f) * t
            }
            stepProgress < 0.8f -> 0.85f
            stepProgress < 0.9f -> {
                val t = (stepProgress - 0.8f) / 0.1f
                0.85f + (1.1f - 0.85f) * t
            }
            else -> 1.1f
        }
    } else 1.1f

    val fingerAlpha = when {
        progress < 0.2f -> 0f
        progress < 0.5f -> (progress - 0.2f) / 0.3f
        progress < 4.6f -> 1f
        progress < 4.9f -> 1f - (progress - 4.6f) / 0.3f
        else -> 0f
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Phone, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("DIALER PREVIEW", fontSize = 11.sp, color = PrimaryLight, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        Spacer(modifier = Modifier.height(8.dp))

        Surface(
            color = Color(0xFF121212), // Dark dialer background
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display area
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .background(SurfaceLevel1, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayedText,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp,
                            color = PrimaryLight,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Numpad Grid
                    Box(modifier = Modifier.fillMaxWidth()) {
                        val keys = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("*", "0", "#")
                        )
                        
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            keys.forEach { row ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround
                                ) {
                                    row.forEach { key ->
                                        val isActive = activeKey == key
                                        val scale by animateFloatAsState(
                                            targetValue = if (isActive) 0.85f else 1f,
                                            animationSpec = spring(dampingRatio = 0.6f, stiffness = 800f),
                                            label = "keyScale"
                                        )
                                        
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .scale(scale)
                                                .background(
                                                    color = if (isActive) PrimaryLight.copy(alpha = 0.2f) else Color.Transparent,
                                                    shape = androidx.compose.foundation.shape.CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = key,
                                                fontSize = 20.sp,
                                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isActive) PrimaryLight else TextPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Finger pointer overlay on top of everything
                if (fingerAlpha > 0f) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                        val gridWidth = maxWidth - 32.dp
                        val centerX = 16.dp + gridWidth * ((pointerOffsetX * 2f + 1f) / 6f)
                        val centerY = 102.dp + (pointerOffsetY * 52).dp
                        
                        val xDp = centerX - 26.dp
                        val yDp = centerY - 16.dp 
                        
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = fingerAlpha),
                            modifier = Modifier
                                .offset(x = xDp, y = yDp)
                                .scale(handScale)
                                .size(52.dp)
                        )
                    }
                }
                
                // Success Overlay
                androidx.compose.animation.AnimatedVisibility(
                    visible = showSuccess,
                    enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f),
                    exit = fadeOut(tween(200)),
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xE6000000)), // Dark blur effect
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                color = PrimaryLight,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "App Unlocked",
                                color = PrimaryLight,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
@Composable
fun QsTileSetupAnimPreview() {
    val context = LocalContext.current
    val qsTileResId = remember { context.resources.getIdentifier("ic_qs_tile", "drawable", context.packageName) }
    val qsTileNameId = remember { context.resources.getIdentifier("qs_tile_name", "string", context.packageName) }
    val appName = if (qsTileNameId != 0) androidx.compose.ui.res.stringResource(id = qsTileNameId) else androidx.compose.ui.res.stringResource(id = com.mobile.superiorchat.R.string.app_name)

    val infiniteTransition = rememberInfiniteTransition(label = "setupAnim")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val isDragging = progress in 1.0f..3.5f
    val isPressing = progress in 1.0f..3.8f
    
    val dragProgress = when {
        progress < 1.5f -> 0f
        progress < 3.0f -> {
            val t = (progress - 1.5f) / 1.5f
            if (t < 0.5f) 4f * t * t * t else {
                val f = -2f * t + 2f
                1f - (f * f * f) / 2f
            }
        }
        else -> 1f
    }

    Column(modifier = Modifier.fillMaxWidth()) {

        Surface(
            color = Color(0xFF1A1A1A),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, DividerColor),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val innerWidth = maxWidth - 24.dp
                val space = (innerWidth - 216.dp) / 3
                val calculatedEndX = space * 2.5f + 144.dp
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Hold and drag to add tiles", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.height(16.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth().height(86.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                        DummyCircularTile("Wi-Fi", Icons.Filled.Wifi, true)
                        DummyCircularTile("Bluetooth", Icons.Filled.Bluetooth, false)
                        
                        Box(modifier = Modifier.width(72.dp), contentAlignment = Alignment.TopCenter) {
                            if (dragProgress == 1f && !isDragging) {
                                CircularQsTileUi(appName, qsTileResId, true)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.White.copy(alpha = 0.05f), androidx.compose.foundation.shape.CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.1f), androidx.compose.foundation.shape.CircleShape)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DividerColor))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text("From apps that you installed", color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.height(16.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().height(86.dp),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Spacer(modifier = Modifier.width(8.dp))
                        if (dragProgress == 0f && !isDragging) {
                            CircularQsTileUi(appName, qsTileResId, false)
                        } else {
                            Box(modifier = Modifier.alpha(0f)) {
                                CircularQsTileUi(appName, qsTileResId, false)
                            }
                        }
                    }
                }
                
                if (isDragging) {
                    Box(modifier = Modifier.matchParentSize().padding(12.dp)) {
                        val startX = 8.dp
                        val startY = 191.dp
                        val endX = calculatedEndX
                        val endY = 32.dp
                        
                        val currentX = startX + (endX - startX) * dragProgress
                        val currentY = startY + (endY - startY) * dragProgress
                        
                        CircularQsTileUi(
                            appName = appName,
                            iconRes = qsTileResId,
                            isActive = false,
                            modifier = Modifier.offset(x = currentX, y = currentY)
                        )
                        
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .offset(x = currentX + 24.dp, y = currentY + 36.dp)
                                .scale(if (isPressing) 0.9f else 1.1f)
                                .size(32.dp)
                        )
                    }
                }
                
                if (!isDragging && dragProgress == 1f && progress >= 3.5f && progress < 4.0f) {
                    val depart = (progress - 3.5f) / 0.5f
                    Box(modifier = Modifier.matchParentSize().padding(12.dp)) {
                        val endX = calculatedEndX
                        val endY = 32.dp
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f * (1f - depart)),
                            modifier = Modifier
                                .offset(x = endX + 24.dp, y = endY + 36.dp + (20.dp * depart))
                                .scale(if (progress < 3.8f) 0.9f else 1.1f)
                                .size(32.dp)
                        )
                    }
                }
                
                if (!isDragging && dragProgress == 0f && progress > 0.5f && progress < 1.0f) {
                    val approach = (progress - 0.5f) / 0.5f
                    Box(modifier = Modifier.matchParentSize().padding(12.dp)) {
                        val startX = 8.dp
                        val startY = 191.dp
                        Icon(
                            imageVector = Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.8f * approach),
                            modifier = Modifier
                                .offset(x = startX + 24.dp, y = startY + 36.dp + (20.dp * (1f - approach)))
                                .scale(1.1f)
                                .size(32.dp)
                        )
                    }
                }
                
                androidx.compose.animation.AnimatedVisibility(
                    visible = progress > 4.5f,
                    enter = fadeIn() + scaleIn(initialScale = 0.8f),
                    exit = fadeOut(),
                    modifier = Modifier.matchParentSize()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color(0xE6000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = PrimaryLight, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Tile Added!", color = PrimaryLight, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }
                }
            }
            }
        }
    }
}

@Composable
fun DummyCircularTile(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector, isActive: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp).height(86.dp)) {
        Surface(
            color = if (isActive) PrimaryLight else SurfaceLevel2,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = name,
            fontSize = 11.sp,
            color = TextPrimary,
            maxLines = 2,
            lineHeight = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CircularQsTileUi(appName: String, iconRes: Int, isActive: Boolean, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier.width(72.dp).height(86.dp)) {
        Surface(
            color = if (isActive) PrimaryLight else SurfaceLevel2,
            shape = androidx.compose.foundation.shape.CircleShape,
            modifier = Modifier.size(56.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (iconRes != 0) {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(id = iconRes),
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = appName,
            fontSize = 11.sp,
            color = TextPrimary,
            maxLines = 2,
            lineHeight = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
        )
    }
}
