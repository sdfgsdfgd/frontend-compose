// shared/src/commonMain/kotlin/net/sdfgsdfg/PlatformContext.kt
package net.sdfgsdfg.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import org.jetbrains.compose.resources.DrawableResource

/**
 * A minimal handle to whatever “platform context” each target needs.
 * Plain class → easiest to provide real actuals everywhere.
 */
expect class PlatformContext

/** Ambient access (only if you really need it in Composables). */
expect val LocalPlatformContext: ProvidableCompositionLocal<PlatformContext>


/** Turn a bundled resource into a URI/Path the native media engine can play. */
@Composable
expect fun DrawableResource.toPlayablePath(): String