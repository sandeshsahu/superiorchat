package com.mobile.superiorutils.core

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import com.mobile.superiorutils.utils.Security

class KeyProvider : ContentProvider() {

    override fun onCreate(): Boolean {
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        // Return a cursor containing the public key
        val publicKey = Security.getPublicKeyBase64()
        val cursor = MatrixCursor(arrayOf("public_key"))
        cursor.addRow(arrayOf(publicKey))
        return cursor
    }

    override fun getType(uri: Uri): String? {
        return "vnd.android.cursor.item/vnd.com.mobile.superiorutils.key"
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
