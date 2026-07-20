package com.mobile.superiorchat.ui.profile

import android.content.Context
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mobile.superiorchat.data.repository.LocalMediaItem
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.popups.ActionDialog
import com.mobile.superiorchat.ui.components.popups.ErrorDialog
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.ui.components.glow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import android.graphics.BitmapFactory

// ══════════════════════════════════════════════════════════
//  Profile Screen
// ══════════════════════════════════════════════════════════

import com.mobile.superiorchat.ui.GlobalDialogState
import com.mobile.superiorchat.ui.components.media.ImageCropper
import com.mobile.superiorchat.ui.components.media.MediaViewer
import com.mobile.superiorchat.ui.components.profile.EditInfoSheet
import com.mobile.superiorchat.ui.components.profile.ProfileSettingsSheet

sealed class ProfileOverlay {
    data object None : ProfileOverlay()
    data object GalleryPicker : ProfileOverlay()
    data class CropDialog(val uri: Uri) : ProfileOverlay()
    data object EditInfo : ProfileOverlay()
    data object Settings : ProfileOverlay()
    data object FullScreenPhoto : ProfileOverlay()
    data class RateLimitWarning(val pendingInfo: Triple<String, String, String>) : ProfileOverlay()
}

@Composable
fun ProfileScreen(
    hasCredentials: Boolean,
    onClearCredentials: () -> Unit,
    onShowGlobalDialog: (GlobalDialogState) -> Unit = {},
    onNavigateToSettings: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Load data once when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // ── Overlay State ────────────────────────────────────────
    var currentOverlay by remember { mutableStateOf<ProfileOverlay>(ProfileOverlay.None) }

    val permissionHandler = rememberProfilePermissionHandler(
        context = context,
        onShowGlobalDialog = onShowGlobalDialog,
        onGalleryGranted = { currentOverlay = ProfileOverlay.GalleryPicker },
        onCameraGranted = { uri -> 
            currentOverlay = ProfileOverlay.CropDialog(uri) 
        }
    )

    val launchGallery = { permissionHandler.requestGallery() }

    val syncState by com.mobile.superiorchat.core.StatusFlow.syncState.collectAsState()
    val syncMessage by com.mobile.superiorchat.core.StatusFlow.syncMessage.collectAsState()

    Scaffold(
        containerColor = Background
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Hero Header ──────────────────────────────
                ProfileHeroHeader(
                    displayName = viewModel.displayName,
                    username = viewModel.username,
                    botId = viewModel.botId,
                    avatarUri = viewModel.avatarUri,
                    onAvatarClick = { if (viewModel.avatarUri != null) currentOverlay = ProfileOverlay.FullScreenPhoto else launchGallery() },
                    onEditPhotoClick = { launchGallery() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── 3-Button Action Bar ──────────────────────
                ProfileActionBar(
                    onSetPhoto = { launchGallery() },
                    onEditInfo = { 
                        val expiry = com.mobile.superiorchat.core.AppGraph.prefs.profileEditRateLimitExpiry
                        if (System.currentTimeMillis() < expiry) {
                            val remaining = ((expiry - System.currentTimeMillis()) / 1000).toInt()
                            val hours = remaining / 3600
                            val mins = (remaining % 3600) / 60
                            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            viewModel.rateLimitError = "Telegram rate limit reached. Please try again in $timeStr."
                        } else {
                            currentOverlay = ProfileOverlay.EditInfo 
                        }
                    },
                    onSettings = { currentOverlay = ProfileOverlay.Settings }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // ── Info Rows ────────────────────────────────
                ProfileInfoSection(
                    displayName = viewModel.displayName,
                    description = viewModel.description,
                    shortDescription = viewModel.shortDescription
                )

                Spacer(modifier = Modifier.height(32.dp))
            }

            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryLight)
                }
            }

            // StatusFlow Pill
            androidx.compose.animation.AnimatedVisibility(
                visible = syncState != com.mobile.superiorchat.core.SyncState.IDLE,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).padding(top = padding.calculateTopPadding() + 8.dp)
            ) {
                val pillBgColor = when (syncState) {
                    com.mobile.superiorchat.core.SyncState.SUCCESS,
                    com.mobile.superiorchat.core.SyncState.SYNCING_PROFILE,
                    com.mobile.superiorchat.core.SyncState.SYNCING_MESSAGES -> PrimaryLight
                    com.mobile.superiorchat.core.SyncState.ERROR,
                    com.mobile.superiorchat.core.SyncState.OFFLINE,
                    com.mobile.superiorchat.core.SyncState.AUTH_ERROR -> Color(0xFF690005) // Dark Red
                    else -> SurfaceLevel1
                }
                val pillTextColor = when (syncState) {
                    com.mobile.superiorchat.core.SyncState.SUCCESS,
                    com.mobile.superiorchat.core.SyncState.SYNCING_PROFILE,
                    com.mobile.superiorchat.core.SyncState.SYNCING_MESSAGES -> Color(0xFF1000A9) // Dark Blue for contrast
                    else -> Color.White
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBgColor)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (syncState == com.mobile.superiorchat.core.SyncState.SYNCING_PROFILE || syncState == com.mobile.superiorchat.core.SyncState.SYNCING_MESSAGES) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = pillTextColor,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = syncMessage ?: "",
                        color = pillTextColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // ── Overlays ─────────────────────────────────────────

    // Gallery picker using GalleryGrid
    if (currentOverlay is ProfileOverlay.GalleryPicker) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { currentOverlay = ProfileOverlay.None },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
        ) {
            com.mobile.superiorchat.ui.components.media.GalleryGrid(
                maxSelection = 1,
                showVideos = false,
                onDismiss = { currentOverlay = ProfileOverlay.None },
                onMediaSelected = { items ->
                    if (items.isNotEmpty()) {
                        currentOverlay = ProfileOverlay.CropDialog(items.first().uri)
                        false // Prevent GalleryGrid from firing onDismiss and wiping our CropDialog!
                    } else false
                },
                onCameraClick = {
                    permissionHandler.requestCamera()
                }
            )
        }
    }

    // Crop dialog
    if (currentOverlay is ProfileOverlay.CropDialog) {
        val uri = (currentOverlay as ProfileOverlay.CropDialog).uri
        ImageCropper(
            imageUri = uri,
            onDismiss = { currentOverlay = ProfileOverlay.None },
            onCropConfirm = { croppedUri, cropX, cropY, cropSize ->
                viewModel.uploadProfilePhoto(context, uri, cropX, cropY, cropSize)
                currentOverlay = ProfileOverlay.None
            }
        )
    }

    // Edit Info bottom sheet
    if (currentOverlay is ProfileOverlay.EditInfo) {
        EditInfoSheet(
            currentName = viewModel.displayName,
            currentDescription = viewModel.description,
            currentShortDescription = viewModel.shortDescription,
            onDismiss = { currentOverlay = ProfileOverlay.None },
            onSave = { name, desc, shortDesc ->
                if (name != viewModel.displayName || desc != viewModel.description || shortDesc != viewModel.shortDescription) {
                    currentOverlay = ProfileOverlay.RateLimitWarning(Triple(name, desc, shortDesc))
                } else {
                    currentOverlay = ProfileOverlay.None
                }
            }
        )
    }

    // Profile Settings bottom sheet
    if (currentOverlay is ProfileOverlay.Settings) {
        ProfileSettingsSheet(
            hasCredentials = hasCredentials,
            onDismiss = { currentOverlay = ProfileOverlay.None },
            onClearCredentials = onClearCredentials,
            onNavigateToAppSettings = onNavigateToSettings
        )
    }

    // Full-screen photo viewer
    if (currentOverlay is ProfileOverlay.FullScreenPhoto && viewModel.avatarUri != null) {
        MediaViewer(
            mediaPath = viewModel.avatarUri!!.toString(),
            mediaType = "photo",
            onDismiss = { currentOverlay = ProfileOverlay.None }
        )
    }

    // Rate limit warning before saving
    if (currentOverlay is ProfileOverlay.RateLimitWarning) {
        val pendingInfo = (currentOverlay as ProfileOverlay.RateLimitWarning).pendingInfo
        ActionDialog(
            title = "Warning: Rate Limits",
            message = "Telegram strictly limits how often you can change your bot's name and description. Frequent updates will result in a 24-hour ban. Are you sure you want to proceed?",
            icon = Icons.Filled.Warning,
            iconTint = Color(0xFFE5C07B), // Warning Yellow
            confirmText = "Proceed",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.saveInfo(pendingInfo.first, pendingInfo.second, pendingInfo.third)
                currentOverlay = ProfileOverlay.None
            },
            onDismiss = {
                currentOverlay = ProfileOverlay.None
            }
        )
    }

    // Rate limit error (from ViewModel)
    viewModel.rateLimitError?.let { errorMsg ->
        ErrorDialog(
            message = errorMsg,
            onDismiss = { viewModel.clearError() }
        )
    }
}

// ══════════════════════════════════════════════════════════
//  Hero Header — avatar circle, name, username, bot ID
// ══════════════════════════════════════════════════════════

@Composable
private fun ProfileHeroHeader(
    displayName: String,
    username: String,
    botId: String,
    avatarUri: Uri?,
    onAvatarClick: () -> Unit,
    onEditPhotoClick: () -> Unit
) {
    val glowAlpha by rememberInfiniteTransition(label = "hero_glow").animateFloat(
        initialValue = 0.35f, targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Primary.copy(alpha = 0.12f), Background),
                    endY = Float.POSITIVE_INFINITY
                )
            )
            .padding(top = 40.dp, bottom = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            // Avatar + conditional badge
            Box(contentAlignment = Alignment.BottomEnd) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .glow(color = Primary.copy(alpha = glowAlpha), radius = 44f, dy = 0f, cornerRadius = 55.dp)
                        .clip(CircleShape)
                        .border(
                            width = 2.dp,
                            brush = Brush.sweepGradient(listOf(Primary, Secondary, PrimaryLight, Primary)),
                            shape = CircleShape
                        )
                        .bounceClick(onClick = onAvatarClick),
                    contentAlignment = Alignment.Center
                ) {
                    if (avatarUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(avatarUri).crossfade(true).build(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // No photo — show person placeholder, no badge
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.radialGradient(listOf(Secondary.copy(0.4f), Primary.copy(0.2f))),
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.Person, null, tint = PrimaryLight, modifier = Modifier.size(52.dp))
                        }
                    }
                }

                // Badge: Edit icon on avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .offset(x = (-2).dp, y = (-2).dp)
                        .glow(color = PrimaryLight.copy(0.5f), radius = 14f, dy = 4f, cornerRadius = 16.dp)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(PrimaryLight, Primary)))
                        .bounceClick(onClick = onEditPhotoClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, null, tint = Background, modifier = Modifier.size(15.dp))
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Display name
            AnimatedContent(
                targetState = displayName,
                transitionSpec = { slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut() },
                label = "name_anim"
            ) { name ->
                Text(name, color = TextPrimary, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Spacer(modifier = Modifier.height(3.dp))

            Text("@$username", color = PrimaryLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.height(10.dp))

            // Bot ID chip — read-only
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(Icons.Filled.Tag, null, tint = TextSecondary, modifier = Modifier.size(12.dp))
                Text("ID: $botId", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Icon(Icons.Filled.Lock, null, tint = TextSecondary.copy(0.45f), modifier = Modifier.size(10.dp))
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Three Action Buttons — Set Photo / Edit Info / Settings
// ══════════════════════════════════════════════════════════

@Composable
private fun ProfileActionBar(
    onSetPhoto: () -> Unit,
    onEditInfo: () -> Unit,
    onSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ProfileActionButton(
            label = "Set Photo",
            icon = Icons.Filled.AddAPhoto,
            modifier = Modifier.weight(1f),
            onClick = onSetPhoto
        )
        ProfileActionButton(
            label = "Edit Info",
            icon = Icons.Filled.Edit,
            modifier = Modifier.weight(1f),
            onClick = onEditInfo
        )
        ProfileActionButton(
            label = "Settings",
            icon = Icons.Filled.Settings,
            modifier = Modifier.weight(1f),
            onClick = onSettings
        )
    }
}

@Composable
private fun ProfileActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.93f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "btn_scale"
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceLevel1)
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = label, tint = PrimaryLight, modifier = Modifier.size(22.dp))
        Text(label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    }
}

// ══════════════════════════════════════════════════════════
//  Info Rows — display-only
// ══════════════════════════════════════════════════════════

@Composable
private fun ProfileInfoSection(
    displayName: String,
    description: String,
    shortDescription: String
) {
    // Flat rows — no box container, matches Telegram style
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        InfoRow(value = displayName, label = "Name", icon = Icons.Filled.Person, isFirst = true)
        InfoDivider()
        InfoRow(
            value = shortDescription.ifEmpty { "Not set" },
            label = "About",
            icon = Icons.AutoMirrored.Filled.ShortText,
            dimValue = shortDescription.isEmpty()
        )
        InfoDivider()
        InfoRow(
            value = description.ifEmpty { "Not set" },
            label = "Description",
            icon = Icons.Filled.Info,
            isLast = true,
            dimValue = description.isEmpty()
        )
    }
}

@Composable
private fun InfoRow(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isFirst: Boolean = false,
    isLast: Boolean = false,
    dimValue: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = 16.dp, end = 16.dp,
                top = if (isFirst) 16.dp else 12.dp,
                bottom = if (isLast) 16.dp else 12.dp
            ),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, null, tint = PrimaryLight.copy(0.8f), modifier = Modifier.size(18.dp).padding(top = 2.dp))
        Column {
            Text(
                text = value,
                color = if (dimValue) TextSecondary else TextPrimary,
                fontSize = 14.sp,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(label, color = TextSecondary, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 48.dp),
        color = DividerColor,
        thickness = 0.5.dp
    )
}

// ══════════════════════════════════════════════════════════
//  Permission Handler Utility
// ══════════════════════════════════════════════════════════

class ProfilePermissionHandler(
    val requestGallery: () -> Unit,
    val requestCamera: () -> Unit,
    val launchCameraDirectly: () -> Unit
)

@Composable
fun rememberProfilePermissionHandler(
    context: Context,
    onShowGlobalDialog: (com.mobile.superiorchat.ui.GlobalDialogState) -> Unit,
    onGalleryGranted: () -> Unit,
    onCameraGranted: (Uri) -> Unit
): ProfilePermissionHandler {
    val cameraUri = remember { mutableStateOf<Uri?>(null) }
    
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) cameraUri.value?.let { onCameraGranted(it) }
        }
    )

    val launchCameraDirectly: () -> Unit = {
        val imageFile = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.provider", imageFile
        )
        cameraUri.value = uri
        cameraLauncher.launch(uri)
    }

    val permissionHandler = com.mobile.superiorchat.utils.rememberPermissionHandler(onShowGlobalDialog)

    return remember(context, onShowGlobalDialog, onGalleryGranted, onCameraGranted) {
        ProfilePermissionHandler(
            requestGallery = {
                permissionHandler.requestStorageForMedia {
                    onGalleryGranted()
                }
            },
            requestCamera = { 
                permissionHandler.requestCamera {
                    launchCameraDirectly()
                }
            },
            launchCameraDirectly = launchCameraDirectly
        )
    }
}


