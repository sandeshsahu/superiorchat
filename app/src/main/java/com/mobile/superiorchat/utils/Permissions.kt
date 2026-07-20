package com.mobile.superiorchat.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.mobile.superiorchat.ui.GlobalDialogState

class PermissionHandler(
    private val context: Context,
    private val onShowGlobalDialog: (GlobalDialogState) -> Unit,
    private val singleLauncher: ManagedActivityResultLauncher<String, Boolean>,
    private val multipleLauncher: ManagedActivityResultLauncher<Array<String>, Map<String, @JvmSuppressWildcards Boolean>>,
    private val currentSingleCallback: MutableState<((Boolean) -> Unit)?>,
    private val currentMultipleCallback: MutableState<((Map<String, Boolean>) -> Unit)?>
) {
    /**
     * Request camera permission and handle rationale/denial automatically.
     */
    fun requestCamera(onGranted: () -> Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            onGranted()
            return
        }
        onShowGlobalDialog(
            GlobalDialogState.CameraPermissionRationale(
                onConfirm = {
                    currentSingleCallback.value = { isGranted ->
                        if (isGranted) {
                            onGranted()
                        } else {
                            showRationaleOrDenied(Manifest.permission.CAMERA)
                        }
                    }
                    singleLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        )
    }

    /**
     * Request audio permission and handle rationale/denial automatically.
     */
    fun requestAudio(onGranted: () -> Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            onGranted()
            return
        }
        onShowGlobalDialog(
            GlobalDialogState.MicrophonePermissionRationale(
                onConfirm = {
                    currentSingleCallback.value = { isGranted ->
                        if (isGranted) {
                            onGranted()
                        } else {
                            showRationaleOrDenied(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                    singleLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        )
    }

    /**
     * Request notification permission if API 33+, else invoke onGranted directly.
     * @param showDenial If true, shows the permanently denied popup if they reject it. Set to false for startup prompts.
     */
    fun requestNotification(showDenial: Boolean = true, onGranted: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (hasPerm) {
                onGranted()
                return
            }
            currentSingleCallback.value = { isGranted ->
                if (isGranted) {
                    onGranted()
                } else if (showDenial) {
                    showRationaleOrDenied(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
            singleLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onGranted()
        }
    }

    /**
     * Request storage permissions intelligently handling API versions 
     * (TIRAMISU for READ_MEDIA_IMAGES, UPSIDE_DOWN_CAKE for visual_user_selected, and legacy READ_EXTERNAL_STORAGE).
     */
    fun requestStorageForMedia(onGranted: () -> Unit) {
        val images = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val storage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        
        val hasFullAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) images else storage
        if (hasFullAccess) {
            onGranted()
            return
        }

        val hasPartialAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED
        } else false

        val permsToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_AUDIO
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val launchAction = {
            currentMultipleCallback.value = { results ->
                val imagesGranted = results[Manifest.permission.READ_MEDIA_IMAGES] == true
                val storageGranted = results[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                val partialGranted = results[Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED] == true

                if (imagesGranted || storageGranted) {
                    onGranted()
                } else if (partialGranted) {
                    onGranted()
                } else {
                    val fullPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
                    showRationaleOrDenied(fullPerm)
                }
            }
            multipleLauncher.launch(permsToCheck)
        }

        if (hasPartialAccess) {
            val fullPerm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) Manifest.permission.READ_MEDIA_IMAGES else Manifest.permission.READ_EXTERNAL_STORAGE
            val activity = context as? Activity
            
            if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, fullPerm)) {
                onShowGlobalDialog(GlobalDialogState.PartialMediaAccessPermanentlyDenied(
                    onContinue = onGranted,
                    onGoToSettings = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }
                ))
            } else {
                onShowGlobalDialog(GlobalDialogState.PartialMediaAccess(
                    onContinue = onGranted,
                    onUpgrade = launchAction
                ))
            }
        } else {
            onShowGlobalDialog(GlobalDialogState.StoragePermissionRationale(
                onConfirm = launchAction
            ))
        }
    }

    /**
     * Generic permission launch (useful for AppNav's refresh logic).
     */
    fun launchSingle(permission: String, onResult: (Boolean) -> Unit) {
        currentSingleCallback.value = onResult
        singleLauncher.launch(permission)
    }

    fun launchMultiple(permissions: Array<String>, onResult: (Map<String, Boolean>) -> Unit) {
        currentMultipleCallback.value = onResult
        multipleLauncher.launch(permissions)
    }
    
    fun showRationaleOrDenied(permission: String) {
        val activity = context as? Activity
        if (activity != null && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            onShowGlobalDialog(
                GlobalDialogState.PermissionPermanentlyDenied(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                )
            )
        }
    }
}

@Composable
fun rememberPermissionHandler(
    onShowGlobalDialog: (GlobalDialogState) -> Unit
): PermissionHandler {
    val context = LocalContext.current
    
    // We hold references to the callbacks so we can swap them dynamically.
    val currentSingleCallback = remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val currentMultipleCallback = remember { mutableStateOf<((Map<String, Boolean>) -> Unit)?>(null) }

    val singleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { result ->
        currentSingleCallback.value?.invoke(result)
    }

    val multipleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        currentMultipleCallback.value?.invoke(results)
    }

    return remember(context, onShowGlobalDialog) {
        PermissionHandler(
            context = context,
            onShowGlobalDialog = onShowGlobalDialog,
            singleLauncher = singleLauncher,
            multipleLauncher = multipleLauncher,
            currentSingleCallback = currentSingleCallback,
            currentMultipleCallback = currentMultipleCallback
        )
    }
}
