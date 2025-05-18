package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf

actual class PlatformContext // TODO: Replace with UIKit-specific handle when you need one

actual val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext> =
    staticCompositionLocalOf { net.sdfgsdfg.platform.PlatformContext() }

@Composable
actual fun DrawableResource.toPlayablePath(): String {
    TODO("Not yet implemented")
}