package org.stratum0.statuswidget.push

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.AsyncTask
import android.os.Build
import android.text.format.DateUtils
import android.util.Log
import org.stratum0.statuswidget.Constants
import org.stratum0.statuswidget.SpaceStatus
import org.stratum0.statuswidget.SpaceStatusData
import org.stratum0.statuswidget.interactors.Stratum0StatusFetcher
import org.stratum0.statuswidget.service.StratumsphereStatusProvider

class SpaceUpdateJobService : JobService() {
    private val stratum0StatusFetcher = Stratum0StatusFetcher()

    override fun onStartJob(params: JobParameters): Boolean {
        object : AsyncTask<Void,Void, SpaceStatusData>() {
            override fun doInBackground(vararg p0: Void?): SpaceStatusData {
                return stratum0StatusFetcher.fetch()
            }

            override fun onPostExecute(result: SpaceStatusData) {
                StratumsphereStatusProvider.sendRefreshBroadcast(applicationContext, result)

                val isSuccessful = result.status != SpaceStatus.ERROR
                jobFinished(params, !isSuccessful)
            }
        }.execute()

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        fun jobScheduleConnectivityRefresh(context: Context) {
            val serviceComponent = ComponentName(context, SpaceUpdateJobService::class.java)

            val job = JobInfo.Builder(Constants.JOB_ID_SPACE_STATUS_REFRESH_CONNECTIVITY, serviceComponent)
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)

            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.schedule(job.build())
        }

        fun jobSchedulePeriodicRefresh(context: Context) {
            val serviceComponent = ComponentName(context, SpaceUpdateJobService::class.java)

            val job = JobInfo.Builder(Constants.JOB_ID_SPACE_STATUS_REFRESH_PERIODIC, serviceComponent)
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

        fun jobCancelPeriodicRefresh(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            jobScheduler.cancel(Constants.JOB_ID_SPACE_STATUS_REFRESH_PERIODIC)

            Log.d(Constants.TAG, "Job cancelled!")
        }
    }
}