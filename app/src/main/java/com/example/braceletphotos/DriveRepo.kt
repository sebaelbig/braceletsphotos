package com.example.braceletphotos

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DriveRepo(ctx: Context, accountName: String) {
    private val credential = GoogleAccountCredential
        .usingOAuth2(ctx, listOf(DriveScopes.DRIVE_READONLY))
        .apply { selectedAccountName = accountName }

    private val drive = Drive.Builder(
        AndroidHttp.newCompatibleTransport(), GsonFactory.getDefaultInstance(), credential
    ).setApplicationName("BraceletPhotos").build()

    suspend fun listFolders(): List<File> = withContext(Dispatchers.IO) {
        drive.files().list()
            .setQ("mimeType='application/vnd.google-apps.folder' and trashed=false")
            .setFields("files(id,name)").execute().files ?: emptyList()
    }

    suspend fun photosIn(folderId: String, since: String? = null): List<File> =
        withContext(Dispatchers.IO) {
            var q = "'$folderId' in parents and mimeType contains 'image/' and trashed=false"
            if (since != null) q += " and createdTime > '$since'"
            drive.files().list()
                .setQ(q).setOrderBy("createdTime desc")
                .setFields("files(id,name,createdTime,thumbnailLink,webViewLink)")
                .execute().files ?: emptyList()
        }
}
