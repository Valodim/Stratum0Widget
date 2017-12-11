package org.stratum0.statuswidget.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import org.stratum0.statuswidget.service.SpaceStatusJobService

class Stratum0FcmService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        SpaceStatusJobService.jobRefreshNow(applicationContext)
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