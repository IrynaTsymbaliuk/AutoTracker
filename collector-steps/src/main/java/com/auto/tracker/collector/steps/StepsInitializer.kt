package com.auto.tracker.collector.steps

import android.content.Context
import androidx.startup.Initializer
import com.auto.tracker.core.DataType
import com.auto.tracker.core.HealthDataManager

class StepsInitializer : Initializer<Unit> {

    override fun create(context: Context) {
        HealthDataManager.registerCollector(
            DataType.STEPS,
            StepsCollector.getInstance(context)
        )
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}
