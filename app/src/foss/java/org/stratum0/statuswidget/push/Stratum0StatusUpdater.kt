package org.stratum0.statuswidget.push

import android.content.Context

object Stratum0StatusUpdater {
    fun initializeBackgroundUpdates(context: Context) {
        PeriodicUpdateJobService.jobScheduleRefresh(context)
    }

    fun stopBackgroundUpdates(context: Context) {
        PeriodicUpdateJobService.jobCancelRefresh(context)
    }

    fun hasPush(): Boolean {
        return false
    }
}