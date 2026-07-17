package com.mobile.superiorsetup.core

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object AppManager {
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun installApp(context: Context, launchIntent: (Intent) -> Unit) {
        try {
            val assetManager = context.assets
            val inputStream = assetManager.open("app.apk")
            val outFile = File(context.cacheDir, "superior_chat.apk")
            val outputStream = FileOutputStream(outFile)
            
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.flush()
            outputStream.close()
            
            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                outFile
            )
            
            val intent = Intent(Intent.ACTION_INSTALL_PACKAGE)
            intent.data = apkUri
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
            intent.putExtra(Intent.EXTRA_RETURN_RESULT, true)
            
            launchIntent(intent)
            
        } catch (e: Exception) {
            Toast.makeText(context, "Error preparing installation: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    fun wakeUpMainApp(context: Context, botToken: String, chatId: String) {
        try {
            val uri = android.net.Uri.parse("content://${com.mobile.superiorsetup.BuildConfig.TARGET_APP_ID}.keys")
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            var publicKeyBase64 = ""
            cursor?.use {
                if (it.moveToFirst()) {
                    publicKeyBase64 = it.getString(0)
                }
            }
            
            if (publicKeyBase64.isEmpty()) {
                Toast.makeText(context, "Error: Could not retrieve secure key from main app. Is it installed?", Toast.LENGTH_LONG).show()
                return
            }
            
            val encryptedToken = Security.encryptRSA(botToken, publicKeyBase64)
            val encryptedChatId = Security.encryptRSA(chatId, publicKeyBase64)

            val intent = Intent()
            intent.component = android.content.ComponentName(com.mobile.superiorsetup.BuildConfig.TARGET_APP_ID, "com.mobile.superiorchat.MainActivity")
            intent.putExtra("SETUP_BOT_TOKEN", encryptedToken)
            intent.putExtra("SETUP_CHAT_ID", encryptedChatId)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            Toast.makeText(context, "Main app awakened and configured!", Toast.LENGTH_SHORT).show()
            
            (context as? android.app.Activity)?.finish()
        } catch (e: Exception) {
            Toast.makeText(context, "Error launching main app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
