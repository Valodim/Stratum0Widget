package org.stratum0.statuswidget.service


import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.RemoteViews
import org.stratum0.statuswidget.BuildConfig
import org.stratum0.statuswidget.R
import org.stratum0.statuswidget.SpaceStatus
import org.stratum0.statuswidget.SpaceStatusData
import org.stratum0.statuswidget.interactors.Stratum0StatusFetcher
import org.stratum0.statuswidget.interactors.Stratum0WifiInteractor
import org.stratum0.statuswidget.push.Stratum0StatusUpdater
import org.stratum0.statuswidget.ui.StatusActivity


class StratumsphereStatusProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        Stratum0StatusUpdater.initializeBackgroundUpdates(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        Stratum0StatusUpdater.stopBackgroundUpdates(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.main)
        setOnClickListeners(context, appWidgetIds, views)

        val statusData = getCachedSpaceStatusData(appWidgetManager, appWidgetIds)
        setViewInfo(context, statusData, appWidgetIds, views)

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CLICK -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                onWidgetClick(context, appWidgetIds)
            }
            EVENT_REFRESH -> {
                val status = intent.getParcelableExtra<SpaceStatusData>(SpaceStatusService.EXTRA_STATUS)
                onSpaceStatusUpdated(context, status)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Stratum0StatusUpdater.initializeBackgroundUpdates(context)
            }
        }

        super.onReceive(context, intent)
    }

    private fun onWidgetClick(context: Context, appWidgetIds: IntArray) {
        val cachedSpaceStatusData = getCachedSpaceStatusData(context)
        if (!Stratum0StatusUpdater.hasPush() && cachedSpaceStatusData.status == SpaceStatus.UNKNOWN) {
            refreshStatusAsync(context, appWidgetIds)
        } else {
            startStatusActivity(context)
        }
    }

    private val stratum0StatusFetcher = Stratum0StatusFetcher()

    private fun refreshStatusAsync(context: Context, appWidgetIds: IntArray) {
        object : AsyncTask<Void, Void, SpaceStatusData>() {
            override fun onPreExecute() {
                showUpdatingMessage(context, appWidgetIds)
            }

            override fun doInBackground(vararg p0: Void?): SpaceStatusData {
                return stratum0StatusFetcher.fetch()
            }

            override fun onPostExecute(result: SpaceStatusData) {
                onSpaceStatusUpdated(context, result)
            }
        }.execute()
    }

    private fun showUpdatingMessage(context: Context, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.main)
        val updatingText = context.getText(R.string.updating)

        for (i in appWidgetIds.indices) {
            views.setTextViewText(R.id.lastUpdateTextView, updatingText)
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun sendWidgetUpdateIntent(context: Context, appWidgetIds: IntArray) {
        val updateIntent = Intent(context, StratumsphereStatusProvider::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        context.sendBroadcast(updateIntent)
    }

    private fun startStatusActivity(context: Context) {
        val activityIntent = Intent(context, StatusActivity::class.java)
        activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(activityIntent)
    }

    private fun onSpaceStatusUpdated(context: Context, statusData: SpaceStatusData) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, StratumsphereStatusProvider::class.java))

        setCachedSpaceStatusData(statusData, appWidgetIds, appWidgetManager)
        sendWidgetUpdateIntent(context, appWidgetIds)

        checkWifiAndHandleNotification(context)
    }

    private fun setCachedSpaceStatusData(
            statusData: SpaceStatusData, appWidgetIds: IntArray, appWidgetManager: AppWidgetManager) {
        val options = Bundle()
        options.putParcelable("status", statusData)
        val wrap = Bundle()
        wrap.putParcelable("data", options)
        for (appWidgetId in appWidgetIds) {
            appWidgetManager.updateAppWidgetOptions(appWidgetId, wrap)
        }
    }

    private fun getCachedSpaceStatusData(context: Context): SpaceStatusData {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val appWidgetIds = appWidgetManager.getAppWidgetIds(
                ComponentName(context, StratumsphereStatusProvider::class.java))
        return getCachedSpaceStatusData(appWidgetManager, appWidgetIds)
    }

    private fun getCachedSpaceStatusData(appWidgetManager: AppWidgetManager, appWidgetIds: IntArray): SpaceStatusData {
        val appWidgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetIds.first())
        val statusDataWrapper: Bundle? = appWidgetOptions.getParcelable("data")
        if (statusDataWrapper != null) {
            statusDataWrapper.classLoader = javaClass.classLoader
            return statusDataWrapper.getParcelable<SpaceStatusData>("status")
        }

        return SpaceStatusData.createUnknownStatus()
    }

    private fun setOnClickListeners(context: Context, appWidgetIds: IntArray, views: RemoteViews) {
        val intent = Intent(context, StratumsphereStatusProvider::class.java)
        intent.action = ACTION_CLICK
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            val clickIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.widget_root, clickIntent)
        }
    }

    private fun setViewInfo(context: Context, statusData: SpaceStatusData, appWidgetIds: IntArray, views: RemoteViews) {
        val lastUpdateText = when (statusData.status) {
            SpaceStatus.OPEN -> {
                val timeInMillis = statusData.since!!.timeInMillis
                val date = when {
                    DateUtils.isToday(timeInMillis) -> context.getString(R.string.time_today)
                    DateUtils.isToday(timeInMillis + DateUtils.DAY_IN_MILLIS) -> context.getString(R.string.time_yesterday)
                    else -> DateUtils.formatDateTime(context, timeInMillis,
                            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_ALL)
                }
                val time = DateUtils.formatDateTime(context, timeInMillis,
                        DateUtils.FORMAT_SHOW_TIME or DateUtils.FORMAT_ABBREV_ALL)
                context.getString(R.string.status_since, date, time)
            }
            SpaceStatus.CLOSED -> context.getString(R.string.status_closed_short)
            SpaceStatus.UNKNOWN -> context.getString(R.string.status_unknown_short)
        }
        val statusBackgroundColor = when (statusData.status) {
            SpaceStatus.OPEN -> R.color.status_open
            SpaceStatus.CLOSED -> R.color.status_closed
            SpaceStatus.UNKNOWN -> R.color.status_unknown
        }

        @Suppress("deprecation") // we don't want to involve support lib for this
        val color = context.resources.getColor(statusBackgroundColor)
        for (i in appWidgetIds.indices) {
            views.setInt(R.id.statusImageBackground, "setColorFilter", color)
            views.setTextViewText(R.id.lastUpdateTextView, lastUpdateText)
        }
    }

    private fun checkWifiAndHandleNotification(context: Context) {
        val isOnS0Wifi = Stratum0WifiInteractor.isOnStratum0Wifi(context)
        if (isOnS0Wifi) {
            val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
            preferences.edit().putBoolean("spottedS0Wifi", true).apply()
        }
    }

    companion object {
        val ACTION_CLICK = "click"

        val EVENT_REFRESH = "SpaceStatus.event.refresh"

        fun sendRefreshBroadcast(context: Context, statusData: SpaceStatusData) {
            val intent = Intent(EVENT_REFRESH)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.putExtra(SpaceStatusService.EXTRA_STATUS, statusData)
            context.sendBroadcast(intent)
        }
    }
}
