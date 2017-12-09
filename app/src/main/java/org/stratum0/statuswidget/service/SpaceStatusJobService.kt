package org.stratum0.statuswidget.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import org.stratum0.statuswidget.BuildConfig
import org.stratum0.statuswidget.Constants
import org.stratum0.statuswidget.SpaceStatus
import org.stratum0.statuswidget.SpaceStatusData
import org.stratum0.statuswidget.interactors.Stratum0StatusFetcher

class SpaceStatusJobService : JobService() {
    private val stratum0StatusFetcher = Stratum0StatusFetcher()

    override fun onStartJob(params: JobParameters): Boolean {
        object : AsyncTask<Void,Void, SpaceStatusData>() {
            override fun doInBackground(vararg p0: Void?): SpaceStatusData {
                return stratum0StatusFetcher.fetch()
            }

            override fun onPostExecute(result: SpaceStatusData) {
                sendRefreshBroadcast(result)

                val isSuccessful = result.status != SpaceStatus.UNKNOWN
                jobFinished(params, !isSuccessful)
            }
        }.execute()

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    private fun sendRefreshBroadcast(statusData: SpaceStatusData) {
        val intent = Intent(EVENT_REFRESH)
        intent.`package` = BuildConfig.APPLICATION_ID
        intent.putExtra(SpaceStatusService.EXTRA_STATUS, statusData)
        sendBroadcast(intent)
    }

    companion object {
        val EVENT_REFRESH = "SpaceStatus.event.refresh"

        fun jobScheduleRefresh(context: Context) {
            val serviceComponent = ComponentName(context, SpaceStatusJobService::class.java)

            val job = JobInfo.Builder(Constants.JOB_ID_SPACE_STATUS_REFRESH, serviceComponent)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                job.setPeriodic(45 * DateUtils.MINUTE_IN_MILLIS, 15 * DateUtils.MINUTE_IN_MILLIS)
            } else {
                job.setPeriodic(45 * DateUtils.MINUTE_IN_MILLIS)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                job.setRequiresBatteryNotLow(true)
            }

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(job.build())

            Log.d(Constants.TAG, "Job scheduled!")
        }

        fun jobCancelRefresh(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(Constants.JOB_ID_SPACE_STATUS_REFRESH)

            Log.d(Constants.TAG, "Job cancelled!")
        }
    }
}