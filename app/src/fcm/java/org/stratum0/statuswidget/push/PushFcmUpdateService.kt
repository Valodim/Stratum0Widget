package org.stratum0.statuswidget.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.stratum0.statuswidget.interactors.Stratum0StatusFetcher
import org.stratum0.statuswidget.service.StratumsphereStatusProvider

class PushFcmUpdateService : FirebaseMessagingService() {
    private val stratum0StatusFetcher = Stratum0StatusFetcher()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val status = stratum0StatusFetcher.fetch()
        StratumsphereStatusProvider.sendRefreshBroadcast(applicationContext, status)
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