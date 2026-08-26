package com.mobile.superiorchat.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.mobile.superiorchat.theme.*
import com.mobile.superiorchat.ui.components.bounceClick
import com.mobile.superiorchat.utils.PermissionHandler

// ══════════════════════════════════════════════════════════
//  Permission Data Model
// ══════════════════════════════════════════════════════════

data class PermissionState(
    val name: String,
    val isGranted: Boolean,
    val displayStatus: String? = null,
    val buttonText: String = "Grant",
    val onClick: () -> Unit
)

// ══════════════════════════════════════════════════════════
//  Self-Contained Permissions Screen
// ══════════════════════════════════════════════════════════

@Composable
fun PermissionsScreen(
    viewModel: MainViewModel,
    permissionHandler: PermissionHandler,
    requestPostNotifications: () -> Unit
) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val permissionStatus by viewModel.permissionStatus.collectAsState()

    val permissions = remember(permissionStatus) {
        listOf(
            PermissionState(
                name = "Post Notifications",
                isGranted = permissionStatus.hasPostNotifs,
                buttonText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.POST_NOTIFICATIONS)
                    } == true) "Retry" else "Grant"
            ) { requestPostNotifications() },
            PermissionState(
                name = "Camera",
                isGranted = permissionStatus.hasCamera,
                buttonText = if (activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
                    } == true) "Retry" else "Grant"
            ) {
                permissionHandler.requestCamera { viewModel.refreshPermissions() }
            },
            PermissionState(
                name = "Microphone",
                isGranted = permissionStatus.hasMicrophone,
                buttonText = if (activity?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
                    } == true) "Retry" else "Grant"
            ) {
                permissionHandler.requestAudio { viewModel.refreshPermissions() }
            },
            PermissionState(
                name = "Media & Storage",
                isGranted = permissionStatus.mediaAccessLevel == MediaAccessLevel.FULL,
                displayStatus = when (permissionStatus.mediaAccessLevel) {
                    MediaAccessLevel.FULL -> "Granted"
                    MediaAccessLevel.PARTIAL -> "Partial Access"
                    MediaAccessLevel.NONE -> "Required"
                },
                buttonText = if (activity?.let {
                        val permsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
                        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
                            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
                        } else {
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                        permsToCheck.any { perm -> ActivityCompat.shouldShowRequestPermissionRationale(it, perm) }
                    } == true) "Retry" else "Grant"
            ) {
                permissionHandler.requestStorageForMedia { viewModel.refreshPermissions() }
            },
            PermissionState("Ignore Battery Optimizations", permissionStatus.hasIgnoreBattery) {
                context.startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:${context.packageName}")))
            },
            PermissionState("Install Unknown Apps", permissionStatus.hasInstallPackages) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}")))
                }
            },
            PermissionState("All Files Access", permissionStatus.hasManageStorage) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    try {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:${context.packageName}")))
                    } catch (e: Exception) {
                        context.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp)
            .padding(top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "All permissions must be granted for full functionality.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        items(permissions) { perm ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceLevel2, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(8.dp).background(if (perm.isGranted) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant, CircleShape))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            perm.name,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            perm.displayStatus ?: if (perm.isGranted) "Granted" else "Required",
                            fontSize = 11.sp,
                            color = if (perm.isGranted) PrimaryLight else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!perm.isGranted) {
                    Box(
                        modifier = Modifier
                            .bounceClick(scaleDown = 0.95f) { perm.onClick() }
                            .background(PrimaryLight, RoundedCornerShape(10.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(perm.buttonText, color = MaterialTheme.colorScheme.onPrimaryContainer, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .background(PrimaryLight.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("\u2713 Active", color = PrimaryLight, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
