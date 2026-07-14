package com.mobile.superiorutils.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.mobile.superiorutils.theme.DividerColor
import com.mobile.superiorutils.theme.PrimaryLight
import com.mobile.superiorutils.ui.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File

import com.mobile.superiorutils.data.repository.LocalMediaItem

enum class PickerTab { GALLERY, FILES }

@Composable
fun MediaPicker(
    visible: Boolean,
    initialTab: PickerTab,
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    onMediaSelected: (List<LocalMediaItem>) -> Boolean,
    onFilesSelected: (List<File>) -> Boolean,
    onCameraClick: () -> Unit,
    onSystemPickerClick: () -> Unit,
    onRequestManageStoragePermission: () -> Unit
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(initialTab) }
    var isBottomBarVisible by remember { mutableStateOf(true) }

    // Dialog layout animation trigger via MutableTransitionState to avoid brittle delay()
    val transitionState = remember { MutableTransitionState(false) }
    LaunchedEffect(visible) {
        if (visible) {
            transitionState.targetState = true
        }
    }

    val animatedDismiss: () -> Unit = {
        transitionState.targetState = false
    }

    LaunchedEffect(transitionState.currentState, transitionState.isIdle) {
        if (!transitionState.targetState && transitionState.isIdle) {
            onDismiss()
        }
    }

    // Capture back button inside dialog
    BackHandler(enabled = transitionState.targetState) {
        animatedDismiss()
    }

    Dialog(
        onDismissRequest = { animatedDismiss() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        AnimatedVisibility(
            visibleState = transitionState,
            enter = slideInVertically(initialOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f)) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }, animationSpec = spring(dampingRatio = 0.85f)) + fadeOut()
        ) {
            // Root Box with systemBarsPadding to prevent overlapping system bars
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .systemBarsPadding()
            ) {
                // Drag handle pill at the top of the sheet
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(DividerColor)
                    )
                }

                // Inner content layout
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(Unit) {
                            var dragAmountX = 0f
                            detectHorizontalDragGestures(
                                onDragEnd = {
                                    if (dragAmountX < -150f && currentTab == PickerTab.GALLERY) {
                                        currentTab = PickerTab.FILES
                                    } else if (dragAmountX > 150f && currentTab == PickerTab.FILES) {
                                        currentTab = PickerTab.GALLERY
                                    }
                                    dragAmountX = 0f
                                }
                            ) { _, dragAmount ->
                                dragAmountX += dragAmount
                            }
                        }
                ) {
                    AnimatedContent(
                        targetState = currentTab,
                        transitionSpec = {
                            if (targetState == PickerTab.FILES) {
                                slideInHorizontally(animationSpec = spring()) { it } togetherWith 
                                slideOutHorizontally(animationSpec = spring()) { -it }
                            } else {
                                slideInHorizontally(animationSpec = spring()) { -it } togetherWith 
                                slideOutHorizontally(animationSpec = spring()) { it }
                            }
                        },
                        label = "tab_switch_transition",
                        modifier = Modifier.fillMaxSize()
                    ) { tab ->
                        when (tab) {
                            PickerTab.GALLERY -> {
                                isBottomBarVisible = true
                                GalleryGrid(
                                    viewModel = viewModel,
                                    onDismiss = animatedDismiss,
                                    onMediaSelected = onMediaSelected,
                                    onCameraClick = onCameraClick
                                )
                            }
                            PickerTab.FILES -> {
                                FileExplorer(
                                    viewModel = viewModel,
                                    onDismiss = animatedDismiss,
                                    onFilesSelected = onFilesSelected,
                                    onSystemPickerClick = onSystemPickerClick,
                                    onBottomBarVisibilityChanged = { isBottomBarVisible = it },
                                    onRequestManageStoragePermission = onRequestManageStoragePermission
                                )
                            }
                        }
                    }
                }

                // Shared animated bottom bar
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
                ) {
                    SharedPickerBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedPickerBottomBar(
    currentTab: PickerTab,
    onTabSelected: (PickerTab) -> Unit
) {
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(Color.Black)
            .border(1.dp, DividerColor.copy(alpha = 0.05f))
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Tab Items Container
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.05f))
                .padding(4.dp)
        ) {
            // Sliding Background Pill
            val density = androidx.compose.ui.platform.LocalDensity.current
            val tabWidth = remember { mutableStateOf(0.dp) }
            
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val width = maxWidth / 2
                val targetOffset = if (currentTab == PickerTab.GALLERY) 0.dp else width
                val animatedOffset by animateDpAsState(
                    targetValue = targetOffset,
                    animationSpec = spring(stiffness = 500f),
                    label = "tab_pill_offset"
                )

                Box(
                    modifier = Modifier
                        .offset(x = animatedOffset)
                        .width(width)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PrimaryLight.copy(alpha = 0.15f))
                        .border(1.dp, PrimaryLight.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                )
            }

            // Tab Content Buttons
            Row(modifier = Modifier.fillMaxSize()) {
                // Gallery Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentTab != PickerTab.GALLERY) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(PickerTab.GALLERY)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = if (currentTab == PickerTab.GALLERY) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gallery",
                            color = if (currentTab == PickerTab.GALLERY) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (currentTab == PickerTab.GALLERY) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                // Files Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (currentTab != PickerTab.FILES) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTabSelected(PickerTab.FILES)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = "Files",
                            tint = if (currentTab == PickerTab.FILES) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Files",
                            color = if (currentTab == PickerTab.FILES) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            fontWeight = if (currentTab == PickerTab.FILES) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}
