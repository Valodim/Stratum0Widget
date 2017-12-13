@file:Suppress("UNUSED_PARAMETER") // must be compatible with file in other flavors

package org.stratum0.statuswidget.push

import android.content.Context

object Stratum0StatusUpdater {
    fun initializeBackgroundUpdates(context: Context) {
        PushFcmUpdateService.subscribeToStatusUpdates()
    }

    fun stopBackgroundUpdates(context: Context) {
        PushFcmUpdateService.unsubscribeFromStatusUpdates()
    }

    fun hasPush(): Boolean {
        return true
    }
}