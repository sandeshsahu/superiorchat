package com.mobile.superiorutils.media

import android.content.Context
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import java.util.UUID

object MediaSync {

    fun enqueueDownload(
        context: Context,
        messageId: Long,
        fileId: String,
        mediaType: String
    ): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(MediaWorker.KEY_MESSAGE_ID, messageId)
            .putString(MediaWorker.KEY_FILE_ID, fileId)
            .putString(MediaWorker.KEY_TRANSFER_TYPE, "DOWNLOAD")
            .putString(MediaWorker.KEY_MEDIA_TYPE, mediaType)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }

    fun enqueueUpload(
        context: Context,
        messageId: Long,
        localPath: String,
        mediaType: String
    ): UUID {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putLong(MediaWorker.KEY_MESSAGE_ID, messageId)
            .putString(MediaWorker.KEY_LOCAL_PATH, localPath)
            .putString(MediaWorker.KEY_TRANSFER_TYPE, "UPLOAD")
            .putString(MediaWorker.KEY_MEDIA_TYPE, mediaType)
            .build()

        val request = OneTimeWorkRequestBuilder<MediaWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(request)
        return request.id
    }
}
