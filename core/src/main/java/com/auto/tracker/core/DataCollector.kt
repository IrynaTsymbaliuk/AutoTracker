package com.auto.tracker.core

import kotlinx.coroutines.flow.Flow

interface DataCollector<T> {
    suspend fun checkPermissions(): PermissionState
    suspend fun sync(): Result<Unit>
    fun observe(): Flow<List<T>>
}

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
}
