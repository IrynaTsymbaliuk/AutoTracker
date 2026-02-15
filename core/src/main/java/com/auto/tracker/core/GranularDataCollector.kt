package com.auto.tracker.core

interface GranularDataCollector<T : HealthData> : DataCollector<T> {
    suspend fun get(from: Long, to: Long, granularity: Granularity): List<T>
}
