# Gradle Plugin for Flashcat Android SDK

> This plugin is used to upload your Proguard/Dexguard/R8 mapping files and NDK symbol files to Flashcat to get a complete RUM Error Tracking experience.

Once your mapping files are uploaded, any error/crash reported through RUM will have a deobfuscated/symbolicated stacktrace, enabling an easy diagnosis of the root cause of the error.

## 📌 Important Notice

This project is a fork of [Datadog's dd-sdk-android-gradle-plugin](https://github.com/DataDog/dd-sdk-android-gradle-plugin), modified and adapted for the Flashcat platform.

**Original Project**: [DataDog/dd-sdk-android-gradle-plugin](https://github.com/DataDog/dd-sdk-android-gradle-plugin)  
**License**: Apache License 2.0  
**Copyright**: Original work © 2019-2024 Datadog, Inc. | Modifications © 2025 Flashcat, Inc.

All modifications maintain compliance with the Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE) files for details.

---

## Getting Started

### Setup

Add the following line to your `build.gradle` file.

```groovy
plugins {
    id("cloud.flashcat.android-gradle-plugin") version "1.1.0"
}
```

或在 Kotlin DSL 中：

```kotlin
plugins {
    id("cloud.flashcat.android-gradle-plugin") version "1.1.0"
}
```

### Uploading

To upload your mapping files to Flashcat, run the `uploadMapping[Variant]` task in your Android application project as part of your build or after your build, for example:

```bash
./gradlew uploadMappingRelease
```

Similarly, to upload NDK symbols, run the `uploadNdkSymbolFiles[Variant]` task in your Android application project. For example:

```bash
./gradlew uploadNdkSymbolFilesRelease
```

### Configuration

You can configure the plugin by adding the following block at the end of your `build.gradle` file.

```groovy
flashcat {
    versionName = "1.3.0" // Optional, by default it is read from your Android plugin configuration's version name
    serviceName = "my-service" // Optional, by default it is read from your Android plugin configuration's package name
    site = "CN" // Optional, can be "CN" or "STAGING" (check `FlashcatSite` documentation for the full list). Default is "CN"
    checkProjectDependencies = "warn" // Optional, can be "warn", "fail" or "none". Default is "fail". Will check if Flashcat SDK is in the project dependencies.
    mappingFilePath = "path/to/mapping.txt" // Optional, provides a custom mapping file path. Default is "build/outputs/mapping/{variant}/mapping.txt".
    nonDefaultObfuscation = false // Optional, to be used if a 3rd-party obfuscation tool is used. Default is false.
    ignoreFlashcatCiFileConfig = false // Optional, ignore configuration provided in `flashcat-ci.json` file if found. Default is false.
    additionalSymbolFilesLocations = ["/path/to/location/obj"] // Optional, additional locations the Gradle plugin will check for `.so` files during `uploadNdkSymbolFiles` task. Default is none.
}
```

If you're using variants, you can set a custom configuration per variant using the following syntax.

#### Groovy

```groovy
flashcat {
    site = "CN" // Variants with no configurations will use this as default
    variants {
        fr {
            site = "STAGING"
            mappingFilePath = "path/to/fr/mapping.txt"
        }
    }
}
```

#### Kotlin Script

```kotlin
flashcat {
    site = "CN" // Variants with no configurations will use this as default
    variants {
        register("fr") {
            site = "STAGING"
            mappingFilePath = "path/to/fr/mapping.txt"
        }
    }
}
```

### Gradle configuration cache support

This plugin supports [Gradle configuration cache](https://docs.gradle.org/7.1/userguide/configuration_cache.html) starting from the version `1.1.0`, but to have this support you need to disable SDK dependency check by setting `checkProjectDependencies` to `none`:

```groovy
flashcat {
    ...
    checkProjectDependencies = "none"
    ...
}
```

### Environment Variables

You can also configure the plugin using environment variables:

```bash
# API Key (required)
export FC_API_KEY="your-flashcat-api-key"
# or
export FLASHCAT_API_KEY="your-flashcat-api-key"

# Site (optional)
export FLASHCAT_SITE="ci.flashcat.cloud"
```

### Configuration File (flashcat-ci.json)

You can also use a `flashcat-ci.json` file in your project root for configuration:

```json
{
  "apiKey": "your-flashcat-api-key",
  "flashcatSite": "ci.flashcat.cloud"
}
```

---

## Available Sites

The plugin supports the following Flashcat sites:

- **CN**: `ci.flashcat.cloud` (default)
- **STAGING**: `ci-dev.flashcat.cloud` (internal usage only)

---

## Troubleshooting

If you encounter any issue when using the Gradle Plugin for Flashcat Android SDK, please contact Flashcat support.

---

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)

---

## License

This project is licensed under the Apache License, Version 2.0 - see the [LICENSE](LICENSE) file for details.

This is a derivative work of Datadog's dd-sdk-android-gradle-plugin. See [NOTICE](NOTICE) for attribution details.
