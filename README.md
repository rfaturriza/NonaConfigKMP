# Nona Config SDK

A Kotlin Multiplatform SDK for Nona Config, replicating core features of Firebase Remote Config: defaults, fetching, caching, and activation.

## Features
- **In-app Defaults**: Local hardcoded default values.
- **Fetching**: Retrieve updated values from the cloud.
- **Caching**: Local persistence and request throttling.
- **Activation**: Decoupled fetch and apply logic for seamless UI updates.

## Installation

### Kotlin Multiplatform / Android
Add the dependency to your `build.gradle.kts` (via JitPack for now):

```kotlin
repositories {
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.ryware:nona-config:1.0.0")
}
```

## Usage

### 1. Basic Initialization

```kotlin
val nonaConfig = NonaConfig.instance
nonaConfig.initialize(apiKey = "your-api-key", environmentId = "production")
```

### 2. Setting Defaults

```kotlin
nonaConfig.setDefaults(mapOf(
    "welcome_message" to "Hello!",
    "feature_enabled" to true
))
```

### 3. Fetching and Activating

```kotlin
coroutineScope.launch {
    // Fetch from remote and activate locally
    // Uses ETags for efficient fetching (only downloads if changed)
    nonaConfig.fetchAndActivate()
}
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
NonaConfig.instance.initialize(BuildConfig.NONA_API_KEY, "production")
```

### iOS
Use Environment Variables or `.xcconfig` files to handle secrets.

**ContentView.swift**
```swift
let apiKey = ProcessInfo.processInfo.environment["NONA_API_KEY"] ?? "default-key"
NonaConfig.instance.initialize(apiKey: apiKey, environmentId: "production")
```

## Distribution

### Android (AAR)
The library is configured for Maven publication. To publish to a local repository for testing:
```bash
./gradlew publishToMavenLocal
```

### iOS (Swift Package Manager)
The library provides a `Package.swift` for SPM integration.
1. Build the XCFramework:
   ```bash
   ./gradlew :sharedLogic:assembleXCFramework
   ```
2. In Xcode, go to **File > Add Packages...** and point to this repository.

## License
Apache License 2.0
