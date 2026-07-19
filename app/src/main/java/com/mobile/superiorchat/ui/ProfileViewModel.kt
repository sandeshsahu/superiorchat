package com.mobile.superiorchat.ui

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.superiorchat.bot.TelegramApi
import com.mobile.superiorchat.core.AppGraph
import com.mobile.superiorchat.core.NetState
import com.mobile.superiorchat.core.StatusFlow
import com.mobile.superiorchat.core.SyncState
import com.mobile.superiorchat.utils.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppGraph.prefs

    var displayName by mutableStateOf("")
        private set
    var username by mutableStateOf("")
        private set
    var botId by mutableStateOf("")
        private set
    var description by mutableStateOf("")
        private set
    var shortDescription by mutableStateOf("")
        private set
    var avatarUri by mutableStateOf<Uri?>(null)
        private set

    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var rateLimitError by mutableStateOf<String?>(null)

    fun clearError() {
        rateLimitError = null
    }

    fun loadProfile() {
        val token = prefs.botToken
        if (token.isBlank()) return
        
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            
            // Fetch everything in parallel
            val meDeferred = async { TelegramApi.getMe(token) }
            val descDeferred = async { TelegramApi.getMyDescription(token) }
            val shortDescDeferred = async { TelegramApi.getMyShortDescription(token) }
            val photoDeferred = async { TelegramApi.getMyProfilePhotoUrl(token) }
            
            val me = meDeferred.await()
            val desc = descDeferred.await()
            val shortDesc = shortDescDeferred.await()
            val photoUrl = photoDeferred.await()
            
            if (me != null) {
                botId = me.result?.id?.toString() ?: ""
                displayName = me.result?.first_name ?: ""
                username = me.result?.username ?: ""
            }
            description = desc
            shortDescription = shortDesc
            
            if (photoUrl != null) {
                // We just store the URL as Uri, AsyncImage will handle downloading and caching
                avatarUri = Uri.parse(photoUrl)
            } else {
                avatarUri = null
            }
            
            isLoading = false
        }
    }

    fun saveInfo(name: String, desc: String, shortDesc: String) {
        val token = prefs.botToken
        if (token.isBlank()) return
        if (!NetState.isOnline.value) {
            StatusFlow.reportStatus(SyncState.ERROR, "You are offline")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isSaving = true
            
            try {
                var nameSuccess = true
                if (name != displayName) {
                    nameSuccess = TelegramApi.setMyName(token, name)
                    if (nameSuccess) displayName = name
                }
                
                var descSuccess = true
                if (desc != description) {
                    descSuccess = TelegramApi.setMyDescription(token, desc)
                    if (descSuccess) description = desc
                }
                
                var shortDescSuccess = true
                if (shortDesc != shortDescription) {
                    shortDescSuccess = TelegramApi.setMyShortDescription(token, shortDesc)
                    if (shortDescSuccess) shortDescription = shortDesc
                }
                
                if (nameSuccess && descSuccess && shortDescSuccess) {
                    StatusFlow.reportStatus(SyncState.SUCCESS, "Saved")
                } else {
                    StatusFlow.reportStatus(SyncState.ERROR, "Update failed")
                }
            } catch (e: com.mobile.superiorchat.bot.RateLimitException) {
                val expiryMillis = System.currentTimeMillis() + (e.retryAfterSeconds * 1000L)
                prefs.profileEditRateLimitExpiry = expiryMillis
                val hours = e.retryAfterSeconds / 3600
                val mins = (e.retryAfterSeconds % 3600) / 60
                val timeStr = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
                rateLimitError = "Telegram rate limit reached. Please try again in $timeStr."
                StatusFlow.reportStatus(SyncState.ERROR, "Rate Limited")
            } catch (e: Exception) {
                rateLimitError = e.message ?: "Failed to update profile info."
                StatusFlow.reportStatus(SyncState.ERROR, "Update failed")
            } finally {
                isSaving = false
            }
        }
    }

    fun uploadProfilePhoto(context: Context, uri: Uri, cropX: Float, cropY: Float, cropSize: Float) {
        val token = prefs.botToken
        if (token.isBlank()) return
        if (!NetState.isOnline.value) {
            StatusFlow.reportStatus(SyncState.ERROR, "Offline")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            isSaving = true
            try {
                // 1. Load bitmap
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                if (originalBitmap != null) {
                    val width = originalBitmap.width
                    val height = originalBitmap.height
                    val minDim = Math.min(width, height)
                    
                    // 2. Apply the user's fractional crop box on the original image
                    val pixelCropX = (cropX * width).toInt().coerceIn(0, width - 1)
                    val pixelCropY = (cropY * height).toInt().coerceIn(0, height - 1)
                    val maxPossibleSize = Math.min(width - pixelCropX, height - pixelCropY)
                    val pixelCropSize = (cropSize * minDim).toInt().coerceAtMost(maxPossibleSize)
                    
                    val croppedBitmap = if (pixelCropSize > 0) {
                         Bitmap.createBitmap(originalBitmap, pixelCropX, pixelCropY, pixelCropSize, pixelCropSize)
                    } else originalBitmap

                    // 4. Scale to 512x512
                    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, 512, 512, true)
                    
                    // 4. Save to temp file
                    val tempFile = File(context.cacheDir, "profile_upload_${System.currentTimeMillis()}.jpg")
                    val outputStream = FileOutputStream(tempFile)
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()
                    
                    // 5. Upload via TelegramApi
                    val success = TelegramApi.setMyProfilePhoto(token, tempFile)
                    
                    if (success) {
                        avatarUri = Uri.fromFile(tempFile)
                        StatusFlow.reportStatus(SyncState.SUCCESS, "Saved")
                    } else {
                        StatusFlow.reportStatus(SyncState.ERROR, "Upload failed")
                    }
                    
                    // Clean up original bitmap to save memory
                    originalBitmap.recycle()
                    if (croppedBitmap != originalBitmap) croppedBitmap.recycle()
                    scaledBitmap.recycle()
                } else {
                    StatusFlow.reportStatus(SyncState.ERROR, "Failed to load image")
                }
            } catch (e: Exception) {
                StatusFlow.reportStatus(SyncState.ERROR, "Crop failed")
            } finally {
                isSaving = false
            }
        }
    }

    fun removeProfilePhoto() {
        val token = prefs.botToken
        if (token.isBlank()) return
        StatusFlow.reportStatus(SyncState.ERROR, "Removing profile photos is only supported via @BotFather in Telegram.")
    }
}
