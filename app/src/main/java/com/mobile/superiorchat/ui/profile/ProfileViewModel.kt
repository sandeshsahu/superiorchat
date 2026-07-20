package com.mobile.superiorchat.ui.profile

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
        val expectedBotId = token.substringBefore(":")
        
        viewModelScope.launch(Dispatchers.IO) {
            // Load from Cache immediately
            val cachedProfile = AppGraph.appRepository.getProfileSync(expectedBotId)
            if (cachedProfile != null) {
                botId = cachedProfile.chatId
                displayName = cachedProfile.title
                username = cachedProfile.username
                description = cachedProfile.bio ?: ""
                shortDescription = "" // Can be added to DB later if needed
                avatarUri = if (cachedProfile.profilePhotoPath.isNotBlank()) Uri.parse(cachedProfile.profilePhotoPath) else null
            } else {
                isLoading = true
            }
            
            // Sync from network
            try {
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
                    description = desc
                    shortDescription = shortDesc
                    
                    if (photoUrl != null) {
                        avatarUri = Uri.parse(photoUrl)
                    } else {
                        avatarUri = null
                    }
                    
                    // Update cache
                    val userProfile = com.mobile.superiorchat.data.entity.UserProfile(
                        chatId = botId,
                        title = displayName,
                        username = username,
                        type = "bot",
                        profilePhotoPath = photoUrl ?: "",
                        photoUniqueId = "",
                        bio = description,
                        inviteLink = null,
                        hasProtectedContent = false,
                        isForum = false
                    )
                    AppGraph.appRepository.insertProfile(userProfile)
                }
            } catch (e: Exception) {
                // Silently ignore network errors during sync, relying on cache
            } finally {
                isLoading = false
            }
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
                val tempFile = com.mobile.superiorchat.utils.FileUtils.cropAndScaleImage(
                    context = context,
                    uri = uri,
                    cropX = cropX,
                    cropY = cropY,
                    cropSize = cropSize,
                    targetWidth = 512,
                    targetHeight = 512
                )
                
                if (tempFile != null) {
                    val success = TelegramApi.setMyProfilePhoto(token, tempFile)
                    if (success) {
                        avatarUri = Uri.fromFile(tempFile)
                        StatusFlow.reportStatus(SyncState.SUCCESS, "Saved")
                    } else {
                        StatusFlow.reportStatus(SyncState.ERROR, "Upload failed")
                    }
                } else {
                    StatusFlow.reportStatus(SyncState.ERROR, "Failed to crop image")
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
