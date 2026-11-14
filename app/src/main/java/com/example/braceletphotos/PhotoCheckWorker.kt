package com.example.braceletphotos

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.work.*
import java.time.Instant
import java.util.concurrent.TimeUnit

class PhotoCheckWorker(private val ctx: Context, params: WorkerParameters) :
    CoroutineWorker(ctx, params) {

    override suspend fun doWork(): Result {
        val folder = Prefs.get(ctx, Prefs.FOLDER_ID) ?: return Result.success()
        val account = ctx.getSharedPreferences("auth", 0).getString("account", null)
            ?: return Result.success()
        val since = Prefs.get(ctx, Prefs.LAST_CHECK) ?: Instant.now().toString()

        return try {
            val now = Instant.now().toString()
            val photos = DriveRepo(ctx, account).photosIn(folder, since)
            if (photos.isNotEmpty()) notify(photos.size, photos.first().webViewLink)
            Prefs.set(ctx, Prefs.LAST_CHECK, now)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    private fun notify(count: Int, link: String?) {
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("photos", "New photos", NotificationManager.IMPORTANCE_HIGH)
        )
        val pi = link?.let {
            PendingIntent.getActivity(
                ctx, 0, Intent(Intent.ACTION_VIEW, Uri.parse(it)),
                PendingIntent.FLAG_IMMUTABLE
            )
        }
        nm.notify(1, NotificationCompat.Builder(ctx, "photos")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("New bracelet photo")
            .setContentText("$count new photo(s) linked to your bracelet")
            .setContentIntent(pi).setAutoCancel(true).build())
    }

    companion object {
        fun schedule(ctx: Context) {
            val req = PeriodicWorkRequestBuilder<PhotoCheckWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(ctx).enqueueUniquePeriodicWork(
                "photo-check", ExistingPeriodicWorkPolicy.UPDATE, req)
        }
    }
}
