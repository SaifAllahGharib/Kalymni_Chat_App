package com.kalymni.notification

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kalymni.R

class FCMNotificationService : FirebaseMessagingService() {

    @SuppressLint("NewApi", "MissingPermission", "WrongConstant")
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification!!.title
        val text = message.notification!!.body
        val channelId = "MESSAGE"
        val channel = NotificationChannel(
            channelId,
            "Message Notification",
            NotificationManager.IMPORTANCE_HIGH
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification =
            Notification.Builder(this, channelId).setContentTitle(title).setContentText(text)
                .setSmallIcon(R.mipmap.ic_launcher_round).setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)

        NotificationManagerCompat.from(this)
            .notify(java.util.Random().nextInt(85 - 65), notification.build())
    }

}