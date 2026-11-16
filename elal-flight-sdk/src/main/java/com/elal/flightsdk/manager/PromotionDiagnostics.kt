package com.elal.flightsdk.manager

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import com.elal.flightsdk.model.PromotionStatus
import com.elal.flightsdk.presentation.PromotionStatusResolver

/** Reads whether the system will promote the Live Update, and opens the setting that controls it. */
internal object PromotionDiagnostics {

    /** The current [PromotionStatus] from the notification and promotion permissions. */
    fun read(context: Context): PromotionStatus {
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
        val canPostPromoted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            context.getSystemService(NotificationManager::class.java).canPostPromotedNotifications()
        } else {
            false
        }
        return PromotionStatusResolver.resolve(Build.VERSION.SDK_INT, notificationsEnabled, canPostPromoted)
    }

    /**
     * The app's Live Updates setting on Android 16+ (`ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS`, F9), else the
     * app's notification settings. On One UI 9 the former opens the general app page (no per-app toggle).
     */
    fun settingsIntent(context: Context): Intent {
        val action = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS
        } else {
            Settings.ACTION_APP_NOTIFICATION_SETTINGS
        }
        return Intent(action)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
