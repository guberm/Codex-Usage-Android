package com.guberdev.codexusage

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import java.util.concurrent.TimeUnit

class UsageBackupJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        if (SecureTokenStore(this).load() == null) {
            jobFinished(params, false)
            return false
        }
        RefreshCoordinator.refresh(this) {
            jobFinished(params, false)
        }
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JOB_ID = 48191

        fun schedule(context: Context) {
            val hours = MonitorSettingsStore(context).load().checkEveryHours.toLong()
            val interval = TimeUnit.HOURS.toMillis(hours)
                .coerceAtLeast(JobInfo.getMinPeriodMillis())
            val info = JobInfo.Builder(
                JOB_ID,
                ComponentName(context, UsageBackupJobService::class.java),
            )
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPersisted(true)
                .setPeriodic(interval)
                .build()
            context.getSystemService(JobScheduler::class.java).schedule(info)
        }

        fun cancel(context: Context) {
            context.getSystemService(JobScheduler::class.java).cancel(JOB_ID)
        }
    }
}
