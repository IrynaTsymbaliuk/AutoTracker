package com.auto.tracker.collector.steps.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "steps_hourly")
internal data class StepsEntity(
    @PrimaryKey
    val hourStart: Long,
    val hourEnd: Long,
    val count: Long,
    val zoneOffset: String,
    val syncedAt: Long
)
