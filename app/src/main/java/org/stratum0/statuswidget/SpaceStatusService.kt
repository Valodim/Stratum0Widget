package org.stratum0.statuswidget


import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent


class SpaceStatusService : IntentService("Space Status Service") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            ACTION_UPDATE -> {
                val openName = intent.getStringExtra(EXTRA_UPDATE_NAME)
                statusUpdate(openName)
            }
        }
    }

    private val stratum0StatusFetcher = Stratum0StatusFetcher()
    private val stratum0StatusUpdater = Stratum0StatusUpdater()

    private fun statusUpdate(name: String?) {
        stratum0StatusUpdater.update(name)

        val expectedStatus = if (name == null) SpaceStatus.CLOSED else SpaceStatus.OPEN
        var cachedSpaceStatus: SpaceStatusData? = null
        for (i in 1..5) {
            Thread.sleep(UPDATE_CHECK_INTERVAL_MS)

            cachedSpaceStatus = stratum0StatusFetcher.fetch()
            if (cachedSpaceStatus.status == expectedStatus && cachedSpaceStatus.openedBy.equals(name)) {
                break
            }
        }

        if (cachedSpaceStatus == null) {
            cachedSpaceStatus = SpaceStatusData.createUnknownStatus()
        }

        sendRefreshBroadcast(cachedSpaceStatus)
    }

    private fun sendRefreshBroadcast(statusData: SpaceStatusData) {
        val intent = Intent(EVENT_UPDATE_RESULT)
        intent.`package` = BuildConfig.APPLICATION_ID
        intent.putExtra(EXTRA_STATUS, statusData)
        sendBroadcast(intent)
    }

    companion object {
        val ACTION_UPDATE = "SpaceStatus.update"

        val EVENT_UPDATE_RESULT = "SpaceStatus.event.update_result"

        val EXTRA_STATUS = "status"
        val EXTRA_UPDATE_NAME = "updateName"

        val UPDATE_CHECK_INTERVAL_MS = 1000L

        fun triggerStatusUpdate(context: Context, appWidgetIds: IntArray, name: String?) {
            val intent = Intent(context, SpaceStatusService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_UPDATE
            intent.putExtra(EXTRA_UPDATE_NAME, name)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.startService(intent)
        }
    }

}

