package com.mobile.superiorutils.ui.components

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mobile.superiorutils.theme.DividerColor
import com.mobile.superiorutils.theme.PrimaryLight
import com.mobile.superiorutils.theme.SurfaceLevel2
import com.mobile.superiorutils.ui.ChatViewModel
import com.mobile.superiorutils.ui.LocalFileItem
import com.mobile.superiorutils.ui.LocalMediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

enum class PickerTab { GALLERY, FILES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPicker(
    visible: Boolean,
    initialTab: PickerTab,
    onDismiss: () -> Unit,
    viewModel: ChatViewModel,
    onMediaSelected: (List<Uri>) -> Unit,
    onFilesSelected: (List<File>) -> Unit,
    onCameraClick: () -> Unit,
    onSystemPickerClick: () -> Unit
) {
    if (!visible) return

    val scope = rememberCoroutineScope()
    var currentTab by remember { mutableStateOf(initialTab) }

    // Dialog layout animation trigger
    var dialogVisible by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            dialogVisible = true
        }
    }

    val animatedDismiss: () -> Unit = {
        dialogVisible = false
        scope.launch {
            delay(200) // Wait for exit animation
            onDismiss()
        }
    }

    // Capture back button inside dialog
    BackHandler(enabled = dialogVisible) {
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
            visible = dialogVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            // Root Box with systemBarsPadding to prevent overlapping system bars
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .systemBarsPadding()
            ) {
                AnimatedContent(
                    targetState = currentTab,
                    transitionSpec = {
                        if (targetState == PickerTab.FILES) {
                            slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                        } else {
                            slideInHorizontally { -it } togetherWith slideOutHorizontally { it }
                        }
                    },
                    label = "tab_switch_transition",
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        PickerTab.GALLERY -> {
                            GalleryGrid(
                                viewModel = viewModel,
                                onDismiss = animatedDismiss,
                                onMediaSelected = onMediaSelected,
                                onCameraClick = onCameraClick,
                                onSwitchToFiles = { currentTab = PickerTab.FILES }
                            )
                        }
                        PickerTab.FILES -> {
                            FileExplorer(
                                viewModel = viewModel,
                                onDismiss = animatedDismiss,
                                onFilesSelected = onFilesSelected,
                                onSystemPickerClick = onSystemPickerClick,
                                onSwitchToGallery = { currentTab = PickerTab.GALLERY }
                            )
                        }
                    }
                }
            }
        }
    }
}
