package org.stratum0.statuswidget


import android.annotation.SuppressLint
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Message
import android.widget.RemoteViews
import java.util.*


class StratumsphereStatusProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        SpaceStatusService.triggerStatusRefresh(context, appWidgetIds, true)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val appWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS)
        if (appWidgetIds == null) {
            super.onReceive(context, intent)
            return
        }

        when (intent.action) {
            ACTION_CLICK -> {
                onWidgetClick(context, appWidgetIds)
            }
            SpaceStatusService.EVENT_REFRESH_IN_PROGRESS -> onSpaceStatusUpdateInProgress(context, appWidgetIds)
            SpaceStatusService.EVENT_REFRESH -> {
                val status = intent.getParcelableExtra<SpaceStatusData>(SpaceStatusService.EXTRA_STATUS)
                onSpaceStatusUpdated(context, appWidgetIds, status)
            }
        }

        super.onReceive(context, intent)
    }

    private fun setOnClickListeners(context: Context, appWidgetIds: IntArray, views: RemoteViews) {
        val appWidgetManager = AppWidgetManager.getInstance(context)

        val intent = Intent(context, StratumsphereStatusProvider::class.java)
        intent.action = ACTION_CLICK
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)

        for (appWidgetId in appWidgetIds) {
            val clickIntent = PendingIntent.getBroadcast(context, appWidgetId, intent, PendingIntent.FLAG_UPDATE_CURRENT)
            views.setOnClickPendingIntent(R.id.statusImageView, clickIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views)
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

        if (clickCount == 0) {
            preferences.edit().putInt("clicks", clickCount + 1).commit()
            sendWidgetUpdateIntent(context, appWidgetIds)

            object : Handler() {
                override fun handleMessage(msg: Message) {
                    preferences.edit().putInt("clicks", 0).commit()
                }
            }.sendEmptyMessageDelayed(0, 500)
        } else {
            val activityIntent = Intent(context, StatusActivity::class.java)
            activityIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            activityIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
            context.startActivity(activityIntent)
            preferences.edit().putInt("clicks", 0).commit()
            return
        }
    }

    private fun sendWidgetUpdateIntent(context: Context, appWidgetIds: IntArray) {
        val updateIntent = Intent(context, StratumsphereStatusProvider::class.java)
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, appWidgetIds)
        context.sendBroadcast(updateIntent)
    }

    private fun onSpaceStatusUpdateInProgress(context: Context, appWidgetIds: IntArray) {
        val views = RemoteViews(context.packageName, R.layout.main)
        val updatingText = context.getText(R.string.updating)

        // indicate that the status is currently updating
        for (i in appWidgetIds.indices) {
            val appWidgetId = appWidgetIds[i]

            views.setTextViewText(R.id.lastUpdateTextView, updatingText)
            val appWidgetManager = AppWidgetManager.getInstance(context)
            appWidgetManager!!.updateAppWidget(appWidgetId, views)
        }

    }

    private fun onSpaceStatusUpdated(context: Context, appWidgetIds: IntArray, statusData: SpaceStatusData) {
        val isOnS0Wifi = Stratum0WifiManager.isOnStratum0Wifi(context)
        if (isOnS0Wifi) {
            val preferences = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)
            preferences.edit().putBoolean("spottedS0Wifi", true).apply()
        }

        notificationManager.handleStatusNotification(context, statusData, isOnS0Wifi)

        val views = RemoteViews(context.packageName, R.layout.main)

        val upTimeText = getUptimeText(statusData)

        val lastUpdateText = String.format("%s:\n%02d:%02d", context.getText(R.string.currentTime), statusData.lastUpdate.get(Calendar.HOUR_OF_DAY), statusData.lastUpdate.get(Calendar.MINUTE))

        val currentImage = when (statusData.status) {
            SpaceStatus.OPEN -> R.drawable.stratum0_open
            SpaceStatus.UNKNOWN -> R.drawable.stratum0_unknown
            SpaceStatus.CLOSED -> R.drawable.stratum0_closed
        }

        for (i in appWidgetIds.indices) {
            views.setImageViewResource(R.id.statusImageView, currentImage)
            views.setTextViewText(R.id.lastUpdateTextView, lastUpdateText)
            views.setTextViewText(R.id.spaceUptimeTextView, upTimeText)
        }

        setOnClickListeners(context, appWidgetIds, views)

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

        return String.format("%02d     %02d", uptimeHours, uptimeMinutes)
    }

    companion object {
        val notificationManager = StratumNotificationManager()

        val ACTION_CLICK = "click"
    }
}
