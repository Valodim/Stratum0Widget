package org.stratum0.statuswidget

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.support.v4.app.NotificationCompat

class StratumNotificationManager {

    internal fun handleStatusNotification(context: Context, statusData: SpaceStatusData) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (statusData.status != SpaceStatus.CLOSED) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        if (!isOnStratum0Wifi(context)) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val notificationIntent = Intent(context, StratumsphereStatusProvider::class.java)
        val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)

        val notification = NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.stratum0_unknown)
                .setContentTitle(context.getText(R.string.notification_title))
                .setContentText(context.getText(R.string.notification_content))
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL)
                .setContentIntent(contentIntent)
                .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        private val NOTIFICATION_ID = 1

        private fun isOnStratum0Wifi(context: Context): Boolean {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo

            return wifiInfo.ssid != null && (wifiInfo.ssid == "Stratum0" || wifiInfo.ssid == "Stratum0_5g")
        }
    }

}