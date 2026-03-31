package com.example.uptmqueue

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_QUEUE = "queue_channel"
        const val CHANNEL_MESSAGES = "messages_channel"
        const val CHANNEL_ANNOUNCEMENTS = "announcements_channel"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"] ?: "queue"
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // ✅ Check mute status
        val isEnabled = when (type) {
            "chat" -> sharedPref.getBoolean("notif_messages", true)
            "announcement" -> sharedPref.getBoolean("notif_announcements", true)
            else -> sharedPref.getBoolean("notif_queue", true)
        }

        if (!isEnabled) return  // Muted, keluar terus

        // ✅ Ambil dari data payload
        val title = remoteMessage.data["title"] ?: "UPTM Queue"
        val body = remoteMessage.data["body"] ?: ""

        val channelId = when (type) {
            "chat" -> CHANNEL_MESSAGES
            "announcement" -> CHANNEL_ANNOUNCEMENTS
            else -> CHANNEL_QUEUE
        }

        showNotification(title, body, channelId)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToDatabase(token)
    }

    private fun saveTokenToDatabase(token: String) {
        val studentId = getSharedPreferences("UserPrefs", MODE_PRIVATE)
            .getString("studentId", null) ?: return

        com.google.firebase.database.FirebaseDatabase.getInstance().reference
            .child("students").child(studentId).child("fcmToken")
            .setValue(token)
    }

    private fun showNotification(title: String, body: String, channelId: String) {
        createNotificationChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // 🔔 Queue
            NotificationChannel(
                CHANNEL_QUEUE,
                "Queue Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications when your queue number is called"
                enableVibration(true)
                manager.createNotificationChannel(this)
            }

            // 💬 Messages
            NotificationChannel(
                CHANNEL_MESSAGES,
                "Chat Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages from staff"
                enableVibration(true)
                manager.createNotificationChannel(this)
            }

            // 📢 Announcements
            NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "Announcements",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Important announcements from UPTM Bursary"
                enableVibration(true)
                manager.createNotificationChannel(this)
            }
        }
    }
}
