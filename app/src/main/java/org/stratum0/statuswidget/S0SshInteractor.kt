package org.stratum0.statuswidget

import android.os.SystemClock
import android.util.Log
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.Session
import java.io.ByteArrayOutputStream
import java.net.ConnectException

class S0SshInteractor {
    val MAX_CONNECTION_TIME = 3500

    fun open(sshPrivateKey: String, sshPassword: String): Int? {
        val user = "auf"
        val server = if (BuildConfig.DEBUG) "192.168.178.21" else "powerberry"

        val jsch = JSch()
        JSch.setConfig("StrictHostKeyChecking", "no")

        try {
            jsch.addIdentity("id_rsa", sshPrivateKey.toByteArray(), null, sshPassword.toByteArray())
        } catch (e: JSchException) {
            e.printStackTrace()
            return R.string.unlock_error_identity
        }

        val sshSession: Session
        sshSession = jsch.getSession(user, server)

        Log.d(this.javaClass.name, "Trying to connect...")

        val baos = ByteArrayOutputStream()
        baos.write("Output: ".toByteArray(), 0, 8)

        try {
            sshSession.connect(3000)

            val channel = sshSession.openChannel("shell")
            channel.setOutputStream(baos)
            channel.connect(3000)

            val startTime = SystemClock.elapsedRealtime()
            while (channel.isConnected()) {
                if (SystemClock.elapsedRealtime() - startTime > MAX_CONNECTION_TIME) {
                    channel.disconnect()
                    break
                }
            }

            Log.d(this.javaClass.name, baos.toString())
        } catch (e: JSchException) {
            Log.d(this.javaClass.name, "Connect NOT successful", e)
            if (e.cause is ConnectException) {
                return R.string.unlock_error_connect
            }
            when (e.message) {
                "Auth fail" -> return R.string.unlock_error_auth
            }
            return R.string.unlock_error_unknown
        }

        Log.d(this.javaClass.name, "Connect successful")

        return null
    }

}