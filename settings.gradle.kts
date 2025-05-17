rootProject.name = "frontend-compose"

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

include(":shared")
//include(":desktop")
//include(":android")
include(":ios")

//
// xx Where we are going
//
// settings.gradle.kts
// ├── :shared         ← KMP, owns ALL logic + shared resources
// ├── :androidApp     ← Android + Compose
// ├── :desktopApp     ← Compose-Desktop JVM
// └── :iosApp         ← dumb wrapper Xcode project (Gradle only builds KMP framework)
//
//
// xx Inside shared/ only:
//
// src/commonMain/kotlin/**         <— every multiplatform class
// src/commonMain/resources/**      <— earth.webp, strings, etc.
// src/androidMain/kotlin/**
// src/desktopMain/kotlin/**
// src/iosMain/kotlin/**