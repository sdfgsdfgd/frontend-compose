/* ────────────────────────────────────────────────────────── */
/*  macosCaptureNative/build.gradle.kts                       */
/* ────────────────────────────────────────────────────────── */
// xx The huge  LiquidGlassMP & JNI questchain :
//   ( 1 ) ( Done )   The Initial Architectural Goose Chase:   https://chatgpt.com/g/g-3X6EMarap-x5/c/687a25d2-9f50-832a-8f52-4c94e5d02edd?model=gpt-4-5
//   ( 2 ) ( Done )   JNI Goosechase:                          https://chatgpt.com/g/g-3X6EMarap-x5/c/68842f4e-9d3c-832d-a4af-44693c4cbb36?model=gpt-4-5
//   ( 3 ) (  WIP )   SCK --> Metal+Skia Goosechase:           https://chatgpt.com/c/68842bf0-c938-8329-8c66-09409bac1bab?model=gpt-4-5
repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    maven("https://packages.jetbrains.team/maven/p/skija/maven")
}

plugins {
    id("cpp-library")
}

// ──[ 0. Context ]──────────────────────────────────────────────────────────────────────────────────────
val skikoVersion = "0.9.4.2"
val hostArch = if (System.getProperty("os.arch").contains("aarch")) "macos-arm64" else "macos-x64"
val skikoHeaders: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val skikoNative: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
val javaHome: String = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
val skiaRootDir = project.rootDir.resolve("skia-headers")
val skiaHeadersDir = project.rootDir.resolve("skia-headers/include")
val unpackSkikoNative by tasks.registering(Copy::class) {
    dependsOn(skikoNative)

    // all jars in the configuration
    val jars = skikoNative.incoming.artifactView { }.files
    from(jars.map { zipTree(it) }) {
        include("**/META-INF/native/$hostArch/libskiko-$hostArch.dylib")
        include("**/META-INF/native/$hostArch/libskia-$hostArch.dylib")
        include("**/libskiko-$hostArch.dylib")
        include("**/libskia-$hostArch.dylib")
        eachFile {           // flatten the path, keep original filename
            name = name.substringAfterLast('/')
            // eachFile { relativePath = RelativePath(true, *relativePath.segments.dropWhile { it != "include" }.toTypedArray()) }
        }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("skikoNative"))
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE   // safety net
}

val macosSdk: Provider<String> = providers
    .exec { commandLine("xcrun", "--sdk", "macosx", "--show-sdk-path") }
    .standardOutput.asText
    .map(String::trim) // strip the trailing newline
// ──[ - ]──────────────────────────────────────────────────────────────────────────────────────

// xx: using cloned headers from skia repo itself, remove runtimes, no longer necessary ?
//   ( jars were for decompiling for headers, they turned out not to be there, as mostly expected )
dependencies { //noinspection UseTomlInstead
    skikoHeaders("org.jetbrains.skiko:skiko-awt-runtime-$hostArch:$skikoVersion@jar")
    skikoHeaders("org.jetbrains.skija:skija-shared:0.93.6@jar")
    skikoNative("org.jetbrains.skiko:skiko-awt-runtime-$hostArch:$skikoVersion")
}

/* ───── 1.  Basic dylib skeleton ─────────────────────────── */
library {
    baseName.set("DesktopCapture") // → libDesktopCapture.dylib
    linkage = listOf(Linkage.SHARED) // Linkage.STATIC,
    targetMachines.set(
        listOf(
            machines.macOS.architecture("arm64"),
            machines.macOS.x86_64
        )
    )

    // source.setFrom("src/main/objectiveCpp/iosurface_capture.mm")
    source.from(
        // fileTree("src/main/cpp") {
        //     include("**/*.mm", "src/main/cpp/*.mm", "**/*.cpp")
        // },
        fileTree("src/main/objectiveCpp") {
            include("**/*.mm", "src/main/objectiveCpp/*.mm", "**/*.cpp")
        }
    )

    publicHeaders.from(fileTree("src/main/headers") {
        include("**/*.h")
    })
}

// ─── helper that sets the full arg list ─────────────────────────────
fun CppCompile.applyAppleSDK(
    javaHome: String,
    skiaHeadersDir: File,
    skiaRootDir: File
) {
    val sdk: Provider<String> = project.providers.exec {
        commandLine("xcrun", "--sdk", "macosx", "--show-sdk-path")
    }.standardOutput.asText.map(String::trim)

    compilerArgs.set(
        sdk.map { s ->
            listOf(
                "-x", "objective-c++", "-std=c++17", "-fobjc-arc",

                "-mmacosx-version-min=14.0",

                "-isysroot", s,
                "-F", "$s/System/Library/Frameworks",
                "-iframework", "$s/System/Library/Frameworks",

                "-DSK_METAL",
                "-I$javaHome/include", "-I$javaHome/include/darwin",
                "-I${skiaHeadersDir.absolutePath}", "-I${skiaRootDir.absolutePath}",
                "-I${skiaHeadersDir}/modules",
                "-I${skiaHeadersDir.absolutePath}/third_party",
            )
        }
    )
}

tasks.withType<CppCompile>().configureEach {
    source.from(project.fileTree("src/main/objectiveCpp") { include("**/*.mm") })

    //
    //
    applyAppleSDK(javaHome, skiaHeadersDir, skiaRootDir)
    //
    //
}

@Suppress("UnstableApiUsage")
tasks.withType<ObjectiveCppCompile>().configureEach {
    compilerArgs.addAll(
        "-std=c++17", "-fobjc-arc",
        "-I$javaHome/include", "-I$javaHome/include/darwin",
        "-I${skiaHeadersDir.absolutePath}",
        "-I${skiaRootDir.absolutePath}"
    )
}

tasks.withType<LinkSharedLibrary>().configureEach {
    dependsOn(unpackSkikoNative)

    val libDir = layout.buildDirectory.dir("skikoNative")

    // link Metal + CoreGraphics (+ QuartzCore for CAMetalLayer if needed)
    linkerArgs.addAll(
        "-framework", "ScreenCaptureKit",
        "-framework", "Metal",
        "-framework", "CoreGraphics",
        "-framework", "QuartzCore",
        "-framework", "CoreVideo",
        "-framework", "IOSurface",
        "-framework", "Foundation",

        "-L${libDir.get().asFile.absolutePath}",
        "-lskiko-$hostArch",      // e.g. libskiko-macos-arm64.dylib
        // 👇 allow unresolved symbols; they’ll be fixed up at runtime xx         [ Symbol Interposition ]
        //                                                               ( technique of substituting syms at runtime )
        //                                                              ( commonly called  `Runtime Symbol Resolution`  )
        //                                                                     (  `Runtime Dynamic Linking`  )
        // lets symbols dangle until libskiko-macos-arm64.dylib is loaded at runtime.
        //  ( lazy dynamic linking / lazy runtime dynamic symbol resolution )
        //  Unresolved symbols stay "dangling" until runtime,
        //     then macOS’s dynamic linker (dyld) resolves them from already-loaded dylibs in your JVM process
        //     (like libskiko-macos-arm64.dylib).
        "-undefined", "dynamic_lookup"
    )

    doLast {
        println("► ")
        println("► ")
        println("► ")
        println("► ")
        println("► ")
        println("► [ Kaan - dylib ] File → " + linkedFile.get().asFile)
        println("► [ Kaan - dylib ] File Absolute Path → ${linkedFile.get().asFile.absolutePath}")
        println("► ")
        println("► ")
        println("► ")
        println("► ")
        println("► ")
    }
}


//
//
//
// TO BE DELETED
//
//
//
// TODO    Try one last time getting this to work with runtime dep or it's variants, maybe one of them have the headers
//            - ( better than keeping and maintaining a Skia/ clone locally )
//            - if no hope, remove
//
//      val extractSkiaHeaders by tasks.registering(Copy::class) {
//          val jars = skikoHeaders.incoming.artifactView { }.files
//          from(jars.map { zipTree(it) }) {
//              include("**/include/**")
//              includeEmptyDirs = false
//          }
//          into(skiaHeadersDir)
//          outputs.dir(skiaHeadersDir)
//      }
//
