package com.auto.tracker.collector.steps

import android.annotation.SuppressLint
import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.auto.tracker.core.GranularDataCollector
import com.auto.tracker.core.Granularity
import com.auto.tracker.core.PermissionState
import com.auto.tracker.core.StepsData
import java.time.Duration
import java.time.Instant

class StepsCollector private constructor(private val context: Context) :
    GranularDataCollector<StepsData> {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    private fun isAvailable(): Boolean =
        HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE

    override suspend fun checkPermissions(): PermissionState {
        if (!isAvailable()) return PermissionState.Unavailable
        val granted = client.permissionController.getGrantedPermissions()
        return if (granted.containsAll(PERMISSIONS)) PermissionState.Granted
        else PermissionState.Denied
    }

    override suspend fun get(from: Long, to: Long, granularity: Granularity): List<StepsData> {
        return runCatching {
            fetchFromHealthConnect(
                from = Instant.ofEpochMilli(from),
                to = Instant.ofEpochMilli(to),
                granularity = granularity
            )
        }.getOrElse { emptyList() }
    }

    private suspend fun fetchFromHealthConnect(
        from: Instant,
        to: Instant,
        granularity: Granularity
    ): List<StepsData> {
        val duration = when (granularity) {
            Granularity.HOURLY -> Duration.ofHours(1)
            Granularity.DAILY -> Duration.ofDays(1)
        }

        val response = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from, to),
                timeRangeSlicer = duration
            )
        )

        return response.map { bucket ->
            StepsData(
                timestamp = bucket.startTime.toEpochMilli(),
                count = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L
            )
        }
    }

    companion object {
        val PERMISSIONS = setOf(
            HealthPermission.getReadPermission(StepsRecord::class)
        )

        // Safe to hold Application Context in static field - it lives for the app's lifetime
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: StepsCollector? = null

        fun getInstance(context: Context): StepsCollector =
            instance ?: synchronized(this) {
                StepsCollector(context.applicationContext).also { instance = it }
            }
    }
}