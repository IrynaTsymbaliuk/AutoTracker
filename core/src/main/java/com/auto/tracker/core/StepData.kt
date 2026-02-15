package com.auto.tracker.core

data class StepsData(
    override val timestamp: Long,  // epoch millis - start of hour or day depending on granularity
    override val count: Long
) : HealthData