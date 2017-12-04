package org.stratum0.statuswidget


import android.app.IntentService
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock


class SpaceStatusService : IntentService("Space Status Service") {
    var cachedSpaceStatus: SpaceStatusData? = null

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)

        when (intent.action) {
            ACTION_REFRESH -> statusRefresh(appWidgetIds, intent.getBooleanExtra(EXTRA_SKIP_CACHE, false))
            ACTION_UPDATE -> {
                val openName = intent.getStringExtra(EXTRA_UPDATE_NAME)
                statusUpdate(appWidgetIds, openName)
            }
        }
    }

    private val stratum0StatusFetcher = Stratum0StatusFetcher()
    private val stratum0StatusUpdater = Stratum0StatusUpdater()

    private fun statusRefresh(appWidgetIds: IntArray, skipCache: Boolean) {
        if (skipCache || cachedSpaceStatus == null || cachedSpaceStatus!!.isOlderThan(60*1000)) {
            val startRealtime = SystemClock.elapsedRealtime()
            sendRefreshInProgressBroadcast(appWidgetIds)
            cachedSpaceStatus = stratum0StatusFetcher.fetch()

            val elapsedRealtime = SystemClock.elapsedRealtime() - startRealtime
            if (elapsedRealtime < MIN_OPERATION_MS) {
                Thread.sleep(MIN_OPERATION_MS - elapsedRealtime)
            }
        }

        sendRefreshBroadcast(appWidgetIds, cachedSpaceStatus!!)
    }

    private fun statusUpdate(appWidgetIds: IntArray, name: String?) {
        stratum0StatusUpdater.update(name)

        val status = stratum0StatusFetcher.fetch()
        sendRefreshBroadcast(appWidgetIds, status)
    }

    private fun sendRefreshBroadcast(appWidgetIds: IntArray, statusData: SpaceStatusData) {
        val intent = Intent(EVENT_REFRESH)
        intent.`package` = BuildConfig.APPLICATION_ID
        intent.putExtra(EXTRA_STATUS, statusData)
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        sendBroadcast(intent)
    }

    private fun sendRefreshInProgressBroadcast(appWidgetIds: IntArray) {
        val intent = Intent(EVENT_REFRESH_IN_PROGRESS)
        intent.`package` = BuildConfig.APPLICATION_ID
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        sendBroadcast(intent)
    }

    companion object {
        val ACTION_REFRESH = "SpaceStatus.refresh"
        val ACTION_UPDATE = "SpaceStatus.update"

        val EVENT_REFRESH = "SpaceStatus.event.refresh"
        val EVENT_REFRESH_IN_PROGRESS = "SpaceStatus.event.refreshInProgress"

        val EXTRA_STATUS = "status"
        val EXTRA_UPDATE_NAME = "updateName"
        val EXTRA_SKIP_CACHE = "skipCache"

        val MIN_OPERATION_MS = 500

        fun triggerStatusRefresh(context: Context, appWidgetIds: IntArray, skipCache: Boolean) {
            val intent = Intent(context, SpaceStatusService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_REFRESH
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            intent.putExtra(EXTRA_SKIP_CACHE, skipCache)
            context.startService(intent)
        }

        fun triggerStatusUpdate(context: Context, appWidgetIds: IntArray, name: String?) {
            val intent = Intent(context, SpaceStatusService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_REFRESH
            intent.putExtra(EXTRA_UPDATE_NAME, name)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.startService(intent)
        }
    }

}

