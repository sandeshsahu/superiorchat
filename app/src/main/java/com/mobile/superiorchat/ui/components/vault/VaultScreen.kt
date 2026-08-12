package com.mobile.superiorchat.ui.components.vault

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.media.VaultManager
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.SkeletonContainer
import com.mobile.superiorchat.ui.components.media.GalleryGrid
import com.mobile.superiorchat.ui.components.media.MediaViewer
import com.mobile.superiorchat.ui.GlobalDialogState
import com.mobile.superiorchat.utils.PermissionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Hidden Media Vault — shown when the safeguard PIN (1234) is entered.
 *
 * Interaction:
 *  • Tap   → opens MediaViewer fullscreen
 *  • Hold  → preview (MediaViewer while held, dismiss on release)
 *  • Unhide button (AppBar) → enters selection mode
 *  • Selection mode: tap items to check, floating Unhide (N) button at bottom
 *  • (+) FAB → permission check → GalleryGrid picker → files moved to vault
 *  • × or back → finishAndRemoveTask()
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    permissionHandler: PermissionHandler,
    onGlobalDialog: (GlobalDialogState) -> Unit
) {
    val context = LocalContext.current
    val prefs = AppGraph.prefs
    val scope = rememberCoroutineScope()

    // ── State ─────────────────────────────────────────────────────────────────

    var vaultItems by remember { mutableStateOf<List<File>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSelectMode by remember { mutableStateOf(false) }
    val selectedPaths = remember { mutableStateListOf<String>() }
    var viewerPath by remember { mutableStateOf<String?>(null) }
    var viewerType by remember { mutableStateOf("photo") }
    var showPicker by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isUnhiding by remember { mutableStateOf(false) }
    var cameraUri by remember { mutableStateOf<android.net.Uri?>(null) }

    fun refreshVault(showLoading: Boolean = false) {
        scope.launch(Dispatchers.IO) {
            if (showLoading) {
                withContext(Dispatchers.Main) { isLoading = true }
            }
            val items = VaultManager.getVaultItems(prefs)
            withContext(Dispatchers.Main) {
                vaultItems = items
                isLoading = false
            }
        }
    }

    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success) {
                cameraUri?.let { uri ->
                    scope.launch {
                        showPicker = false
                        isLoading = true
                        var hidden = 0
                        withContext(Dispatchers.IO) {
                            if (VaultManager.hideMedia(context, uri, prefs)) hidden++
                        }
                        statusMessage = if (hidden > 0) "Saved camera photo to vault" else "Failed to save photo"
                        refreshVault(showLoading = false)
                    }
                }
            }
        }
    )

    LaunchedEffect(Unit) { refreshVault() }

    LaunchedEffect(statusMessage) {
        if (statusMessage != null) {
            delay(2500)
            statusMessage = null
        }
    }

    // ── Back handler ──────────────────────────────────────────────────────────

    BackHandler {
        when {
            isSelectMode -> { isSelectMode = false; selectedPaths.clear() }
            showPicker   -> showPicker = false
            else         -> (context as? Activity)?.finishAndRemoveTask()
        }
    }

    // ── Root ──────────────────────────────────────────────────────────────────

    Box(modifier = Modifier.fillMaxSize().background(Background)) {

        // ── Content ───────────────────────────────────────────────────────────
        Column(modifier = Modifier.fillMaxSize()) {

            // TopAppBar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .height(64.dp)
                    .background(Background)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon
                IconButton(
                    onClick = {
                        if (isSelectMode) { isSelectMode = false; selectedPaths.clear() }
                        else (context as? Activity)?.finishAndRemoveTask()
                    }
                ) {
                    Icon(
                        imageVector = if (isSelectMode) Icons.Default.Close else Icons.Default.ArrowBack, 
                        contentDescription = "Close", 
                        tint = TextPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))

                // Title
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = if (isSelectMode) "${selectedPaths.size} selected" else "Hidden Media",
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                        },
                        label = "vault_title"
                    ) { title ->
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = (-0.5).sp
                        )
                    }
                    if (!isSelectMode) {
                        Text(
                            text = "${vaultItems.size} items",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Actions
                if (!isSelectMode && vaultItems.isNotEmpty()) {
                    IconButton(onClick = { isSelectMode = true; selectedPaths.clear() }) {
                        Icon(Icons.Default.Checklist, contentDescription = "Select", tint = TextPrimary)
                    }
                }
                
                if (!isSelectMode) {
                    IconButton(onClick = { 
                        val hasManageStorage = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                            android.os.Environment.isExternalStorageManager()
                        } else {
                            androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_GRANTED
                        }
                        
                        if (!hasManageStorage) {
                            val intent = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                            } else {
                                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                            }
                            onGlobalDialog(GlobalDialogState.ManageStorageRequired(intent))
                        } else {
                            showPicker = true
                        }
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Add", tint = PrimaryLight)
                    }
                }
            }

            // ── Loading ───────────────────────────────────────────────────────
            if (isLoading) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(12) {
                        SkeletonContainer(modifier = Modifier.aspectRatio(1f), cornerRadius = 0.dp)
                    }
                }
            }

            // ── Empty state ───────────────────────────────────────────────────
            if (!isLoading && vaultItems.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(SurfaceLevel2),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Text("Vault is Empty", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Media added to the vault will be hidden from the device gallery.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 48.dp)
                        )
                    }
                }
            }

            // ── Vault grid ────────────────────────────────────────────────────
            if (!isLoading && vaultItems.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(
                        start = 2.dp, end = 2.dp,
                        top = 2.dp,
                        bottom = if (isSelectMode) 96.dp else 2.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vaultItems, key = { it.absolutePath }) { file ->
                        VaultTile(
                            file = file,
                            isSelectMode = isSelectMode,
                            isSelected = selectedPaths.contains(file.absolutePath),
                            onTap = {
                                if (isSelectMode) {
                                    if (selectedPaths.contains(file.absolutePath))
                                        selectedPaths.remove(file.absolutePath)
                                    else
                                        selectedPaths.add(file.absolutePath)
                                } else {
                                    viewerType = if (VaultManager.isVideoFile(file)) "video" else "photo"
                                    viewerPath = file.absolutePath
                                }
                            },
                            onHoldStart = {
                                if (!isSelectMode) {
                                    viewerType = if (VaultManager.isVideoFile(file)) "video" else "photo"
                                    viewerPath = file.absolutePath
                                }
                            },
                            onHoldEnd = {
                                if (!isSelectMode) viewerPath = null
                            }
                        )
                    }
                }
            }
        }

        // FAB Removed (Moved to App Bar)

        // ── Unhide bottom bar (selection mode) ────────────────────────────────
        AnimatedVisibility(
            visible = isSelectMode,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Button(
                onClick = {
                    if (selectedPaths.isEmpty()) return@Button
                    isUnhiding = true
                    val toUnhide = selectedPaths.toList()
                    scope.launch {
                        var success = 0
                        toUnhide.forEach { path ->
                            if (VaultManager.unhideMedia(context, path, prefs)) success++
                        }
                        isUnhiding = false
                        isSelectMode = false
                        selectedPaths.clear()
                        statusMessage = if (success > 0) "Restored $success item(s) to Pictures" else "Could not restore items"
                        refreshVault()
                    }
                },
                enabled = selectedPaths.isNotEmpty() && !isUnhiding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryLight,
                    disabledContainerColor = SurfaceLevel2,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                shape = RoundedCornerShape(28.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                if (isUnhiding) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                } else {
                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    AnimatedContent(
                        targetState = selectedPaths.size,
                        transitionSpec = {
                            (slideInVertically { -it } + fadeIn()).togetherWith(slideOutVertically { it } + fadeOut())
                        },
                        label = "unhide_count"
                    ) { count ->
                        Text(
                            if (count == 0) "Select items to unhide" else "Unhide ($count)",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }

        // ── Status snackbar ───────────────────────────────────────────────────
        AnimatedVisibility(
            visible = statusMessage != null,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 88.dp, start = 16.dp, end = 16.dp),
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceLevel2)
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Text(statusMessage ?: "", color = TextPrimary, fontSize = 14.sp)
            }
        }
    }

    // ── Gallery picker overlay ────────────────────────────────────────────────
    AnimatedVisibility(
        visible = showPicker,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = Modifier.zIndex(10f)
    ) {
        GalleryGrid(
            confirmLabel = "Hide",
            showCaption = false,
            onDismiss = { showPicker = false },
            onMediaSelected = { selectedItems, _ ->
                scope.launch {
                    var hidden = 0
                    showPicker = false
                    isLoading = true // Show skeleton while hiding and loading
                    
                    withContext(Dispatchers.IO) {
                        selectedItems.forEach { item ->
                            if (VaultManager.hideMedia(context, item.uri, prefs)) hidden++
                        }
                    }
                    statusMessage = if (hidden > 0) "Hidden $hidden item(s) from gallery" else "Failed to hide items"
                    refreshVault(showLoading = false) // Already loading
                }
                true
            },
            onCameraClick = {
                permissionHandler.requestCamera {
                    val imageFile = File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
                    val uri = androidx.core.content.FileProvider.getUriForFile(
                        context, 
                        "${context.packageName}.provider", 
                        imageFile
                    )
                    cameraUri = uri
                    cameraLauncher.launch(uri)
                }
            }
        )
    }

    // ── MediaViewer ───────────────────────────────────────────────────────────
    MediaViewer(
        mediaPath = viewerPath,
        mediaType = viewerType,
        mediaIndex = vaultItems.indexOfFirst { it.absolutePath == viewerPath }.coerceAtLeast(0),
        onDismiss = { viewerPath = null },
        onSwipeLeft = {
            val currentIndex = vaultItems.indexOfFirst { it.absolutePath == viewerPath }
            if (currentIndex in 0 until vaultItems.lastIndex) {
                val nextFile = vaultItems[currentIndex + 1]
                viewerPath = nextFile.absolutePath
                viewerType = if (VaultManager.isVideoFile(nextFile)) "video" else "photo"
                true
            } else false
        },
        onSwipeRight = {
            val currentIndex = vaultItems.indexOfFirst { it.absolutePath == viewerPath }
            if (currentIndex > 0) {
                val prevFile = vaultItems[currentIndex - 1]
                viewerPath = prevFile.absolutePath
                viewerType = if (VaultManager.isVideoFile(prevFile)) "video" else "photo"
                true
            } else false
        }
    )
}

// ── Vault tile composable ─────────────────────────────────────────────────────

@Composable
private fun VaultTile(
    file: File,
    isSelectMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldEnd: () -> Unit
) {
    val context = LocalContext.current
    val isVideo = VaultManager.isVideoFile(file)

    val tileScale by animateFloatAsState(
        targetValue = if (isSelected) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.7f),
        label = "vault_tile_scale"
    )

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .background(SurfaceLevel2)
            .scale(tileScale)
            .pointerInput(isSelectMode) {
                detectTapGestures(
                    onPress = {
                        if (isSelectMode) {
                            val released = tryAwaitRelease()
                            if (released) onTap()
                        } else {
                            val holdJob = kotlinx.coroutines.CoroutineScope(Dispatchers.Main).launch {
                                delay(150)
                                onHoldStart()
                            }
                            val released = tryAwaitRelease()
                            holdJob.cancel()
                            if (released && !holdJob.isCompleted) {
                                onTap() // clean tap
                            } else {
                                onHoldEnd() // hold released
                            }
                        }
                    }
                )
            }
    ) {
        // Thumbnail
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(file)
                .size(300)
                .bitmapConfig(android.graphics.Bitmap.Config.RGB_565)
                .apply { if (isVideo) videoFrameMillis(1000) }
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // Selection dim
        if (isSelectMode && isSelected) {
            Box(modifier = Modifier.fillMaxSize().background(PrimaryLight.copy(alpha = 0.35f)))
        }

        // Video play badge
        if (isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                    Text("VIDEO", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Checkbox (selection mode)
        AnimatedVisibility(
            visible = isSelectMode,
            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
            enter = scaleIn(animationSpec = tween(150)) + fadeIn(),
            exit = scaleOut(animationSpec = tween(100)) + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryLight else Color.Black.copy(alpha = 0.5f))
                    .border(1.5.dp, if (isSelected) PrimaryLight else Color.White.copy(alpha = 0.6f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                AnimatedVisibility(visible = isSelected, enter = scaleIn(), exit = scaleOut()) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(13.dp))
                }
            }
        }
    }
}
