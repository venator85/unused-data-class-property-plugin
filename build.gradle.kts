import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = "com.example"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // ponytail: dev su IntelliJ IDEA Community; il plugin gira anche in Android Studio
        // (stessa piattaforma). Per targettizzare AS: androidStudio("2024.2.1.11").
        intellijIdeaCommunity("2024.2.5")
        bundledPlugin("org.jetbrains.kotlin")
        testFramework(TestFrameworkType.Platform)
    }
    testImplementation("junit:junit:4.13.2")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "242"
            untilBuild = "999.*" // ponytail: nessun bound superiore, alziamo se una release rompe l'API
        }
    }
}

kotlin {
    jvmToolchain(21)
}
