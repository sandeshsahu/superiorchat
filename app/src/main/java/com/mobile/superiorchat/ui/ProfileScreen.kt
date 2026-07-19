package com.mobile.superiorchat.ui

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
import com.mobile.superiorchat.ui.components.ActionDialog
import com.mobile.superiorchat.ui.components.ErrorDialog
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.ui.components.glow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

// ══════════════════════════════════════════════════════════
//  Profile Screen
// ══════════════════════════════════════════════════════════

@Composable
fun ProfileScreen(onNavigateToSettings: (() -> Unit)? = null) {
    val context = LocalContext.current
    val viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    // Load data once when screen opens
    LaunchedEffect(Unit) {
        viewModel.loadProfile()
    }

    // ── Overlay State ────────────────────────────────────────
    var showGalleryPicker by remember { mutableStateOf(false) }
    var showCropDialog by remember { mutableStateOf<Uri?>(null) }
    var showEditInfoSheet by remember { mutableStateOf(false) }
    var showProfileSettings by remember { mutableStateOf(false) }
    var showFullScreenPhoto by remember { mutableStateOf(false) }
    var showRemovePhotoConfirm by remember { mutableStateOf(false) }
    var showRateLimitWarning by remember { mutableStateOf(false) }
    var pendingEditInfo by remember { mutableStateOf<Triple<String, String, String>?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val syncState by com.mobile.superiorchat.core.StatusFlow.syncState.collectAsState()
    val syncMessage by com.mobile.superiorchat.core.StatusFlow.syncMessage.collectAsState()

    // Show errors/success messages from ViewModel
    LaunchedEffect(syncMessage) {
        if (!syncMessage.isNullOrBlank() && syncState != com.mobile.superiorchat.core.SyncState.SYNCING_PROFILE && syncState != com.mobile.superiorchat.core.SyncState.SYNCING_MESSAGES) {
            snackbarHostState.showSnackbar(syncMessage!!)
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = SurfaceLevel1,
                    contentColor = TextPrimary,
                    actionColor = Primary
                )
            }
        },
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
                    onAvatarClick = { if (viewModel.avatarUri != null) showFullScreenPhoto = true else showGalleryPicker = true },
                    onRemovePhotoClick = { showRemovePhotoConfirm = true }
                )

                Spacer(modifier = Modifier.height(8.dp))

                // ── 3-Button Action Bar ──────────────────────
                ProfileActionBar(
                    onSetPhoto = { showGalleryPicker = true },
                    onEditInfo = { 
                        val expiry = com.mobile.superiorchat.core.AppGraph.prefs.profileEditRateLimitExpiry
                        if (System.currentTimeMillis() < expiry) {
                            val remaining = ((expiry - System.currentTimeMillis()) / 1000).toInt()
                            val hours = remaining / 3600
                            val mins = (remaining % 3600) / 60
                            val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                            viewModel.rateLimitError = "Telegram rate limit reached. Please try again in $timeStr."
                        } else {
                            showEditInfoSheet = true 
                        }
                    },
                    onSettings = { showProfileSettings = true }
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
        }
    }

    // ── Overlays ─────────────────────────────────────────

    // Gallery picker sheet
    if (showGalleryPicker) {
        ProfileImagePickerSheet(
            context = context,
            onDismiss = { showGalleryPicker = false },
            onImagePicked = { uri ->
                showGalleryPicker = false
                showCropDialog = uri
            }
        )
    }

    // Crop dialog
    showCropDialog?.let { uri ->
        ImageCropDialog(
            imageUri = uri,
            onDismiss = { showCropDialog = null },
            onCropConfirm = { croppedUri, cropX, cropY, cropSize ->
                viewModel.uploadProfilePhoto(context, uri, cropX, cropY, cropSize)
                showCropDialog = null
            }
        )
    }

    // Edit Info bottom sheet
    if (showEditInfoSheet) {
        EditInfoSheet(
            currentName = viewModel.displayName,
            currentDescription = viewModel.description,
            currentShortDescription = viewModel.shortDescription,
            onDismiss = { showEditInfoSheet = false },
            onSave = { name, desc, shortDesc ->
                if (name != viewModel.displayName || desc != viewModel.description || shortDesc != viewModel.shortDescription) {
                    pendingEditInfo = Triple(name, desc, shortDesc)
                    showRateLimitWarning = true
                    showEditInfoSheet = false
                } else {
                    showEditInfoSheet = false
                }
            }
        )
    }

    // Profile Settings bottom sheet
    if (showProfileSettings) {
        ProfileSettingsSheet(
            hasPhoto = viewModel.avatarUri != null,
            onDismiss = { showProfileSettings = false },
            onRemovePhoto = { showRemovePhotoConfirm = true },
            onNavigateToAppSettings = onNavigateToSettings
        )
    }

    // Remove photo confirm
    if (showRemovePhotoConfirm) {
        ActionDialog(
            title = "Remove Profile Photo",
            message = "This will revert the bot to its default Telegram avatar.",
            icon = Icons.Filled.DeleteForever,
            iconTint = ErrorRed,
            confirmText = "Remove",
            dismissText = "Cancel",
            onConfirm = {
                viewModel.removeProfilePhoto()
                showRemovePhotoConfirm = false
                showProfileSettings = false
            },
            onDismiss = { showRemovePhotoConfirm = false }
        )
    }

    // Full-screen photo viewer
    if (showFullScreenPhoto && viewModel.avatarUri != null) {
        FullScreenPhotoDialog(
            imageUri = viewModel.avatarUri!!,
            onDismiss = { showFullScreenPhoto = false }
        )
    }

    // Rate limit warning before saving
    if (showRateLimitWarning && pendingEditInfo != null) {
        ActionDialog(
            title = "Warning: Rate Limits",
            message = "Telegram strictly limits how often you can change your bot's name and description. Frequent updates will result in a 24-hour ban. Are you sure you want to proceed?",
            icon = Icons.Filled.Warning,
            iconTint = Color(0xFFE5C07B), // Warning Yellow
            confirmText = "Proceed",
            dismissText = "Cancel",
            onConfirm = {
                pendingEditInfo?.let { (n, d, sd) -> viewModel.saveInfo(n, d, sd) }
                pendingEditInfo = null
                showRateLimitWarning = false
            },
            onDismiss = {
                pendingEditInfo = null
                showRateLimitWarning = false
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
    onRemovePhotoClick: () -> Unit
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

                // Badge: only show when photo IS set — acts as quick-delete
                if (avatarUri != null) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .glow(color = ErrorRed.copy(0.5f), radius = 14f, dy = 4f, cornerRadius = 16.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(ErrorRed, Color(0xFFB71C1C))))
                            .bounceClick(onClick = onRemovePhotoClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Delete, null, tint = Color.White, modifier = Modifier.size(15.dp))
                    }
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
//  Edit Info — ModalBottomSheet with all editable fields
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditInfoSheet(
    currentName: String,
    currentDescription: String,
    currentShortDescription: String,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var draftName by remember { mutableStateOf(currentName) }
    var draftDesc by remember { mutableStateOf(currentDescription) }
    var draftShortDesc by remember { mutableStateOf(currentShortDescription) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLevel1,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(DividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Edit Info", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Changes saved locally — backend coming soon", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Name field
            EditSheetField(
                label = "Display Name",
                value = draftName,
                onValueChange = { if (it.length <= 64) draftName = it },
                maxLength = 64,
                singleLine = true,
                placeholder = "Bot display name"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Short Description field
            EditSheetField(
                label = "About",
                value = draftShortDesc,
                onValueChange = { if (it.length <= 120) draftShortDesc = it },
                maxLength = 120,
                singleLine = true,
                placeholder = "Shown when sharing (≤120 chars)"
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Description field
            EditSheetField(
                label = "Description",
                value = draftDesc,
                onValueChange = { if (it.length <= 512) draftDesc = it },
                maxLength = 512,
                singleLine = false,
                placeholder = "Shown on bot profile page"
            )
            Spacer(modifier = Modifier.height(28.dp))

            val canSave = draftName.trim().isNotEmpty() &&
                    (draftName.trim() != currentName ||
                     draftDesc.trim() != currentDescription ||
                     draftShortDesc.trim() != currentShortDescription)

            Button(
                onClick = {
                    if (canSave) onSave(draftName.trim(), draftDesc.trim(), draftShortDesc.trim())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled = canSave,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Primary,
                    disabledContainerColor = SurfaceLevel2
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = if (canSave) Color.White else TextSecondary)
            }
        }
    }
}

@Composable
private fun EditSheetField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    maxLength: Int,
    singleLine: Boolean,
    placeholder: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Text(
                "${value.length}/$maxLength",
                color = if (value.length >= maxLength) ErrorRed else TextSecondary,
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextSecondary, fontSize = 14.sp) },
            singleLine = singleLine,
            minLines = if (singleLine) 1 else 3,
            maxLines = if (singleLine) 1 else 5,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedContainerColor = SurfaceLevel2,
                focusedContainerColor = SurfaceLevel2,
                unfocusedBorderColor = DividerColor,
                focusedBorderColor = Primary,
                unfocusedTextColor = TextPrimary,
                focusedTextColor = TextPrimary,
                cursorColor = PrimaryLight
            ),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 14.sp)
        )
    }
}

// ══════════════════════════════════════════════════════════
//  Profile Settings Sheet — danger zone + future features
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileSettingsSheet(
    hasPhoto: Boolean,
    onDismiss: () -> Unit,
    onRemovePhoto: () -> Unit,
    onNavigateToAppSettings: (() -> Unit)?
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLevel1,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(DividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("Profile Settings", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Manage your profile preferences", color = TextSecondary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Future features placeholder row
            SettingsSheetRow(
                icon = Icons.Filled.Shield,
                iconTint = InfoBlue,
                title = "Privacy & Security",
                subtitle = "Coming soon",
                onClick = { /* future */ }
            )

            Spacer(modifier = Modifier.height(12.dp))

            SettingsSheetRow(
                icon = Icons.Filled.Notifications,
                iconTint = PrimaryLight,
                title = "Notifications",
                subtitle = "Coming soon",
                onClick = { /* future */ }
            )

            // Danger Zone — always visible, styled as red card
            Spacer(modifier = Modifier.height(28.dp))

            // Danger zone header
            Row(verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = ErrorRed.copy(0.2f))
                Spacer(modifier = Modifier.width(10.dp))
                Text("Danger Zone", color = ErrorRed.copy(0.6f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.width(10.dp))
                HorizontalDivider(modifier = Modifier.weight(1f), color = ErrorRed.copy(0.2f))
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Danger Zone card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(ErrorRed.copy(alpha = 0.07f))
                    .border(1.dp, ErrorRed.copy(alpha = 0.22f), RoundedCornerShape(16.dp))
                    .padding(4.dp)
            ) {
                if (hasPhoto) {
                    SettingsSheetRow(
                        icon = Icons.Filled.DeleteForever,
                        iconTint = ErrorRed,
                        title = "Remove Profile Photo",
                        subtitle = "Reverts to default Telegram avatar",
                        titleColor = ErrorRed,
                        onClick = onRemovePhoto
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.Info, null, tint = ErrorRed.copy(0.5f), modifier = Modifier.size(16.dp))
                        Text(
                            "No actions available — set a profile photo first",
                            color = ErrorRed.copy(0.5f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSheetRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    titleColor: Color = TextPrimary,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "row_scale"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(SurfaceLevel2)
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(18.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = titleColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, color = TextSecondary, fontSize = 11.sp)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
    }
}

// ══════════════════════════════════════════════════════════
//  Full-Screen Photo Viewer — tap/swipe down to open
// ══════════════════════════════════════════════════════════

@Composable
private fun FullScreenPhotoDialog(
    imageUri: Uri,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(imageUri).crossfade(true).build(),
                contentDescription = "Full Screen Photo",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.FillWidth
            )
            // Dismiss hint
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.7f))))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Filled.Close, null, tint = Color.White.copy(0.7f), modifier = Modifier.size(14.dp))
                    Text("Tap anywhere to close", color = Color.White.copy(0.7f), fontSize = 12.sp)
                }
            }
        }
    }
}

// ══════════════════════════════════════════════════════════
//  Gallery Picker — images-only, scroll-safe tiles
// ══════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProfileImagePickerSheet(
    context: Context,
    onDismiss: () -> Unit,
    onImagePicked: (Uri) -> Unit
) {
    var mediaItems by remember { mutableStateOf<List<LocalMediaItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val list = mutableListOf<LocalMediaItem>()
            val projection = arrayOf(
                android.provider.MediaStore.Images.Media._ID,
                android.provider.MediaStore.Images.Media.DATE_ADDED
            )
            context.contentResolver.query(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                "${android.provider.MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media._ID)
                val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATE_ADDED)
                while (cursor.moveToNext() && list.size < 200) {
                    val id = cursor.getLong(idCol)
                    val date = cursor.getLong(dateCol)
                    val uri = android.content.ContentUris.withAppendedId(
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    list.add(LocalMediaItem(id, uri, false, null, date))
                }
            }
            mediaItems = list
        }
        isLoading = false
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceLevel1,
        dragHandle = {
            Box(
                Modifier.padding(top = 12.dp, bottom = 8.dp)
                    .width(40.dp).height(4.dp)
                    .background(DividerColor, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Choose Photo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Select an image to set as profile photo", color = TextSecondary, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, null, tint = TextSecondary)
                }
            }

            when {
                isLoading -> Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryLight, modifier = Modifier.size(32.dp))
                }
                mediaItems.isEmpty() -> Box(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.PhotoLibrary, null, tint = TextSecondary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No photos found", color = TextSecondary, fontSize = 14.sp)
                    }
                }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp)
                ) {
                    items(mediaItems, key = { it.id }) { item ->
                        // Scroll-safe tile: uses clickable + interactionSource, NOT detectTapGestures
                        ProfilePhotoTile(uri = item.uri, onClick = { onImagePicked(item.uri) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// Scroll-safe photo tile — clickable passes scroll gestures to LazyVerticalGrid
@Composable
private fun ProfilePhotoTile(uri: Uri, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val tileScale by animateFloatAsState(
        targetValue = if (isPressed) 0.91f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "tile_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .scale(tileScale)
            .clip(RoundedCornerShape(10.dp))
            .background(SurfaceLevel2)
            // clickable, not pointerInput — scroll events propagate to grid correctly
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(uri).size(280)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .crossfade(true).build(),
            contentDescription = "Photo",
            loading = { Box(Modifier.fillMaxSize().background(SurfaceLevel2)) },
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Bottom gradient scrim
        Box(
            modifier = Modifier.fillMaxWidth().height(28.dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(0.28f))))
        )
    }
}

// ══════════════════════════════════════════════════════════
//  Image Crop Dialog — resizable square with corner handles
// ══════════════════════════════════════════════════════════

@Composable
private fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirm: (Uri, Float, Float, Float) -> Unit
) {
    // Crop box state — normalized fractions [0..1] relative to canvas
    // cropX, cropY = top-left corner; cropSize = side length (all 0..1)
    var cropX by remember { mutableStateOf(0.14f) }
    var cropY by remember { mutableStateOf(0.14f) }
    var cropSize by remember { mutableStateOf(0.72f) }

    // Animated crop border pulse
    val borderAlpha by rememberInfiniteTransition(label = "crop_pulse").animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "border_alpha"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .clip(RoundedCornerShape(24.dp))
                .background(Color.Black)
                .border(1.dp, DividerColor, RoundedCornerShape(24.dp))
        ) {
            Column {
                // Title bar
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = TextSecondary, fontWeight = FontWeight.SemiBold)
                    }
                    Text("Crop Photo", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    TextButton(onClick = { onCropConfirm(imageUri, cropX, cropY, cropSize) }) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Primary.copy(0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("Use", color = PrimaryLight, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Crop canvas — BoxWithConstraints to get pixel size for hit tests
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(Color.Black)
                ) {
                    val canvasWidthPx = constraints.maxWidth.toFloat()
                    val canvasHeightPx = constraints.maxHeight.toFloat()

                    // Background image (static)
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUri).crossfade(true).build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )

                    // Crop overlay — scrim + box + grid + handles (drawn on Canvas)
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            // Move crop box by dragging INSIDE it
                            .pointerInput(canvasWidthPx, canvasHeightPx) {
                                detectDragGestures { change, dragAmount ->
                                    val bx = cropX * canvasWidthPx
                                    val by = cropY * canvasHeightPx
                                    val bs = cropSize * canvasWidthPx
                                    val touchX = change.position.x
                                    val touchY = change.position.y
                                    // Only move if touch is INSIDE crop box (not near corners)
                                    val handleZone = 36f
                                    val insideX = touchX in (bx + handleZone)..(bx + bs - handleZone)
                                    val insideY = touchY in (by + handleZone)..(by + bs - handleZone)
                                    if (insideX && insideY) {
                                        val newX = cropX + dragAmount.x / canvasWidthPx
                                        val newY = cropY + dragAmount.y / canvasHeightPx
                                        cropX = newX.coerceIn(0f, 1f - cropSize)
                                        cropY = newY.coerceIn(0f, 1f - cropSize)
                                    }
                                }
                            }
                    ) {
                        val bx = cropX * size.width
                        val by = cropY * size.height
                        val bs = cropSize * size.width

                        // Dark scrim (4 rects around crop box)
                        val scrim = Color.Black.copy(alpha = 0.65f)
                        drawRect(scrim, topLeft = Offset.Zero, size = Size(size.width, by))
                        drawRect(scrim, topLeft = Offset(0f, by + bs), size = Size(size.width, size.height - by - bs))
                        drawRect(scrim, topLeft = Offset(0f, by), size = Size(bx, bs))
                        drawRect(scrim, topLeft = Offset(bx + bs, by), size = Size(size.width - bx - bs, bs))

                        // Crop border
                        drawRect(
                            color = Primary.copy(alpha = borderAlpha),
                            topLeft = Offset(bx, by),
                            size = Size(bs, bs),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Rule-of-thirds grid
                        val third = bs / 3f
                        val gridColor = Color.White.copy(alpha = 0.18f)
                        for (i in 1..2) {
                            drawLine(gridColor, Offset(bx + third * i, by), Offset(bx + third * i, by + bs), 1.dp.toPx())
                            drawLine(gridColor, Offset(bx, by + third * i), Offset(bx + bs, by + third * i), 1.dp.toPx())
                        }

                        // Corner L-handles
                        val hLen = 20.dp.toPx()
                        val hW = 3.dp.toPx()
                        val hColor = PrimaryLight
                        // TL
                        drawLine(hColor, Offset(bx, by), Offset(bx + hLen, by), hW)
                        drawLine(hColor, Offset(bx, by), Offset(bx, by + hLen), hW)
                        // TR
                        drawLine(hColor, Offset(bx + bs, by), Offset(bx + bs - hLen, by), hW)
                        drawLine(hColor, Offset(bx + bs, by), Offset(bx + bs, by + hLen), hW)
                        // BL
                        drawLine(hColor, Offset(bx, by + bs), Offset(bx + hLen, by + bs), hW)
                        drawLine(hColor, Offset(bx, by + bs), Offset(bx, by + bs - hLen), hW)
                        // BR
                        drawLine(hColor, Offset(bx + bs, by + bs), Offset(bx + bs - hLen, by + bs), hW)
                        drawLine(hColor, Offset(bx + bs, by + bs), Offset(bx + bs, by + bs - hLen), hW)
                    }

                    // ── Corner resize handles (transparent hit areas on top of Canvas) ──
                    val handleSizeDp: Dp = 44.dp
                    
                    val updateCrop: (Float, Float, Boolean, Boolean) -> Unit = { dragX, dragY, isLeft, isTop ->
                        val dx = dragX / canvasWidthPx
                        val dy = dragY / canvasHeightPx
                        val sizeDeltaX = if (isLeft) -dx else dx
                        val sizeDeltaY = if (isTop) -dy else dy
                        val sizeDelta = (sizeDeltaX + sizeDeltaY) / 2f
                        
                        var newSize = cropSize + sizeDelta
                        if (newSize < 0.20f) newSize = 0.20f
                        
                        val maxW = if (isLeft) cropX + cropSize else 1f - cropX
                        val maxH = if (isTop) cropY + cropSize else 1f - cropY
                        val maxSize = kotlin.math.min(maxW, maxH)
                        if (newSize > maxSize) newSize = maxSize
                        
                        cropX = if (isLeft) cropX + cropSize - newSize else cropX
                        cropY = if (isTop) cropY + cropSize - newSize else cropY
                        cropSize = newSize
                    }

                    // TOP-LEFT handle
                    Box(
                        modifier = Modifier
                            .size(handleSizeDp)
                            .offset(
                                x = (cropX * canvasWidthPx).pxToDp(density) - handleSizeDp/2,
                                y = (cropY * canvasHeightPx).pxToDp(density) - handleSizeDp/2
                            )
                            .pointerInput(canvasWidthPx, canvasHeightPx) {
                                detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, true, true) }
                            }
                    )

                    // BOTTOM-RIGHT handle
                    Box(
                        modifier = Modifier
                            .size(handleSizeDp)
                            .offset(
                                x = ((cropX + cropSize) * canvasWidthPx).pxToDp(density) - handleSizeDp/2,
                                y = ((cropY + cropSize) * canvasHeightPx).pxToDp(density) - handleSizeDp/2
                            )
                            .pointerInput(canvasWidthPx, canvasHeightPx) {
                                detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, false, false) }
                            }
                    )

                    // TOP-RIGHT handle
                    Box(
                        modifier = Modifier
                            .size(handleSizeDp)
                            .offset(
                                x = ((cropX + cropSize) * canvasWidthPx).pxToDp(density) - handleSizeDp/2,
                                y = (cropY * canvasHeightPx).pxToDp(density) - handleSizeDp/2
                            )
                            .pointerInput(canvasWidthPx, canvasHeightPx) {
                                detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, false, true) }
                            }
                    )

                    // BOTTOM-LEFT handle
                    Box(
                        modifier = Modifier
                            .size(handleSizeDp)
                            .offset(
                                x = (cropX * canvasWidthPx).pxToDp(density) - handleSizeDp/2,
                                y = ((cropY + cropSize) * canvasHeightPx).pxToDp(density) - handleSizeDp/2
                            )
                            .pointerInput(canvasWidthPx, canvasHeightPx) {
                                detectDragGestures { _, drag -> updateCrop(drag.x, drag.y, true, false) }
                            }
                    )
                }

                // Hint row
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.OpenWith, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Drag inside to move · drag corners to resize", color = TextSecondary, fontSize = 11.sp)
                }
            }
        }
    }
}

// ── px/dp conversion helpers used in BoxWithConstraints ──
private val density: Float
    @Composable get() = LocalDensity.current.density

private fun Float.pxToDp(density: Float) = (this / density).dp
private fun Dp.toPx(density: Float) = (this.value * density)
