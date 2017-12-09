package org.stratum0.statuswidget.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.support.annotation.StringRes
import org.stratum0.statuswidget.*
import org.stratum0.statuswidget.interactors.Stratum0SshInteractor
import org.stratum0.statuswidget.interactors.SshKeyStorage
import org.stratum0.statuswidget.interactors.Stratum0WifiInteractor

class SpaceDoorService : IntentService("Space Door Service") {
    private lateinit var sshKeyStorage: SshKeyStorage
    private val s0SshInteractor = Stratum0SshInteractor()

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

        val error: Int? =
            if (!BuildConfig.DEBUG && !Stratum0WifiInteractor.isOnStratum0Wifi(applicationContext)) {
                R.string.unlock_error_wifi
            } else if (!sshKeyStorage.hasKey()) {
                R.string.unlock_error_no_key
            } else if (!sshKeyStorage.isKeyOk()) {
                R.string.unlock_error_privkey
            } else {
                val sshPrivateKey = sshKeyStorage.getKey()
                val sshPassword = sshKeyStorage.getPassword()
                try {
                    s0SshInteractor.open(sshPrivateKey, sshPassword)
                } catch (e: Exception) {
                    R.string.unlock_error_unknown
                }
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