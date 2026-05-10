package com.daniel.silo.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.daniel.silo.domain.model.SyncResult
import com.daniel.silo.domain.repository.SiloRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: SiloRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 1. Push pending local changes
        val syncResult = repository.syncPending()
        if (syncResult is SyncResult.Error && runAttemptCount < 3) return Result.retry()

        // 2. Refresh full state from server
        repository.refreshAll()

        return Result.success()
    }

    companion object {
        const val WORK_NAME_PERIODIC = "silo_sync_periodic"
        const val WORK_NAME_ONESHOT  = "silo_sync_oneshot"

        fun enqueueOneShot(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_ONESHOT,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<SyncWorker>()
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                    .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                    .build()
            )
        }

        fun enqueuePeriodic(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                    .setConstraints(Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                    .build()
            )
        }
    }
}
