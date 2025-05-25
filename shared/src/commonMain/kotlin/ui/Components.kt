package net.sdfgsdfg.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource

// region Buttons & Texts
/**
 * Ultra‑skeuomorphic 3‑D text:
 *   • light highlight top‑left
 *   • dark drop‑shadow bottom‑right
 *   • subtle vertical steel‑like gradient fill
 */
@Composable
fun SkeuoText(
    text: String,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Serif
) {
    val fill = Brush.verticalGradient(
        listOf(
            Color(0xFFFDFDFD),
            Color(0xFFE6E6E6),
            Color(0xFFC8C8C8)
        )
    )
    Box(modifier = modifier.graphicsLayer { clip = false }) {

        // dark soft shadow
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            color = Color.Black.copy(alpha = 0.55f),
            modifier = Modifier
                .offset(2.dp, 2.dp)
                .blur(2.dp)
        )

        // highlight rim
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier
                .offset((-2).dp, (-2).dp)
                .blur(1.5.dp)
        )

        // main body
        Text(
            text = text,
            fontSize = fontSize,
            fontFamily = fontFamily,
            style = TextStyle(
                brush = fill,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.35f),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f
                )
            )
        )
    }
}
// endregion

// region ────[ Timings  ( Actual Magic ) ]────────────────────────────────────────────────────────────
/**
 * Two-stage ramp:
 *   ① 0-knee  : darkA → midA   (super-gentle linear fade)
 *   ② knee-1  : midA  → lightA (eased, fast drop-off)
 */
private fun buildStops(
    totalStops: Int = 1024,
    knee:      Float = 0.66f,   // 22 % of the strip = slow zone
    lightA:    Float = .02f,    // 0.02% opacity practically gone at top
    midA:      Float = .90f,    // ~90 % opacity when we hit the knee
    darkA:     Float = .99f,    // 99 % at very bottom
    tailEase:  Easing = FastOutSlowInEasing
): Array<Pair<Float, Color>> =
    Array(totalStops + 1) { i ->
        val p = i / totalStops.toFloat()          // 0‒1 along strip

        val a = if (p < knee) {
            // Stage-1: *linear* micro-decrement
            val t = p / knee                      // 0‒1 inside slow zone
            darkA + (midA - darkA) * t            // .99 → .60 very slowly
        } else {
            // Stage-2: eased plunge to full transparency
            val t = (p - knee) / (1 - knee)       // 0‒1 inside fast zone
            val e = tailEase.transform(t)
            midA  + (lightA - midA) * e           // .60 → .02 rapidly
        }

        p to Color.Black.copy(alpha = a)
    }
// endregion

// region ────[ Vid Overlays ]────────────────────────────────────────────────────────────

/* ── the fade strip as its own composable / layer ────────────── */
@Composable
fun GapFadeStrip(blurRadius: Dp = 24.dp) {
    val stops = remember {
        buildStops(
            totalStops = 1024,
            darkA   = .995f,
            lightA  = .04f        // just a ghost at the ceiling
        )
    }

    Box(
        Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .graphicsLayer {}                       // isolate for blur
            .drawWithCache {
                val brush = Brush.verticalGradient(
                    colorStops = stops,
                    startY = size.height,           // ⬅  bottom-up
                    endY = 0f
                )
                onDrawBehind { drawRect(brush) }
            }
            .blur(blurRadius)
    )
}



/* ──────────────────────────────────────────────────────────────── */
/*  A) vignette that hugs the video rectangle                      */
/* ──────────────────────────────────────────────────────────────── */
fun Modifier.videoVignette() = drawWithContent {
    drawContent()

    val vignette = Brush.radialGradient(
        0.0f to Color.Transparent,
        1.0f to Color.Black.copy(alpha = .97f),
        center = Offset(size.width / 2, size.height / 2),
        radius = size.maxDimension / 2,
        tileMode = TileMode.Clamp
    )
    drawRect(vignette)
}

// endregion

// region Archive

// Handmade gourmet version
/* ──────────────────────────────────────────────────────────────── */
/*  B) vertical fade for any strip-height you give it              */
/*     • 0-10 %   : opaque black                                   */
/*     • 10-90 %  : smooth fall-off 80 → 10 % alpha                */
/*     • 90-100 % : to full-transparent                            */
/* ──────────────────────────────────────────────────────────────── */
fun Modifier.verticalGapFade(stripHeight: Dp, density: Density) = drawWithContent {
    drawContent()

    val hPx = with(density) { stripHeight.toPx() }
    if (hPx <= 0f) return@drawWithContent

    val fade = Brush.verticalGradient(
        colorStops = arrayOf(
            0.00f to Color.Transparent,                 //     window top
            0.10f to Color.Black.copy(alpha = .05f),
            0.20f to Color.Black.copy(alpha = .55f),
            0.30f to Color.Black.copy(alpha = .65f),
            0.40f to Color.Black.copy(alpha = .75f),
            0.50f to Color.Black.copy(alpha = .80f),
            0.60f to Color.Black.copy(alpha = .84f),
            0.70f to Color.Black.copy(alpha = .87f),
            0.80f to Color.Black.copy(alpha = .90f),
            0.90f to Color.Black.copy(alpha = .92f),
            0.95f to Color.Black.copy(alpha = .95f),
            1.00f to Color.Black                       // ⬅︎ sits right against the video
        ),
        startY = 0f,
        endY = hPx
    )
    drawRect(fade, size = Size(size.width, hPx), topLeft = Offset.Zero)
}

// xx .................................... temporary .....................
@Composable
fun temporaryOverlays() {
    var showContent by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Box(contentAlignment = Alignment.TopCenter) {
        Column(Modifier.padding(top = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {

            Button(onClick = { showContent = !showContent }) { Text("Click me!") }

            AnimatedVisibility(
                visible = showContent,
                enter = fadeIn() + expandVertically()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painterResource(Res.drawable.compose_multiplatform),
                        contentDescription = null
                    )
                    Text(
                        "placeholder",
                        color = Color.DarkGray,
                        fontSize = 48.sp,
                        fontFamily = FontFamily.Cursive
                    )
                }
            }

            Button(onClick = { showSettings = !showSettings }) { Text("Settings") }
        }
    }
}


fun Modifier.topGradientOverlayOLD(videoTopYDp: Dp, density: Density) = this.then(
    Modifier.drawWithContent {
        drawContent() // Draw the original content

        // Calculate the height in pixels for the gradient overlay above the video
        val gradientHeightPx = with(density) { videoTopYDp.toPx() }

        // Create a gradient from almost fully transparent to opaque black
        val gradient = Brush.verticalGradient(
            colors = listOf(Color.Black.copy(alpha = 0.0f), Color.Black),
            startY = 0f,
            endY = 150f // Fixed endY for consistent rendering across different densities
        )

        // Draw the gradient overlay starting from the top of the desktop to the top of the video
        drawRect(
            brush = gradient,
            size = Size(size.width, gradientHeightPx),
            topLeft = Offset(0f, 0f) // Start from the top of the desktop
        )
    }
)


// endregion