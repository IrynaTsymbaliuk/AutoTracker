package com.auto.tracker.core

import kotlinx.coroutines.flow.Flow

interface GranularDataCollector<T : HealthData> : DataCollector<T> {
    suspend fun get(from: Long, to: Long, granularity: Granularity): List<T>
    fun observe(granularity: Granularity): Flow<List<T>>
}
