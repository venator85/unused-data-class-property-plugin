# Unused Data Class Property

An IntelliJ Platform inspection that highlights Kotlin `data class` primary-constructor properties with no project references. Reads count as usage for `val`; reads or writes count for `var`. References inside `KSerializer` implementations are ignored, so properties used only for serialization can still be reported.

## Requirements

- IntelliJ IDEA 2024.2 or later, or a compatible Android Studio version, with the Kotlin plugin and K2 mode enabled.
- JDK 21 to build the plugin from source. The Gradle wrapper downloads Gradle automatically.

## Installation

1. Build the plugin ZIP from the project root:

   ```sh
   ./gradlew buildPlugin
   ```

   On Windows, run `gradlew.bat buildPlugin` instead. The ZIP is written to `build/distributions/unused-data-class-property-plugin-0.1.0.zip`.

2. In the IDE, open **Settings/Preferences → Plugins**, select the gear icon, and choose **Install Plugin from Disk…**.
3. Select the ZIP file, then restart the IDE when prompted.

The inspection is enabled by default under **Settings/Preferences → Editor → Inspections → Kotlin → Unused data class property**. It marks the name of each unused property with a warning.

To run the tests from the project root, use `./gradlew test` (or `gradlew.bat test` on Windows).
