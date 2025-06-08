import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidApp)
    alias(libs.plugins.jetbrainsCompose)      // brings compose.* aliases
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeHotReload)
}

kotlin {
    // iosMain (and the other aggregate Apple sets such as iosTest, darwinMain, …) are created only when hierarchical source-sets are enabled.
    // Since Kotlin 2.x this is opt-in – if you don’t ask for a hierarchy you just get the “leaf” sets (iosX64Main, iosArm64Main, iosSimulatorArm64Main, …) and nothing in the middle
    applyDefaultHierarchyTemplate()

    androidTarget()
    jvm("desktop")
    iosX64(); iosArm64(); iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.components.resources)
//                implementation(libs.ui.tooling.preview.android)
//                implementation(libs.androidx.constraintlayout)
                /// Compose 1.7.3
                implementation(libs.constraintlayout.compose.multiplatform)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.logging)
                implementation(libs.slf4j.simple)
                implementation(libs.androidx.datastore.preferences)
                implementation(libs.okio)
                implementation(libs.kotlinx.datetime)
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ui.tooling.preview.desktop)
                implementation(compose.desktop.currentOs)
                implementation(libs.vlcj)
                implementation(libs.coroutines.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.server.cio)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.activity.ktx)
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation(libs.androidx.activity.compose)

                // ▶ ExoPlayer / Media3  ( vid )
                implementation(libs.androidx.media3.exoplayer)
                implementation(libs.media3.exoplayer)
                implementation(libs.androidx.media3.ui)
                implementation(libs.material)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.androidx.browser)
            }
        }

        val iosMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(libs.interop)
                implementation(libs.ktor.client.darwin)
            }
        }
    }

    compilerOptions { // this helps suppress some unnecessary beta warnings on actual impls of iOS target and others
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    // xx https://www.jetbrains.com/help/kotlin-multiplatform-dev/compose-multiplatform-resources-setup.html#resources-in-the-androidlibrary-target
//    androidLibrary {
//        // Starting with the Android Gradle plugin version 8.8.0,
//        // you can use the generated Res class and resource accessors in the androidLibrary target.
//        experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
//
//        namespace = "net.sdfgsdfg.kaangpt"
//        compileSdk = 35
//    }
}

android {
    namespace           = "net.sdfgsdfg"
    compileSdk          = 35
    defaultConfig {
        applicationId   = "net.sdfgsdfg"
        minSdk          = 24
        targetSdk       = 35
        versionCode     = 1
        versionName     = "0.1"
    }
    buildFeatures { compose = true }
    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
}

/* xx  ------- (o3 suggested this) https://chatgpt.com/c/68281a29-d068-800c-b9ad-f78eb989d5c0  ---------- */
gradle.projectsEvaluated {
    tasks.findByName("desktopRun")
        ?.let { (it as JavaExec).mainClass.set("MainKt") }
}

compose.desktop {
    application {
        mainClass = "mainKt"
        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb
            )
            packageVersion = "1.0.0"
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "net.sdfgsdfg.resources"
    generateResClass = auto
}

// Very global dependencies to be used rarely, for debug tooling etc...
dependencies {
    debugImplementation(compose.uiTooling)
}