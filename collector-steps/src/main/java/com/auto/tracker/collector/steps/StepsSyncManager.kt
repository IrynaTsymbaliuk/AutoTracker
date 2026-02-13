package com.auto.tracker.collector.steps

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.changes.UpsertionChange
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByDurationRequest
import androidx.health.connect.client.request.ChangesTokenRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.auto.tracker.collector.steps.db.StepsDatabase
import com.auto.tracker.collector.steps.db.StepsEntity
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

internal class StepsSyncManager(private val context: Context) {

    private val client = HealthConnectClient.getOrCreate(context)
    private val dao = StepsDatabase.getInstance(context).stepsDao()

    private val prefs = context.getSharedPreferences(
        "collector_steps_prefs",
        Context.MODE_PRIVATE
    )

    suspend fun sync() {
        val savedToken = prefs.getString(KEY_CHANGES_TOKEN, null)

        if (savedToken == null) {
            initialSync()
        } else {
            incrementalSync(savedToken)
        }
    }

    private suspend fun initialSync() {
        val now = Instant.now()
        val from = LocalDate.now()
            .minusDays(30)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val token = client.getChangesToken(
            ChangesTokenRequest(recordTypes = setOf(StepsRecord::class))
        )

        fetchAndSave(from = from, to = now)

        saveToken(token)
    }

    private suspend fun incrementalSync(token: String) {
        var currentToken = token

        do {
            val response = client.getChanges(currentToken)

            val changedTimeRanges = response.changes
                .filterIsInstance<UpsertionChange>()
                .map { change ->
                    val record = change.record as? StepsRecord
                    record?.startTime to record?.endTime
                }
                .filter { (start, end) -> start != null && end != null }

            changedTimeRanges.forEach { (start, end) ->
                if (start != null && end != null) {
                    val hourStart = start.truncatedTo(java.time.temporal.ChronoUnit.HOURS)
                    val hourEnd = end
                        .truncatedTo(java.time.temporal.ChronoUnit.HOURS)
                        .plus(Duration.ofHours(1))

                    fetchAndSave(from = hourStart, to = hourEnd)
                }
            }

            currentToken = response.nextChangesToken
        } while (response.hasMore)

        saveToken(currentToken)

        val cutoff = Instant.now()
            .minus(Duration.ofDays(366))
            .toEpochMilli()
        dao.deleteBefore(cutoff)
    }

    private suspend fun fetchAndSave(from: Instant, to: Instant) {
        val response = client.aggregateGroupByDuration(
            AggregateGroupByDurationRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from, to),
                timeRangeSlicer = Duration.ofHours(1)
            )
        )

        val entities = response.map { bucket ->
            StepsEntity(
                hourStart = bucket.startTime.toEpochMilli(),
                hourEnd = bucket.endTime.toEpochMilli(),
                count = bucket.result[StepsRecord.COUNT_TOTAL] ?: 0L,
                zoneOffset = (bucket.zoneOffset ?: ZoneOffset.UTC).toString(),
                syncedAt = Instant.now().toEpochMilli()
            )
        }

        if (entities.isNotEmpty()) {
            dao.upsert(entities)
        }
    }

    private fun saveToken(token: String) {
        prefs.edit().putString(KEY_CHANGES_TOKEN, token).apply()
    }

    fun clearToken() {
        prefs.edit().remove(KEY_CHANGES_TOKEN).apply()
    }

    companion object {
        private const val KEY_CHANGES_TOKEN = "changes_token"
    }
}
