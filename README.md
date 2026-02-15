# AutoTracker

[![JitPack](https://jitpack.io/v/IrynaTsymbaliuk/AutoTracker.svg)](https://jitpack.io/#IrynaTsymbaliuk/AutoTracker)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Android API](https://img.shields.io/badge/API-26%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=26)

AutoTracker is a modern Android library that provides an easy and efficient way to collect user health and activity data. It handles permissions, data synchronization, local storage, and background updates automatically.

## Features

- **Unified API**: Single `HealthDataManager` entry point for all health data types
- **Plugin Architecture**: Auto-registration of collectors via AndroidX Startup
- **Flexible Granularity**: Support for hourly and daily data aggregation
- **Simple API**: Clean and intuitive interface for data collection
- **Automatic Background Sync**: Periodic data updates without manual intervention
- **Local Storage**: Efficient caching with Room database
- **Incremental Sync**: Smart synchronization using change tokens to minimize data transfer
- **Permission Management**: Built-in permission state checking
- **Battery Efficient**: Respects device battery constraints
- **Reactive**: Kotlin Flow-based observation for real-time updates
- **Type-Safe**: Strongly typed data models
- **Modular**: Include only the collectors you need

## Current Data Sources

### Steps Collection (Health Connect)
- Collects step count data from Health Connect API
- Supports hourly and daily granularity
- Automatic background sync every 1 hour
- Stores last 366 days of data
- Initial sync covers last 30 days
- Auto-registers via AndroidX Startup

## Installation

### 1. Add Repository

Add JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add Dependencies

In your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.IrynaTsymbaliuk.AutoTracker:core:0.0.2")
    implementation("com.github.IrynaTsymbaliuk.AutoTracker:collector-steps:0.0.2")
}
```

### 3. Configure Health Connect

Add to your `AndroidManifest.xml`:

```xml
<manifest>
    <!-- Health Connect permissions -->
    <uses-permission android:name="android.permission.health.READ_STEPS"/>

    <application>
        <!-- Health Connect activity intent -->
        <activity-alias
            android:name="ViewPermissionUsageActivity"
            android:exported="true"
            android:targetActivity=".MainActivity"
            android:permission="android.permission.START_VIEW_PERMISSION_USAGE">
            <intent-filter>
                <action android:name="androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" />
            </intent-filter>
        </activity-alias>
    </application>
</manifest>
```

## Usage

### Unified API (Recommended - v0.0.2+)

The library automatically registers all collectors via AndroidX Startup. Simply use `HealthDataManager`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Check what data types are available
            val available = HealthDataManager.getAvailableTypes()
            // Returns: [STEPS] if collector-steps module is included

            // Get daily step data for the last 7 days
            val weekAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000)
            val now = System.currentTimeMillis()

            val dailySteps = HealthDataManager.get(
                type = DataType.STEPS,
                from = weekAgo,
                to = now,
                granularity = Granularity.DAILY
            )

            dailySteps.forEach { stepData ->
                println("${stepData.timestamp}: ${stepData.count} steps")
            }
        }
    }
}
```

### Direct Collector Access (Advanced)

For advanced use cases, you can still access collectors directly:

```kotlin
class MainActivity : ComponentActivity() {
    private val stepsCollector by lazy {
        StepsCollector.getInstance(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            // Check if Health Connect is available
            if (!stepsCollector.isAvailable()) {
                // Guide user to install Health Connect
                return@launch
            }

            // Check permissions
            when (stepsCollector.checkPermissions()) {
                PermissionState.Granted -> {
                    // Start collecting data
                    startDataCollection()
                }
                PermissionState.Denied -> {
                    // Request permissions
                    requestPermissions()
                }
            }
        }
    }
}
```

### Request Permissions

```kotlin
private val permissionLauncher = registerForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
) { granted ->
    if (granted.values.all { it }) {
        // All permissions granted
        startDataCollection()
    } else {
        // Show rationale or guide user
    }
}

private fun requestPermissions() {
    val contract = PermissionController.createRequestPermissionResultContract()
    val intent = contract.createIntent(
        this,
        StepsCollector.PERMISSIONS
    )
    permissionLauncher.launch(intent)
}
```

### Sync Data

```kotlin
lifecycleScope.launch {
    // Manual sync
    stepsCollector.sync()
        .onSuccess {
            Log.d("AutoTracker", "Sync successful")
        }
        .onFailure { error ->
            Log.e("AutoTracker", "Sync failed", error)
        }
}
```

### Enable Background Sync

```kotlin
// Enable automatic hourly sync
stepsCollector.enableBackgroundSync(context)

// Disable when no longer needed
stepsCollector.disableBackgroundSync(context)
```

### Observe Data

With HealthDataManager (v0.0.2+):

```kotlin
lifecycleScope.launch {
    // Observe hourly step data
    HealthDataManager.observe(DataType.STEPS, Granularity.HOURLY)
        .collect { stepsDataList ->
            stepsDataList.forEach { data ->
                println("${data.timestamp}: ${data.count} steps")
            }
        }
}
```

Direct collector access:

```kotlin
lifecycleScope.launch {
    stepsCollector.observe(Granularity.DAILY)
        .collect { stepsDataList ->
            // stepsDataList contains today's step counts
            stepsDataList.forEach { data ->
                println("${data.timestamp}: ${data.count} steps")
            }
        }
}
```

### Compose Integration

```kotlin
@Composable
fun StepsScreen() {
    val dailySteps by HealthDataManager.observe(DataType.STEPS, Granularity.DAILY)
        .collectAsState(initial = emptyList())

    LazyColumn {
        items(dailySteps) { data ->
            val date = remember(data.timestamp) {
                SimpleDateFormat("MMM dd", Locale.getDefault())
                    .format(Date(data.timestamp))
            }
            Text("$date: ${data.count} steps")
        }
    }
}
```

## Architecture

### Plugin-Based Design (v0.0.2+)

AutoTracker uses a plugin architecture where each collector auto-registers via AndroidX Startup:

```
┌──────────────────────────────────────┐
│      HealthDataManager (core)        │  ← Single unified API
│  - get(type, from, to, granularity)  │
│  - observe(type, granularity)        │
│  - isAvailable(type)                 │
└──────────────────────────────────────┘
                   ↓
┌──────────────────────────────────────┐
│   DataCollectorRegistry (internal)    │  ← Plugin registry
│  - Manages registered collectors      │
└──────────────────────────────────────┘
                   ↓
    ┌──────────────┴──────────────┐
    ↓                             ↓
┌────────────────┐      ┌────────────────┐
│ StepsCollector │      │ SleepCollector │  ← Auto-registered
│   (Steps)      │      │   (Future)     │     via Initializers
└────────────────┘      └────────────────┘
```

### Core Module

The `core` module provides the foundation:

```kotlin
// Base interface for all collectors
interface DataCollector<T> {
    suspend fun checkPermissions(): PermissionState
    suspend fun sync(): Result<Unit>
    fun observe(): Flow<List<T>>
}

// Extended interface with granularity support
interface GranularDataCollector<T : HealthData> : DataCollector<T> {
    suspend fun get(from: Long, to: Long, granularity: Granularity): List<T>
    fun observe(granularity: Granularity): Flow<List<T>>
}
```

### Collector Modules

Each collector module:
1. Implements `GranularDataCollector<T>`
2. Provides an `Initializer` that auto-registers on app startup
3. Is completely independent (can be included/excluded as needed)

**Available collectors:**
- **collector-steps**: Steps data from Health Connect ✓
- **collector-sleep**: Sleep data *(coming soon)*
- **collector-exercise**: Exercise/workout data *(coming soon)*

### Data Flow

```
Health Connect API
        ↓
StepsSyncManager (incremental sync with change tokens)
        ↓
Room Database (local storage with hourly granularity)
        ↓
StepsCollector (aggregation by hour/day)
        ↓
HealthDataManager (unified API)
        ↓
Your App UI
```

### Auto-Registration

When your app starts:
1. AndroidX Startup discovers all `Initializer` classes
2. Each collector's initializer runs automatically
3. Collectors register themselves with `HealthDataManager`
4. Your app can immediately use `HealthDataManager.get()` / `observe()`

No manual setup required!

### Background Sync

- Runs every 1 hour via WorkManager
- Checks permissions before syncing
- Retries on failure
- Battery-aware (requires battery not low)

## Requirements

- Android API 26+ (Android 8.0 Oreo)
- Health Connect app installed (for steps collection)
- Kotlin 1.9+
- Kotlin Coroutines

## Roadmap

We have exciting plans for the future of AutoTracker:

### Short Term
- [ ] Sleep data collection
- [ ] Workout/training data collection
- [ ] Additional Health Connect metrics

### Medium Term
- [ ] Screen time tracking
- [ ] App usage by categories
- [ ] Multiple data source providers (Google Fit, Samsung Health, etc.)

### Long Term
- [ ] **Kotlin Multiplatform (KMP)** support for iOS and Android
- [ ] Support for older Android versions (API 21+)

## Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Quick Start for Contributors

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Make your changes
4. Write/update tests
5. Commit your changes (`git commit -m 'Add amazing feature'`)
6. Push to the branch (`git push origin feature/amazing-feature`)
7. Open a Pull Request

## Issues and Support

- **Bug Reports**: [Open an issue](https://github.com/IrynaTsymbaliuk/AutoTracker/issues/new?template=bug_report.md)
- **Feature Requests**: [Open an issue](https://github.com/IrynaTsymbaliuk/AutoTracker/issues/new?template=feature_request.md)
- **Questions**: [Start a discussion](https://github.com/IrynaTsymbaliuk/AutoTracker/discussions)

## License

```
Copyright 2026 Iryna Tsymbaliuk

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Acknowledgments

- Health Connect API by Google
- Android Jetpack libraries
- The open-source community

## Star History

If you find this library useful, please consider giving it a ⭐️ on GitHub!

---

**Made with ❤️ for the Android community**
