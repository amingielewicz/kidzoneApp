package com.kidzone.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kidzone.MainActivity
import com.kidzone.R

class KidZoneMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        saveTokenToFirestore(token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val notification = message.notification
        if (notification != null) {
            showNotification(
                title = notification.title ?: "kidZone",
                body = notification.body ?: "",
                data = message.data
            )
        } else if (message.data.isNotEmpty()) {
            showNotification(
                title = message.data["title"] ?: "kidZone",
                body = message.data["body"] ?: "",
                data = message.data
            )
        }
    }

    private fun showNotification(title: String, body: String, data: Map<String, String>) {
        val channelId = CHANNEL_GENERAL
        ensureNotificationChannel(channelId)

        // Deep link URI na podstawie typu powiadomienia — klik w notyfikację
        // otwiera odpowiedni ekran (Navigation Compose obsługuje te URI).
        val deepLinkUri = buildDeepLinkUri(data)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (deepLinkUri != null) {
                this.data = android.net.Uri.parse(deepLinkUri)
                putExtra("deepLink", deepLinkUri)
            }
            data.forEach { (key, value) -> putExtra(key, value) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun ensureNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (manager.getNotificationChannel(channelId) == null) {
                val channel = NotificationChannel(
                    channelId,
                    "Powiadomienia kidZone",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Opinie, nowe miejsca i inne aktywności"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .set(
                mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                SetOptions.merge()
            )
    }

    /**
     * Buduje deep link URI na podstawie pola `type` w data payload FCM.
     */
    private fun buildDeepLinkUri(data: Map<String, String>): String? {
        return when (data["type"]) {
            "new_review", "new_photo" -> {
                val placeId = data["placeId"] ?: return null
                "kidzone://place/$placeId"
            }
            "place_top_rank" -> "kidzone://ranking/places"
            "user_top_rank" -> "kidzone://ranking/users"
            "new_badge" -> "kidzone://profile"
            else -> null
        }
    }

    companion object {
        const val CHANNEL_GENERAL = "kidzone_general"

        fun registerCurrentToken(context: Context) {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .set(
                            mapOf("fcmTokens" to FieldValue.arrayUnion(token)),
                            SetOptions.merge()
                        )
                }
        }

        fun unregisterToken() {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@addOnSuccessListener
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(uid)
                        .update("fcmTokens", FieldValue.arrayRemove(token))
                }
        }
    }
}
