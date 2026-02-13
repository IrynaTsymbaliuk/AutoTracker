package com.auto.tracker.collector.steps

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

internal class StepsSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val collector = StepsCollector.getInstance(applicationContext)

        if (collector.checkPermissions() != com.auto.tracker.core.PermissionState.Granted) {
            return Result.success()
        }

        return collector.sync()
            .fold(
                onSuccess = { Result.success() },
                onFailure = { Result.retry() }
            )
    }

    companion object {
        const val WORK_NAME = "lt_steps_sync"
    }
}
