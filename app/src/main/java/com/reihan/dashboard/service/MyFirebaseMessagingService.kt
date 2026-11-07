package com.reihan.dashboard.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.reihan.dashboard.DashboardApp
import com.reihan.dashboard.MainActivity
import com.reihan.dashboard.data.model.UserDeviceRequest
import com.reihan.dashboard.data.network.RetrofitClient
import com.reihan.dashboard.data.network.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FMC", "New token received: $token")
        if (SessionManager.hasToken()) {
            sendTokenToBackend(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Log.d("FMC", "From: ${remoteMessage.from}")

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d("FMC", "Message Notification Body: ${it.body}")
            showNotification(it.title ?: "Reminder", it.body ?: "")
        }

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FMC", "Message data payload: ${remoteMessage.data}")
            val title = remoteMessage.data["title"] ?: "Reminder"
            val body = remoteMessage.data["body"] ?: ""
            if (remoteMessage.notification == null) {
                showNotification(title, body)
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, DashboardApp.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(com.reihan.dashboard.R.mipmap.ic_launcher) // Using launcher icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // ID 0 is a default ID, you could use a unique ID for multiple notifications
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun sendTokenToBackend(token: String) {
        val hasJwt = SessionManager.hasToken()
        Log.d("FMC_SERVICE", "JWT Token available: $hasJwt")
        
        if (!hasJwt) {
            Log.d("FMC_SERVICE", "Token update skipped: User not authenticated")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("FMC_SERVICE", "Sending updated FMC token to backend...")
                val response = RetrofitClient.apiService.saveDevice(
                    UserDeviceRequest(fmcToken = token, deviceType = "android")
                )
                
                if (response.isSuccessful) {
                    Log.d("FMC_SERVICE", "Token sync successful: ${response.code()}")
                } else {
                    Log.e("FMC_SERVICE", "Token sync failed with code: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("FMC_SERVICE", "Exception during background token sync: ${e.localizedMessage}")
            }
        }
    }
}
