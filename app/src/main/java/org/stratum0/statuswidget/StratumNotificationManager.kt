package org.stratum0.statuswidget

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

class StratumNotificationManager {

    internal fun handleStatusNotification(context: Context, statusData: SpaceStatusData, isOnS0Wifi: Boolean) {
        return // disabled for now

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (statusData.status != SpaceStatus.CLOSED) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        if (!isOnS0Wifi) {
            notificationManager.cancel(NOTIFICATION_ID)
            return
        }

        val notificationIntent = Intent(context, StratumsphereStatusProvider::class.java)
        val contentIntent = PendingIntent.getActivity(context, 0, notificationIntent, 0)


        val notification = Notification.Builder(context)
                .setSmallIcon(R.drawable.ic_stratum0_cutout)
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
    }

}