package com.auto.tracker.core

sealed interface HealthData {
    val timestamp: Long
    val count: Long
}
