package horse.amazin.my.stratum0.statuswidget.interactors

import android.os.SystemClock
import horse.amazin.my.stratum0.statuswidget.BuildConfig
import horse.amazin.my.stratum0.statuswidget.R
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.common.DisconnectReason
import net.schmizz.sshj.transport.TransportException
import net.schmizz.sshj.userauth.UserAuthException
import net.schmizz.sshj.userauth.password.PasswordUtils
import okhttp3.internal.closeQuietly
import timber.log.Timber
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException


class SshInteractor {
    fun open(sshPrivateKey: String, sshPassword: String): Int? {
        val user = if (BuildConfig.DEBUG) "valodim" else "auf"
        val server = if (BuildConfig.DEBUG) "192.168.178.21" else "powerberry"
        val expectedHostKey = if (BuildConfig.DEBUG) "c5:ee:ae:36:c6:fb:77:d5:c3:00:4f:d9:6d:da:fb:7f" else "SHA256:Z9I6IWdocW/tjlJm23iiZ4m2dZVD512329g0B3nn/JA"

        val sshClient = SSHClient()

        sshClient.addHostKeyVerifier(expectedHostKey)
        sshClient.connectTimeout = 3000

        val keys = try {
            sshClient.loadKeys(sshPrivateKey, null, PasswordUtils.createOneOff(sshPassword.toCharArray()))
        } catch (e: IOException) {
            Timber.e(e, "Failed loading identity!")
            return R.string.unlock_error_identity
        }

        Timber.d("Trying to connect...")

        try {
            sshClient.connect(server)
            sshClient.authPublickey(user, keys)
            val sshSession = sshClient.startSession()
            val shell = sshSession.startShell()

            val startTime = SystemClock.elapsedRealtime()
            while (!shell.isEOF) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) { }
                if (SystemClock.elapsedRealtime() - startTime > MAX_CONNECTION_TIME) {
                    shell.closeQuietly()
                    break
                }
            }

            val receivedText = shell.inputStream.bufferedReader().readText()
            Timber.d("Received text: %s", receivedText)
        } catch (e: UserAuthException) {
            Timber.e(e)
            return R.string.unlock_error_auth
        } catch (e: TransportException) {
            Timber.e(e, "Disconnect reason: %s", e.disconnectReason)
            return when (e.disconnectReason) {
                DisconnectReason.HOST_KEY_NOT_VERIFIABLE -> R.string.unlock_error_serverauth
                DisconnectReason.NO_MORE_AUTH_METHODS_AVAILABLE -> R.string.unlock_error_auth
                else -> R.string.unlock_error_network
            }
        } catch (e: UnknownHostException) {
            Timber.e(e)
            return R.string.unlock_error_resolve
        } catch (e: SocketTimeoutException) {
            Timber.e(e)
            return R.string.unlock_error_timeout
        } catch (e: ConnectException) {
            Timber.e(e)
            return R.string.unlock_error_connect
        } catch (e: IOException) {
            Timber.e(e)
            return R.string.unlock_error_unknown
        }

        Timber.d("Connect successful")

        return null
    }

    companion object {
        private val MAX_CONNECTION_TIME = 3500
    }

}