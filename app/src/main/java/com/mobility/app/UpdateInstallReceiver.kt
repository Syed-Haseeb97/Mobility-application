package com.mobility.app

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.database.Cursor

class UpdateInstallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val completedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val prefs = context.getSharedPreferences("mobility_updates", Context.MODE_PRIVATE)
        if (completedId == -1L || completedId != prefs.getLong("download_id", -2L)) return

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val query = DownloadManager.Query().setFilterById(completedId)
        val cursor: Cursor = manager.query(query)
        val successful = cursor.use {
            it.moveToFirst() && it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL
        }
        if (!successful) {
            prefs.edit().remove("download_id").apply()
            return
        }

        val apkUri: Uri = manager.getUriForDownloadedFile(completedId) ?: return
        val install = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(install)
            prefs.edit().remove("download_id").apply()
        } catch (_: Exception) {
            // Download remains in the system Downloads app if Android blocks auto-opening the installer.
        }
    }
}
