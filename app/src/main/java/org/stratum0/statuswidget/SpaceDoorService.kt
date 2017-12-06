package org.stratum0.statuswidget

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.support.annotation.StringRes

class SpaceDoorService : IntentService("Space Door Service") {
    private lateinit var sshKeyStorage: SshKeyStorage

    override fun onCreate() {
        super.onCreate()

        sshKeyStorage = SshKeyStorage(applicationContext)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            ACTION_UNLOCK -> doorUnlock()
        }
    }

    private fun doorUnlock() {
        val startRealtime = SystemClock.elapsedRealtime()

        var error: Int? = null
        if (false && !Stratum0WifiManager.isOnStratum0Wifi(applicationContext)) {
            error = R.string.unlock_error_wifi
        }

        val elapsedRealtime = SystemClock.elapsedRealtime() - startRealtime
        if (elapsedRealtime < MIN_UNLOCK_MS) {
            Thread.sleep(MIN_UNLOCK_MS - elapsedRealtime)
        }

        if (error != null) {
            sendUnlockStatusBroadcastError(error)
        } else {
            sendUnlockStatusBroadcastOk()
        }
    }

    private fun sendUnlockStatusBroadcastError(@StringRes errResId: Int) {
        sendUnlockStatusBroadcast(false, errResId)
    }

    private fun sendUnlockStatusBroadcastOk() {
        sendUnlockStatusBroadcast(true, null)
    }

    private fun sendUnlockStatusBroadcast(ok: Boolean, @StringRes errResId: Int?) {
        val intent = Intent(EVENT_UNLOCK_STATUS)
        intent.`package` = BuildConfig.APPLICATION_ID
        intent.putExtra(EXTRA_STATUS, ok)
        if (!ok) {
            intent.putExtra(EXTRA_ERROR_RES, errResId)
        }
        sendBroadcast(intent)
    }

    companion object {
        val ACTION_UNLOCK = "SpaceDoor.unlock"

        val EVENT_UNLOCK_STATUS = "SpaceDoor.event.unlock_status"

        val EXTRA_STATUS = "status"
        val EXTRA_ERROR_RES = "error"

        val MIN_UNLOCK_MS = 500L

        fun triggerDoorUnlock(context: Context) {
            val intent = Intent(context, SpaceDoorService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_UNLOCK
            context.startService(intent)
        }
    }

}