package com.auto.tracker.core

object HealthDataManager {

    /**
     * Retrieves health data for the specified type and time range.
     *
     * @param type The type of health data to retrieve (e.g., STEPS, SLEEP)
     * @param from Start timestamp in milliseconds (epoch)
     * @param to End timestamp in milliseconds (epoch)
     * @param granularity The granularity of aggregation (HOURLY or DAILY)
     * @return List of health data entries for the specified range
     * @throws IllegalStateException if the collector for the specified type is not registered
     */
    suspend fun get(
        type: DataType,
        from: Long,
        to: Long,
        granularity: Granularity = Granularity.DAILY
    ): List<HealthData> {
        return DataCollectorRegistry.get(type, from, to, granularity)
    }

    /**
     * Checks if a specific data type is available (collector is registered).
     *
     * @param type The type of health data to check
     * @return true if the collector is registered and available, false otherwise
     */
    fun isAvailable(type: DataType): Boolean {
        return DataCollectorRegistry.isAvailable(type)
    }

    /**
     * Gets all currently registered and available data types.
     *
     * @return Set of available data types
     */
    fun getAvailableTypes(): Set<DataType> {
        return DataCollectorRegistry.getAvailableTypes()
    }

    /**
     * Registers a data collector for a specific data type.
     * This should only be called by collector module initializers.
     *
     * @param type The type of health data this collector handles
     * @param collector The collector implementation
     */
    fun registerCollector(type: DataType, collector: DataCollector<out HealthData>) {
        DataCollectorRegistry.register(type, collector)
    }

    /**
     * Unregisters a data collector for a specific data type.
     * This should only be called by collector module initializers during cleanup.
     *
     * @param type The type of health data to unregister
     */
    fun unregisterCollector(type: DataType) {
        DataCollectorRegistry.unregister(type)
    }
}
