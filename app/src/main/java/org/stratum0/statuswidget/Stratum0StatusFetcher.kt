package org.stratum0.statuswidget

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.TimeUnit

class Stratum0StatusFetcher {
    private val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .build()

    fun fetch(): SpaceStatusData {
        val result: String
        val request = Request.Builder()
                .url(Constants.STATUS_URL + "/status.json")
                .build()

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
                val openedBy = spaceStatus.getString("trigger_person")

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

}