package org.stratum0.statuswidget.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.stratum0.statuswidget.interactors.StatusFetcher
import org.stratum0.statuswidget.service.Stratum0WidgetProvider

class PushFcmUpdateService : FirebaseMessagingService() {
    private val stratum0StatusFetcher = StatusFetcher()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val status = stratum0StatusFetcher.fetch()
        Stratum0WidgetProvider.sendRefreshBroadcast(applicationContext, status)
    }

    companion object {
        fun subscribeToStatusUpdates() {
            FirebaseMessaging.getInstance().subscribeToTopic("space-status")
        }

        fun unsubscribeFromStatusUpdates() {
            FirebaseMessaging.getInstance().unsubscribeFromTopic("space-status")
        }
    }
}