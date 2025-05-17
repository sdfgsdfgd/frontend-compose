import com.strumenta.antlrkotlin.gradle.AntlrKotlinTask
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.antlr.plugin)
    alias(libs.plugins.tree.sitter.kotlin)
}

kotlin {
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting {
            kotlin.srcDir("build/generated/src/antlr/main")
            kotlin.srcDir("generatedAntlr")
        }

        // ANTLR
//        val main by getting
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(projects.shared)
            implementation(libs.antlr.core)
        }

        commonMain {// ANTLR
            kotlin {
                srcDir(layout.buildDirectory.dir("generatedAntlr"))
            }
        } // ANTLR

        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("uk.co.caprica:vlcj:4.8.3") // switch if experimental JB VideoPlayer released
            implementation(libs.coil.compose)
            implementation(libs.coil.gif)
            implementation("io.github.tree-sitter:ktreesitter:0.24.0") // version "0.24.0"
        }
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(
                TargetFormat.Dmg,
                TargetFormat.Msi,
                TargetFormat.Deb)
            packageName = "net.sdfgsdfg.kaangpt"
            packageVersion = "1.0.0"
        }
    }
}

// region ANTLR
tasks.register<AntlrKotlinTask>("generateKotlinGrammarSource") {
    // Grammar files
    source = fileTree("/Users/x/Desktop/kotlin/kaangpt_kotlin/composeApp/src/desktopMain/kotlin/antlr/") {
        include("**/*.g4")
    }

    // Output directory
    val outputDir = layout.buildDirectory.dir("generatedAntlr")
    outputDirectory = outputDir.get().asFile
    doFirst {
        outputDir.get().asFile.mkdirs() // Ensure directory is created
    }

    arguments = listOf("-visitor", "-listener", "-package", "net.sdfgsdfg.kaangpt")
    packageName = "net.sdfgsdfg.kaangpt"
}

tasks.named("cleanGenerateKotlinGrammarSource") {
    dependsOn("clean")
}

//tasks.withType(KotlinCompile::class.java) {
//    dependsOn("generateKotlinGrammarSource")
//}

// endregion