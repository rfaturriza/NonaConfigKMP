# Nona Config SDK
[![Maven Central](https://img.shields.io/maven-central/v/io.github.rfaturriza/nona-config.svg)](https://central.sonatype.com/artifact/io.github.rfaturriza/nona-config)
[![GitHub Release](https://img.shields.io/github/v/release/rfaturriza/NonaConfigKMP.svg)](https://github.com/rfaturriza/NonaConfigKMP/releases)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg)](https://kotlinlang.org/docs/multiplatform.html)
[![Swift Package Manager](https://img.shields.io/badge/SPM-compatible-orange.svg)](https://swift.org/package-manager/)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE.md)
[![codecov](https://codecov.io/gh/rfaturriza/NonaConfigKMP/branch/main/graph/badge.svg)](https://codecov.io/gh/rfaturriza/NonaConfigKMP)

A Kotlin Multiplatform SDK for Nona Config, replicating core features of Firebase Remote Config: defaults, fetching, caching, and activation.

## Features
- **In-app Defaults**: Local hardcoded default values.
- **Fetching**: Retrieve updated values from the cloud.
- **Caching**: Local persistence and request throttling.
- **Activation**: Decoupled fetch and apply logic for seamless UI updates.

## Installation

### Kotlin Multiplatform / Android
Add the dependency directly to your `build.gradle.kts` (available on Maven Central, no extra repository or credentials required):

```kotlin
dependencies {
    implementation("io.github.rfaturriza:nona-config:1.0.2")
}
```

### iOS (Swift Package Manager)

#### Option A: Xcode UI
1. In Xcode, select **File > Add Package Dependencies...**
2. Enter the repository URL: `https://github.com/rfaturriza/NonaConfigKMP`
3. Set the **Dependency Rule** to **Up to Next Major Version** starting from `1.0.2`.
4. Add `NonaConfig` to your app target.

#### Option B: `Package.swift`
If your project uses a `Package.swift` manifest, add the dependency to your `Package` definition:

```swift
// Package.swift
let package = Package(
    name: "MyApp",
    dependencies: [
        .package(url: "https://github.com/rfaturriza/NonaConfigKMP", from: "1.0.2")
    ],
    targets: [
        .target(
            name: "MyApp",
            dependencies: [
                .product(name: "NonaConfig", package: "NonaConfigKMP")
            ]
        )
    ]
)
```

## Usage

### 1. Basic Initialization

```kotlin
val nonaConfig = NonaConfig.instance
nonaConfig.initialize(
    apiKey = "your-api-key",
    environmentId = "production",
    baseUrl = "https://your-config-server.com"
)
```

### 2. Setting Defaults

```kotlin
nonaConfig.setDefaults(mapOf(
    "welcome_message" to "Hello!",
    "feature_enabled" to true
))
```

### 3. Fetching and Activating

#### Coroutine (Suspend) or Callback-based
```kotlin
// Option A: Callback-based non-suspend function (ideal for UI & non-coroutine callers)
nonaConfig.fetchAndActivate { success ->
    if (success) {
        val message = nonaConfig.getString("welcome_message")
    }
}

// Option B: Coroutine suspend function
coroutineScope.launch {
    val success = nonaConfig.fetchAndActivate()
}
```

#### Status-Aware Fetching & ETag Management
You can inspect the detailed fetch status (e.g. distinguishing `200 OK` from `304 Not Modified`), read the current ETag, or clear it to force a fresh fetch:

```kotlin
// Fetch with explicit status result
nonaConfig.fetchAndActivateWithStatus { status ->
    when (status) {
        NonaConfig.FetchStatus.SUCCESS_NEW_DATA -> println("HTTP 200 OK - Downloaded new payload")
        NonaConfig.FetchStatus.SUCCESS_NOT_MODIFIED -> println("HTTP 304 - ETag matched, using cached config")
        NonaConfig.FetchStatus.THROTTLED -> println("Fetch throttled by minimum fetch interval")
        NonaConfig.FetchStatus.ERROR -> println("Fetch failed")
    }
}

// Inspect active ETag
val currentETag: String? = nonaConfig.lastETag

// Clear ETag to force unconditional fetch on next request (omits If-None-Match)
nonaConfig.clearETag()
```

### 4. Retrieving Values

```kotlin
val message = nonaConfig.getString("welcome_message")
val isEnabled = nonaConfig.getBoolean("feature_enabled")

// Advanced: Parsing JSON values directly
val theme = nonaConfig.getValue("theme_settings").asJson(ThemeConfig.serializer())
```

### 5. Advanced Settings

```kotlin
val settings = NonaConfigSettings.Builder()
    .setBaseUrl("https://your-custom-backend.com") // Optional: Custom server URL
    .setMinimumFetchInterval(1.hours)
    .setReleaseVersion("1.1.x") // Pin to a specific release line
    .build()

nonaConfig.setConfigSettings(settings)
```

## Security Best Practices

### Android
Do not hardcode your API key. Use the **Secrets Gradle Plugin** to inject it from `local.properties`.

**local.properties** (Git ignored)
```properties
NONA_API_KEY=your_actual_key
```

**build.gradle.kts**
```kotlin
plugins {
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}
```

**MainActivity.kt**
```kotlin
NonaConfig.instance.initialize(
    apiKey = BuildConfig.NONA_API_KEY,
    environmentId = "production",
    baseUrl = "https://your-config-server.com"
)
```

### iOS (Swift)
Do not hardcode your API key. Use a Git-ignored `Secrets.xcconfig` file included in `Config.xcconfig`.

**Secrets.xcconfig** (Git ignored)
```properties
NONA_API_KEY=your_actual_key
```

**Config.xcconfig** (Tracked in Git)
```properties
#include? "Secrets.xcconfig"

TEAM_ID=
PRODUCT_NAME=NonaConfigKMP
PRODUCT_BUNDLE_IDENTIFIER=com.nonaconfig.NonaConfigKMP$(TEAM_ID)
```

**Info.plist**
```xml
<key>NONA_API_KEY</key>
<string>$(NONA_API_KEY)</string>
```

**Swift Initialization**
```swift
import NonaConfig // The name of your framework

let client = NonaConfigClient.companion.instance

// Securely getting the key from Info.plist or Environment
let apiKey = (Bundle.main.object(forInfoDictionaryKey: "NONA_API_KEY") as? String)
    .flatMap { $0.isEmpty ? nil : $0 }
    ?? ProcessInfo.processInfo.environment["NONA_API_KEY"]
    ?? ""

client.initialize(apiKey: apiKey, environmentId: "production", baseUrl: "https://your-config-server.com")
```

**Swift Usage**
```swift
// Setting Defaults
client.setDefaults(defaults: ["welcome_message": "Hello Swift!"])

// Fetch and Activate (Async/Await)
Task {
    do {
        let success = try await client.fetchAndActivate()
        if success {
            let message = client.getString(key: "welcome_message")
            print(message)
        }
    } catch {
        print("Fetch failed: \(error)")
    }
}
```

## Distribution

### Android (AAR)
The library is configured for Maven publication. To publish to a local repository for testing:
```bash
./gradlew publishToMavenLocal
```

### iOS (Swift Package Manager)
Releases automatically publish binary XCFramework artifacts and update `Package.swift` via KMMBridge during CI.

To test building the XCFramework locally:
```bash
./gradlew :sharedLogic:assembleXCFramework
```

## License
Apache License 2.0
