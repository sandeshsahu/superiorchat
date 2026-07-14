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
import com.mobile.superiorutils.data.repository.LocalFileItem
import com.mobile.superiorutils.data.repository.LocalMediaItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import android.os.Build
import android.os.Environment
import android.content.Intent
import android.provider.Settings

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun FileExplorer(
    viewModel: ChatViewModel,
    onDismiss: () -> Unit,
    onFilesSelected: (List<File>) -> Boolean,
    onSystemPickerClick: () -> Unit,
    onBottomBarVisibilityChanged: (Boolean) -> Unit,
    onRequestManageStoragePermission: () -> Unit
) {
    val context = LocalContext.current
    var explorerDirectory by remember { mutableStateOf<File?>(null) }
    val selectedFiles = remember { mutableStateListOf<File>() }
    
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    
    LaunchedEffect(Unit) {
        delay(250) // Wait for Dialog entry slide-up animation to finish to prevent lag
        viewModel.loadRecentFiles(context)
        isLoading = false
    }

    val isExploring = explorerDirectory != null

    // Notify bottom bar visibility changes
    LaunchedEffect(isExploring, isSearching) {
        onBottomBarVisibilityChanged(!isExploring && !isSearching)
    }

    // Inner back handler for folder browsing
    BackHandler(enabled = isExploring || isSearching) {
        if (isSearching) {
            isSearching = false
            searchQuery = ""
        } else if (isExploring) {
            val parent = explorerDirectory!!.parentFile
            val rootPath = android.os.Environment.getExternalStorageDirectory().absolutePath
            if (parent == null || explorerDirectory!!.absolutePath == rootPath) {
                explorerDirectory = null
            } else {
                explorerDirectory = parent
                viewModel.openDirectory(context, parent)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearching) {
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            decorationBox = { innerTextField ->
                                if (searchQuery.isEmpty()) {
                                    Text("Search files...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium)
                                }
                                innerTextField()
                            }
                        )
                    } else {
                        Text(
                            text = if (isExploring) explorerDirectory!!.name else "Select Files",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (isSearching) {
                                isSearching = false
                                searchQuery = ""
                            } else if (isExploring) {
                                val parent = explorerDirectory!!.parentFile
                                val rootPath = android.os.Environment.getExternalStorageDirectory().absolutePath
                                if (parent == null || explorerDirectory!!.absolutePath == rootPath) {
                                    explorerDirectory = null
                                } else {
                                    explorerDirectory = parent
                                    viewModel.openDirectory(context, parent)
                                }
                            } else {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearching || isExploring) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = "Back or Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    if (isSearching) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        IconButton(onClick = { isSearching = true }) {
                            Icon(Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedFiles.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    text = { Text("Send (${selectedFiles.size})", color = Color.Black, fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black) },
                    onClick = {
                        val success = onFilesSelected(selectedFiles.toList())
                        if (success) {
                            onDismiss()
                        }
                    },
                    containerColor = PrimaryLight,
                    elevation = FloatingActionButtonDefaults.elevation(8.dp)
                )
            }
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryLight)
                }
            } else {
                // Slide animation for folder navigation
                AnimatedContent(
                    targetState = explorerDirectory,
                    transitionSpec = {
                        slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                    },
                    label = "folder_navigation_transition",
                    modifier = Modifier.fillMaxSize()
                ) { currentDir ->
                    if (currentDir == null) {
                        // Root View
                        val filteredRecent = if (searchQuery.isBlank()) {
                            viewModel.recentFiles
                        } else {
                            viewModel.recentFiles.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Recent files section
                            if (filteredRecent.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "RECENT FILES",
                                        color = PrimaryLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                    )
                                }
                                
                                items(filteredRecent, key = { it.path }) { file ->
                                    val fileObj = File(file.path)
                                    val isSelected = selectedFiles.contains(fileObj)
                                    FileListItem(
                                        item = file,
                                        isSelected = isSelected,
                                        onClick = {
                                            if (isSelected) {
                                                selectedFiles.remove(fileObj)
                                            } else {
                                                selectedFiles.add(fileObj)
                                            }
                                        }
                                    )
                                }
                            }

                            // Browse storage sections (only when not searching)
                            if (!isSearching) {
                                item {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "BROWSE STORAGE",
                                        color = PrimaryLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
                                    )
                                }

                                // Internal Storage
                                item {
                                    StorageLocationItem(
                                        icon = Icons.Default.Smartphone,
                                        title = "Internal Storage",
                                        subtitle = "Browse device files",
                                        iconColor = PrimaryLight,
                                        onClick = {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                                                onRequestManageStoragePermission()
                                            } else {
                                                val dir = Environment.getExternalStorageDirectory()
                                                explorerDirectory = dir
                                                viewModel.openDirectory(context, dir)
                                            }
                                        }
                                    )
                                }

                                // System Picker Fallback
                                item {
                                    StorageLocationItem(
                                        icon = Icons.Default.OpenInNew,
                                        title = "System File Picker",
                                        subtitle = "Browse all apps and cloud",
                                        iconColor = MaterialTheme.colorScheme.onSurface,
                                        onClick = onSystemPickerClick
                                    )
                                }
                            }
                        }
                    } else {
                        // Directory Explorer View
                        val filteredFilesList = if (searchQuery.isBlank()) {
                            viewModel.explorerFilesList
                        } else {
                            viewModel.explorerFilesList.filter { it.name.lowercase().contains(searchQuery.lowercase()) }
                        }

                        if (filteredFilesList.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Folder is empty",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredFilesList, key = { it.path }) { item ->
                                    val fileObj = File(item.path)
                                    val isSelected = selectedFiles.contains(fileObj)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (item.isDirectory) {
                                                    explorerDirectory = fileObj
                                                    viewModel.openDirectory(context, fileObj)
                                                } else {
                                                    if (isSelected) {
                                                        selectedFiles.remove(fileObj)
                                                    } else {
                                                        selectedFiles.add(fileObj)
                                                    }
                                                }
                                            }
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val icon = if (item.isDirectory) Icons.Default.Folder else com.mobile.superiorutils.utils.FileUtils.resolveFileIcon(item.name)
                                        val iconColor = if (item.isDirectory) PrimaryLight else com.mobile.superiorutils.utils.FileUtils.resolveFileIconColor(item.name)

                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(iconColor.copy(alpha = 0.1f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = "File Type",
                                                tint = iconColor,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.width(16.dp))
                                        
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = item.name,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (!item.isDirectory) {
                                                Text(
                                                    text = "${item.size} • ${item.dateModified}",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            } else {
                                                Text(
                                                    text = "Directory",
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                )
                                            }
                                        }

                                        if (!item.isDirectory) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) PrimaryLight else Color.Transparent
                                                    )
                                                    .border(1.5.dp, if (isSelected) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (isSelected) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = Color.Black,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FileListItem(
    item: LocalFileItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = com.mobile.superiorutils.utils.FileUtils.resolveFileIcon(item.name)
        val iconColor = com.mobile.superiorutils.utils.FileUtils.resolveFileIconColor(item.name)
        
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "File Icon",
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${item.size} • Modified ${item.dateModified}",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) PrimaryLight else Color.Transparent
                )
                .border(1.5.dp, if (isSelected) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = Color.Black,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun StorageLocationItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

