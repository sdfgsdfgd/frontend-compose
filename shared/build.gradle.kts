import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.androidApp)
    alias(libs.plugins.jetbrainsCompose)      // brings compose.* aliases
}

kotlin {
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
            }
        }
        val desktopMain by getting {
            dependencies {
                implementation(libs.ui.tooling.preview.desktop)
                implementation(compose.desktop.currentOs)
                implementation(libs.vlcj)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.activity.ktx)
                implementation(project.dependencies.platform(libs.compose.bom))
                implementation(libs.androidx.activity.compose)
            }
        }
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
    namespace    = "net.sdfgsdfg"
    compileSdk   = 35
    defaultConfig {
        applicationId = "net.sdfgsdfg"
        minSdk  = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }
    buildFeatures { compose = true }
    experimentalProperties["android.experimental.kmp.enableAndroidResources"] = true
}

/* xx Errors out, because jvm needs to be standalone ?? find out why this fix required
    (o3 suggested this) https://chatgpt.com/c/68281a29-d068-800c-b9ad-f78eb989d5c0  ---------- */
gradle.projectsEvaluated {
    tasks.findByName("desktopRun")                // <-- now it exists
        ?.let { (it as JavaExec).mainClass.set("net.sdfgsdfg.MainKt") }
}

compose.desktop {
    application {
        mainClass = "net.sdfgsdfg.MainKt"
        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb
            )
            packageName = "net.sdfgsdfg"
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