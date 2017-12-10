package org.stratum0.statuswidget.service


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Handler
import android.os.Message
import android.widget.RemoteViews
import org.stratum0.statuswidget.*
import org.stratum0.statuswidget.interactors.Stratum0StatusFetcher
import org.stratum0.statuswidget.interactors.Stratum0WifiInteractor
import org.stratum0.statuswidget.ui.FirstRunActivity
import org.stratum0.statuswidget.ui.StatusActivity
import java.util.*


class StratumsphereStatusProvider : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        SpaceStatusJobService.jobScheduleRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        SpaceStatusJobService.jobCancelRefresh(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.main)
        setOnClickListeners(context, appWidgetIds, views)
        appWidgetManager.updateAppWidget(appWidgetIds, views)

        onSpaceStatusUpdateInProgress(context, appWidgetIds)
        asyncRefreshSpaceStatus(context, appWidgetIds)
    }

    private val stratum0StatusFetcher = Stratum0StatusFetcher()

    private fun asyncRefreshSpaceStatus(context: Context, appWidgetIds: IntArray) {
        object : AsyncTask<Void, Void, SpaceStatusData>() {
            override fun onPreExecute() {
                onSpaceStatusUpdateInProgress(context, appWidgetIds)
            }

            override fun doInBackground(vararg p0: Void?): SpaceStatusData {
                return stratum0StatusFetcher.fetch()
            }

            override fun onPostExecute(result: SpaceStatusData) {
                onSpaceStatusUpdated(context, appWidgetIds, result)
            }
        }.execute()
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_CLICK -> {
                val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
                onWidgetClick(context, appWidgetIds)
            }
            SpaceStatusJobService.EVENT_REFRESH -> {
                val status = intent.getParcelableExtra<SpaceStatusData>(SpaceStatusService.EXTRA_STATUS)

                val appWidgetManager = AppWidgetManager.getInstance(context)
                val appWidgetIds = appWidgetManager.getAppWidgetIds(
                        ComponentName(context, StratumsphereStatusProvider::class.java))

                onSpaceStatusUpdated(context, appWidgetIds, status)
            }
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                SpaceStatusJobService.jobScheduleRefresh(context)
            }
        }

        super.onReceive(context, intent)
    }

    @SuppressLint("ApplySharedPref")
    private fun onWidgetClick(context: Context, appWidgetIds: IntArray) {
        val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

        if (preferences.getBoolean("firstrun", true)) {
            val firstrunIntent = Intent(context, FirstRunActivity::class.java)
            firstrunIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(firstrunIntent)
            preferences.edit().putBoolean("firstrun", false).apply()
            return
        }

        val clickCount = preferences.getInt("clicks", 0)
        preferences.edit().putInt("clicks", clickCount + 1).commit()

        if (clickCount > 0) {
            startStatusActivity(context)
        } else {
            object : Handler() {
                override fun handleMessage(msg: Message) {
                    val delayedClickCount = preferences.getInt("clicks", 0)
                    preferences.edit().putInt("clicks", 0).commit()
                    if (delayedClickCount == 1) {
                        sendWidgetUpdateIntent(context, appWidgetIds)
                    }
                }
            }.sendEmptyMessageDelayed(0, 180)
        }
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

    private fun onSpaceStatusUpdateInProgress(context: Context, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.main)
        val updatingText = context.getText(R.string.updating)

        for (i in appWidgetIds.indices) {
            views.setTextViewText(R.id.lastUpdateTextView, updatingText)
        }

        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    private fun onSpaceStatusUpdated(context: Context, appWidgetIds: IntArray, statusData: SpaceStatusData) {
        val views = RemoteViews(context.packageName, R.layout.main)
        setOnClickListeners(context, appWidgetIds, views)
        setViewInfo(context, statusData, appWidgetIds, views)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        appWidgetManager.updateAppWidget(appWidgetIds, views)

        checkWifiAndHandleNotification(context)
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
        val upTimeText = getUptimeText(statusData)
        val lastUpdateText = String.format("%s: %02d:%02d", context.getText(R.string.currentTime),
                statusData.lastUpdate.get(Calendar.HOUR_OF_DAY), statusData.lastUpdate.get(Calendar.MINUTE))
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
            views.setTextViewText(R.id.spaceUptimeTextView, upTimeText)
        }
    }

    private fun checkWifiAndHandleNotification(context: Context) {
        val isOnS0Wifi = Stratum0WifiInteractor.isOnStratum0Wifi(context)
        if (isOnS0Wifi) {
            val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
            preferences.edit().putBoolean("spottedS0Wifi", true).apply()
        }
    }

    private fun getUptimeText(statusData: SpaceStatusData): String {
        val uptimeSeconds = statusData.uptimeSeconds
        if (uptimeSeconds == null) {
            return ""
        }

        var uptimeMinutes = uptimeSeconds / 60 % 60
        var uptimeHours = uptimeSeconds / 60 / 60
        if (uptimeHours > 99) {
            uptimeMinutes = 99
            uptimeHours = 99
        }

        return String.format("%02d      %02d", uptimeHours, uptimeMinutes)
    }

    companion object {
        val ACTION_CLICK = "click"
    }
}
