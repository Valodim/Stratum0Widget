package org.stratum0.statuswidget.interactors

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import org.stratum0.statuswidget.BuildConfig
import org.stratum0.statuswidget.Constants
import org.stratum0.statuswidget.SpaceStatusData
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class Stratum0StatusFetcher {
    private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

    fun fetch(): SpaceStatusData {
        val result: String
        val request = Request.Builder()
                .url(Constants.STATUS_URL + "/status.json")
                .build()

        if (false && BuildConfig.DEBUG) {
            if (debugListIndex == DEBUG_STATUS_LIST.size) {
                debugListIndex = 0
            } else {
                Thread.sleep(200)
                debugListIndex += 1
                return DEBUG_STATUS_LIST[debugListIndex - 1]
            }
        }

        try {
            val response = okHttpClient.newCall(request).execute()
            if (response.code() == 200) {
                result = response.body()!!.string()
            } else {
                Log.d(Constants.TAG, "Got negative http reply " + response.code())
                return SpaceStatusData.createUnknownStatus()
            }
        } catch (e: IOException) {
            Log.e(Constants.TAG, "IOException: " + e.message, e)
            return SpaceStatusData.createUnknownStatus()
        }

        try {
            val jsonRoot = JSONObject(result)
            val spaceStatus = jsonRoot.getJSONObject("state")

            val lastChange = GregorianCalendar.getInstance()
            lastChange.timeInMillis = spaceStatus.getLong("lastchange") * 1000

            if (spaceStatus.getBoolean("open")) {
                val rawOpenedBy = spaceStatus.getString("trigger_person")
                val openedBy = rawOpenedBy.substringBeforeLast("[mx]")

                val since = GregorianCalendar.getInstance()
                since.timeInMillis = spaceStatus.getLong("ext_since") * 1000

                return SpaceStatusData.createOpenStatus(openedBy, lastChange, since)
            } else {
                return SpaceStatusData.createClosedStatus(lastChange)
            }
        } catch (e: JSONException) {
            Log.d(Constants.TAG, "Error creating JSON object: " + e)
            return SpaceStatusData.createUnknownStatus()
        }

    }

    companion object {
        val DEBUG_STATUS_LIST: Array<SpaceStatusData> = arrayOf(
                SpaceStatusData.createUnknownStatus(),
                SpaceStatusData.createClosedStatus(Calendar.getInstance()),
                SpaceStatusData.createOpenStatus("Valodim", Calendar.getInstance(), Calendar.getInstance()))
        private var debugListIndex = DEBUG_STATUS_LIST.size
    }

}