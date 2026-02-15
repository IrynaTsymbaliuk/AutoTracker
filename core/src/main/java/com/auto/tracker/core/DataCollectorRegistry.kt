package com.auto.tracker.core

internal object DataCollectorRegistry {

    private val collectors = mutableMapOf<DataType, DataCollector<out HealthData>>()

    fun register(type: DataType, collector: DataCollector<out HealthData>) {
        collectors[type] = collector
    }

    fun unregister(type: DataType) {
        collectors.remove(type)
    }

    fun getCollector(type: DataType): DataCollector<out HealthData>? {
        return collectors[type]
    }

    fun isAvailable(type: DataType): Boolean {
        return collectors.containsKey(type)
    }

    fun getAvailableTypes(): Set<DataType> {
        return collectors.keys.toSet()
    }

    suspend fun get(
        type: DataType,
        from: Long,
        to: Long,
        granularity: Granularity
    ): List<HealthData> {
        val collector = getCollector(type)
            ?: throw IllegalStateException("Collector for $type is not registered. Make sure the corresponding module is included in your dependencies.")

        return when (collector) {
            is GranularDataCollector -> collector.get(from, to, granularity)
            else -> throw UnsupportedOperationException("Collector for $type does not support get() with granularity")
        }
    }
}
