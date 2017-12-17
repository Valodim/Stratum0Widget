package horse.amazin.my.stratum0.statuswidget.service


import android.app.IntentService
import android.content.Context
import android.content.Intent
import horse.amazin.my.stratum0.statuswidget.*
import horse.amazin.my.stratum0.statuswidget.interactors.StatusFetcher
import horse.amazin.my.stratum0.statuswidget.interactors.StatusUpdater


class StatusChangerService : IntentService("Space Status Service") {
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

    private val stratum0StatusFetcher = StatusFetcher()
    private val stratum0StatusUpdater = StatusUpdater()

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
            cachedSpaceStatus = SpaceStatusData.createErrorStatus()
        }

        sendUpdateResultBroadcast(cachedSpaceStatus)
    }

    private fun sendUpdateResultBroadcast(statusData: SpaceStatusData) {
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

        fun triggerStatusUpdate(context: Context, name: String?) {
            val intent = Intent(context, StatusChangerService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_UPDATE
            intent.putExtra(EXTRA_UPDATE_NAME, name)
            context.startService(intent)
        }
    }

}

