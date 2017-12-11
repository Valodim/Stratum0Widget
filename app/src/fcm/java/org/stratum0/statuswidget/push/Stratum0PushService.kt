package org.stratum0.statuswidget.push

object Stratum0Push {
    fun subscribeToPush() {
        Stratum0FcmService.subscribeToStatusUpdates()
    }

    fun unsubscribeFromPush() {
        Stratum0FcmService.unsubscribeFromStatusUpdates()
    }
}