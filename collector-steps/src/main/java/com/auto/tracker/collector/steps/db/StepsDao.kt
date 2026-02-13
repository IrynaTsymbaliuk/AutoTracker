package com.auto.tracker.collector.steps.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface StepsDao {

    @Upsert
    suspend fun upsert(steps: List<StepsEntity>)

    @Query(
        """
        SELECT * FROM steps_hourly 
        WHERE hourStart >= :from AND hourEnd <= :to 
        ORDER BY hourStart ASC
    """
    )
    fun observe(from: Long, to: Long): Flow<List<StepsEntity>>

    @Query(
        """
        SELECT * FROM steps_hourly 
        WHERE hourStart >= :from AND hourEnd <= :to 
        ORDER BY hourStart ASC
    """
    )
    suspend fun get(from: Long, to: Long): List<StepsEntity>

    @Query("DELETE FROM steps_hourly WHERE hourStart < :before")
    suspend fun deleteBefore(before: Long)
}
