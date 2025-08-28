import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.compose.reload.ComposeHotRun

plugins {
    // Kotlin
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)

    // Compose
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.composeHotReload)

    // Android
    alias(libs.plugins.androidApp)
//    alias(libs.plugins.jetbrainsCompose)      // brings compose.* aliases
}

kotlin {
    // iosMain (and the other aggregate Apple sets such as iosTest, darwinMain, …) are created only when hierarchical source-sets are enabled.
    // Since Kotlin 2.x this is opt-in – if you don’t ask for a hierarchy you just get the “leaf” sets (iosX64Main, iosArm64Main, iosSimulatorArm64Main, …) and nothing in the middle
    applyDefaultHierarchyTemplate()

    androidTarget()
    jvm("desktop")  // ⇐ generate :shared:run & :shared:desktop* tasks
//    iosX64(); iosArm64(); iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.materialIconsExtended)
                implementation(compose.components.resources)
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
                // JetBrains Compose artifacts ONLY, supplied by the plugin:
                implementation(compose.desktop.currentOs)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)

                // Tooling preview for desktop – use JetBrains version (instead of androidx) :
                implementation(compose.uiTooling)
//                implementation(libs.ui.tooling.preview.desktop) ? todo: remove, after checking what this was for w/ o3

                implementation(libs.vlcj)
                implementation(libs.coroutines.core)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.server.cio)
                implementation(libs.jnativehook)
                implementation(libs.kotlinx.coroutines.swing)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.activity.ktx)
                implementation(project.dependencies.platform(libs.compose.bom))  // AndroidX BOM
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

//        val iosMain by getting {
//            dependencies {
//                implementation(compose.runtime)
//                implementation(libs.interop)
//                implementation(libs.ktor.client.darwin)
//            }
//        }
    }

    compilerOptions { // help suppress unnecessary beta warnings on expect/actual s
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    namespace = "net.sdfgsdfg"
    compileSdk = 35

    defaultConfig {
        applicationId = "net.sdfgsdfg.agi-t"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    buildFeatures { compose = true }

    @Suppress("UnstableApiUsage")
    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
}

//gradle.projectsEvaluated {
//    tasks.findByName("desktopRunHot")?.let {
//        (it as org.jetbrains.compose.reload.gradle.ComposeHotRun).mainClass.set("net.sdfgsdfg.MainKt")
//    }
//}

tasks.withType<org.jetbrains.compose.reload.gradle.ComposeHotRun>().configureEach {
    mainClass.set("net.sdfgsdfg.MainKt") // hotreload still requires this (or CLI arg as -PmainClass=net.sdfgsdfg.MainKt )
    args("--auto") // todo: duplicate with other one below, remove one

    dependsOn(":macosCaptureNative:assemble") // todo: also copy .dylib to shared/src/commonMain/composeResources/
//    dependsOn(":macosCaptureNative:linkRelease")   //  todo: research whether these will be needed for crossArch of platfrms
//    dependsOn(":macosCaptureNative:linkDebug")
//    dependsOn(":macosCaptureNative:linkDebugSharedArm64")
//    dependsOn(":macosCaptureNative:linkReleaseSharedArm64")
//    jvmArgs("-Djava.library.path=${nativeLibDir.get().asFile.absolutePath}")
}

/* -------------------------------------------------------- */
/* 1.  Tell desktopRun where the dylib lives                */
/* -------------------------------------------------------- */
val nativeLibDir = project(":macosCaptureNative")
    .layout.buildDirectory
    .dir("lib/main/debug/shared/arm64")

compose.desktop {
    application {
        mainClass = "net.sdfgsdfg.MainKt"

        jvmArgs += listOf(
//            "-verbose:jni",  // verbose JNI logs for debugging JNI bridges
//
//            "-Djava.awt.headless=true", // needed for Skia // xx why tf did I or AI even add this
//            "-Dcompose.animation.interop.blending=true", // needed for Skia
//            "/Users/x/Desktop/kotlin/frontend-compose/macosCaptureNative/build/lib/main/debug/arm64/libDesktopCapture.dylib",
        )
        args += listOf("--auto") // Compose Hot reload auto-detect changes

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb
            )
            packageVersion = "1.0.0"
            modules("jdk.unsupported")

            macOS {
                bundleID = "net.sdfgsdfg"
                mainClass = "net.sdfgsdfg.MainKt"

                infoPlist {                  // inject TCC usage string
                    extraKeysRawXml = """
                        <key>NSInputMonitoringUsageDescription</key>
                        <string>Needed to listen for the Ctrl-Space global shortcut.</string>
                        <key>NSAppleEventsUsageDescription</key>
                        <string>Needed to reposition the mouse to the Arcana window.</string>
                    """.trimIndent()
                }
            }
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "net.sdfgsdfg.resources"
    generateResClass = auto
}

dependencies { // Very global dependencies to be used rarely, for debug tooling etc...
    debugImplementation(compose.uiTooling)
}
