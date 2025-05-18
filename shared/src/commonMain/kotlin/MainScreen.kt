// shared/src/commonMain/kotlin/net/sdfgsdfg/MainScreen.kt
package net.sdfgsdfg

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import net.sdfgsdfg.platform.WindowMetrics
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

/**
 * All formerly-desktop-only UI now lives here.
 *
 * The only things injected from the platform are:
 *   • [video] – the actual video composable
 *   • [metrics] – current window/screen size
 */
@Composable
fun MainScreen(
    metrics : WindowMetrics,
    video   : @Composable () -> Unit,
) {
    // ------- debugging identical to the old App.kt ----------
    println(
        """
        Desktop size                 : ${metrics.pixels}
        desktopWidth / Height (dp)   : ${metrics.sizeDp}
        density                      : ${metrics.density}
        ----------------------------------------------
        """.trimIndent()
    )
    //----------------------------------------------------------

    val videoHeight = 500.dp
    val videoTopY   = max(0.dp, metrics.sizeDp.height - videoHeight)

    var showContent  by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {

            /*  top gradient strip  --------------------------------- */
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(videoTopY)
                    .topGradientOverlay(videoTopY, LocalDensity.current)
            )

            /*  video area supplied by the platform  ---------------- */
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .innerShadow()
                    .align(Alignment.BottomCenter)
            ) { video() }

            /*  overlay controls  ----------------------------------- */
            Box(Modifier.matchParentSize(), contentAlignment = Alignment.TopCenter) {
                Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {

                    Button(onClick = { showContent = !showContent }) { Text("Click me!") }

                    AnimatedVisibility(
                        visible = showContent,
                        enter   = fadeIn() + expandVertically()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painterResource(Res.drawable.compose_multiplatform),
                                contentDescription = null
                            )
                            Text(
                                "placeholder",
                                color      = Color.DarkGray,
                                fontSize   = 48.sp,
                                fontFamily = FontFamily.Cursive
                            )
                        }
                    }

                    Button(onClick = { showSettings = !showSettings }) { Text("Settings") }
                }
            }
        }
    }
}