package com.optionpulse.scanner

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class OptionPulseMessagingService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        val title = message.notification?.title ?: message.data["title"] ?: "OptionPulse Alert"
        val body = message.notification?.body ?: message.data["body"] ?: "New NSE F&O setup"
        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        getSharedPreferences("optionpulse", MODE_PRIVATE).edit().putString("fcm_token", token).apply()
        DeviceRegistrar.register(token)
    }

    private fun showNotification(title: String, body: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Trading alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "High-priority OptionPulse trading setup alerts"
            enableVibration(true)
        })
        val intent = Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title).setContentText(body).setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH).setAutoCancel(true).setContentIntent(pending).build()
        manager.notify((System.currentTimeMillis() % Int.MAX_VALUE).toInt(), notification)
    }

    companion object { const val CHANNEL_ID = "optionpulse_alerts" }
}

object DeviceRegistrar {
    fun register(token: String) {
        if (BuildConfig.DEVICE_ENROLLMENT_TOKEN.isBlank()) return
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching { ApiFactory.api.registerDevice(DeviceToken(token)) }
        }
    }
}
