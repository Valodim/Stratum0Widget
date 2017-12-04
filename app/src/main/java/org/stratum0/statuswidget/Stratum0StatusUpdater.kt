package org.stratum0.statuswidget

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.ExecutionException

class Stratum0StatusUpdater {
    fun update(name: String?) {
        val queryString = if (name != null) {
            Constants.STATUS_URL + "/update?open=true&by=" + name
        } else {
            Constants.STATUS_URL + "/update?open=false"
        }

        try {
            val okHttpClient = OkHttpClient()

            val response = okHttpClient.newCall(Request.Builder().url(queryString).build()).execute()

            if(response.code() == 200) {
                Thread.sleep(500)
                // TODO update
            }
        } catch (e: IOException) {
            Log.e(Constants.TAG, "Update request: could not connect to server.", e)
        } catch (e: InterruptedException) {
            Log.e(Constants.TAG, "Wait for new status didn't finish:", e)
        } catch (e: ExecutionException) {
            Log.e(Constants.TAG, "Error executing update task inside change task:", e)
        }
    }

}