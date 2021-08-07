package horse.amazin.my.stratum0.statuswidget.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.annotation.StringRes
import horse.amazin.my.stratum0.statuswidget.BuildConfig
import horse.amazin.my.stratum0.statuswidget.R
import horse.amazin.my.stratum0.statuswidget.interactors.SshInteractor
import horse.amazin.my.stratum0.statuswidget.interactors.SshKeyStorage
import timber.log.Timber

class DoorUnlockService : IntentService("Space Door Service") {
    private lateinit var sshKeyStorage: SshKeyStorage
    private val s0SshInteractor = SshInteractor()

    override fun onCreate() {
        super.onCreate()

        sshKeyStorage = SshKeyStorage(applicationContext)
    }

    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) {
            return
        }

        when (intent.action) {
            ACTION_LOCK -> sshLoginOperation("zu")
            ACTION_UNLOCK -> sshLoginOperation("auf")
        }
    }

    private fun sshLoginOperation(sshUser: String) {
        val startRealtime = SystemClock.elapsedRealtime()

        val error: Int? =
            if (!sshKeyStorage.hasKey()) {
                R.string.ssh_error_no_key
            } else if (!sshKeyStorage.isKeyOk()) {
                R.string.ssh_error_privkey
            } else {
                val sshPrivateKey = sshKeyStorage.getKey()
                val sshPassword = sshKeyStorage.getPassword()
                try {
                    s0SshInteractor.performSshLogin(sshPrivateKey, sshPassword, sshUser)
                } catch (e: Exception) {
                    Timber.e(e, "Unknown error during connection attempt!")
                    R.string.ssh_error_unknown
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
        const val ACTION_UNLOCK = "SpaceDoor.unlock"
        const val ACTION_LOCK = "SpaceDoor.lock"

        const val EVENT_UNLOCK_STATUS = "SpaceDoor.event.unlock_status"

        const val EXTRA_STATUS = "status"
        const val EXTRA_ERROR_RES = "error"

        const val MIN_UNLOCK_MS = 500L

        fun triggerDoorLock(context: Context) {
            val intent = Intent(context, DoorUnlockService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_LOCK
            context.startService(intent)
        }

        fun triggerDoorUnlock(context: Context) {
            val intent = Intent(context, DoorUnlockService::class.java)
            intent.`package` = BuildConfig.APPLICATION_ID
            intent.action = ACTION_UNLOCK
            context.startService(intent)
        }
    }

}