package com.bidh.therapytracker.sync

import android.accounts.Account
import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile

// Thin wrapper around the Google Drive REST API: sign-in credential, the
// single "Appointment Tracker" folder in the user's regular My Drive (not the
// hidden app-data area, since the whole point is that the user can open these
// files from a computer), and create-or-update for one text file per category.
object DriveServiceHelper {

    const val APP_FOLDER_NAME = "Appointment Tracker"
    private const val MIME_FOLDER = "application/vnd.google-apps.folder"
    private const val MIME_TEXT = "text/plain"

    fun buildService(context: Context, account: Account): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context.applicationContext, listOf(DriveScopes.DRIVE_FILE)
        )
        credential.selectedAccount = account
        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName(APP_FOLDER_NAME)
            .build()
    }

    fun getOrCreateAppFolder(drive: Drive, cachedFolderId: String?): String {
        if (cachedFolderId != null) {
            try {
                val existing = drive.files().get(cachedFolderId).setFields("id,trashed").execute()
                if (existing.trashed != true) return cachedFolderId
            } catch (e: Exception) {
                // Cached id no longer valid (deleted, permissions changed, etc.) - fall through.
            }
        }

        val query = "mimeType='$MIME_FOLDER' and name='$APP_FOLDER_NAME' and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id,name)")
            .execute()
        result.files?.firstOrNull()?.let { return it.id }

        val folderMetadata = DriveFile().apply {
            name = APP_FOLDER_NAME
            mimeType = MIME_FOLDER
        }
        val created = drive.files().create(folderMetadata).setFields("id").execute()
        return created.id
    }

    fun uploadOrUpdateTextFile(drive: Drive, folderId: String, fileName: String, content: String) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        val mediaContent = ByteArrayContent(MIME_TEXT, bytes)

        val escapedName = fileName.replace("'", "\\'")
        val query = "name='$escapedName' and '$folderId' in parents and trashed=false"
        val result = drive.files().list()
            .setQ(query)
            .setSpaces("drive")
            .setFields("files(id,name)")
            .execute()
        val existing = result.files?.firstOrNull()

        if (existing != null) {
            drive.files().update(existing.id, null, mediaContent).execute()
        } else {
            val metadata = DriveFile().apply {
                name = fileName
                parents = listOf(folderId)
            }
            drive.files().create(metadata, mediaContent).setFields("id").execute()
        }
    }
}
