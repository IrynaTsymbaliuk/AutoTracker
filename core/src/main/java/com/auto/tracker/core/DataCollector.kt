package com.auto.tracker.core

interface DataCollector<T> {
    suspend fun checkPermissions(): PermissionState
}

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object Unavailable : PermissionState()
}
