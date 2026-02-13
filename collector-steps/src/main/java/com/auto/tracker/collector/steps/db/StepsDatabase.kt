package com.auto.tracker.collector.steps.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [StepsEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class StepsDatabase : RoomDatabase() {

    abstract fun stepsDao(): StepsDao

    companion object {
        @Volatile private var instance: StepsDatabase? = null

        fun getInstance(context: Context): StepsDatabase =
            instance ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    StepsDatabase::class.java,
                    "collector.steps.db"
                ).build().also { instance = it }
            }
    }
}
