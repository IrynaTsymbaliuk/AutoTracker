package com.auto.tracker.collector.steps

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.work.PeriodicWorkRequestBuilder
import com.auto.tracker.collector.steps.db.StepsDatabase
import com.auto.tracker.collector.steps.db.StepsEntity
import com.auto.tracker.core.GranularDataCollector
import com.auto.tracker.core.Granularity
import com.auto.tracker.core.PermissionState
import com.auto.tracker.core.StepsData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class StepsCollector private constructor(private val context: Context) :
    GranularDataCollector<StepsData> {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }
    private val syncManager = StepsSyncManager(context)
    private val dao = StepsDatabase.getInstance(context).stepsDao()

    fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override suspend fun checkPermissions(): PermissionState {
        if (!isAvailable()) return PermissionState.Denied
        val granted = client.permissionController.getGrantedPermissions()
        return if (granted.containsAll(PERMISSIONS)) PermissionState.Granted
        else PermissionState.Denied
    }

    override suspend fun sync(): Result<Unit> = runCatching {
        syncManager.sync()
    }

    override fun observe(): Flow<List<StepsData>> {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val now = Instant.now()

        return dao.observe(
            from = startOfDay.toEpochMilli(),
            to = now.toEpochMilli()
        ).map { entities ->
            entities.map { entity ->
                StepsData(
                    timestamp = entity.hourStart,
                    count = entity.count
                )
            }
        }
    }

    override suspend fun get(from: Long, to: Long, granularity: Granularity): List<StepsData> {
        val entities = dao.get(from, to)

        return when (granularity) {
            Granularity.HOURLY -> aggregateByHour(entities)
            Granularity.DAILY -> aggregateByDay(entities)
        }
    }

    override fun observe(granularity: Granularity): Flow<List<StepsData>> {
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
        val now = Instant.now()

        return dao.observe(
            from = startOfDay.toEpochMilli(),
            to = now.toEpochMilli()
        ).map { entities ->
            when (granularity) {
                Granularity.HOURLY -> aggregateByHour(entities)
                Granularity.DAILY -> aggregateByDay(entities)
            }
        }
    }

    suspend fun forceFullSync(): Result<Unit> = runCatching {
        syncManager.clearToken()
        syncManager.sync()
    }

    fun enableBackgroundSync(context: Context) {
        val request = PeriodicWorkRequestBuilder<StepsSyncWorker>(
            repeatInterval = 1,
            repeatIntervalTimeUnit = java.util.concurrent.TimeUnit.HOURS
        )
            .setConstraints(
                androidx.work.Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .build()
            )
            .build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                StepsSyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun disableBackgroundSync(context: Context) {
        androidx.work.WorkManager.getInstance(context)
            .cancelUniqueWork(StepsSyncWorker.WORK_NAME)
    }

    private fun aggregateByHour(entities: List<StepsEntity>): List<StepsData> {
        return entities
            .groupBy { entity ->
                // Group by start of hour
                val instant = Instant.ofEpochMilli(entity.hourStart)
                val zonedDateTime = instant.atZone(ZoneId.systemDefault())
                zonedDateTime.toLocalDate().atStartOfDay(ZoneId.systemDefault())
                    .plusHours(zonedDateTime.hour.toLong())
                    .toInstant()
                    .toEpochMilli()
            }
            .toSortedMap()
            .map { (hourTimestamp, hourEntities) ->
                StepsData(
                    timestamp = hourTimestamp,
                    count = hourEntities.sumOf { it.count }
                )
            }
    }

    private fun aggregateByDay(entities: List<StepsEntity>): List<StepsData> {
        return entities
            .groupBy { entity ->
                // Group by start of day
                Instant.ofEpochMilli(entity.hourStart)
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
            }
            .toSortedMap()
            .map { (dayTimestamp, dayEntities) ->
                StepsData(
                    timestamp = dayTimestamp,
                    count = dayEntities.sumOf { it.count }
                )
            }
    }

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )

        @Volatile
        private var instance: StepsCollector? = null

        fun getInstance(context: Context): StepsCollector =
            instance ?: synchronized(this) {
                StepsCollector(context.applicationContext).also { instance = it }
            }
    }
}