package net.sdfgsdfg.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
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
                    color = Color.Black.copy(alpha = 0.15f),
                    offset = Offset(0f, 1f),
                    blurRadius = 6f
                )
            )
        )
    }
}

/**
 * Ultra-skeuomorphic “glass slug”-button.
 * • pill-shaped billet with cylindrical chrome shading
 * • travelling glare bar (∞ loop)
 * • scale + z-elev press feedback
 *
 *   drop it in and pass just [text] + [onClick] — tweak the knobs later.
 */
@Composable
fun SkeuoButton(
    text: String,
    modifier: Modifier = Modifier,
    // palette --------------------------------------------------------
    baseTint: Color = Color.Black.copy(alpha = .4f),
    bevelLight: Color = Color.DarkGray.copy(alpha = .30f),
    bevelDark:  Color = Color.Black.copy(alpha = .10f),
    sweepTint:  Color = Color.DarkGray.copy(alpha = .70f),
    cornerRadius: Dp = 32.dp,
    onClick: () -> Unit,
) {
    /* ─────────── state ─────────── */
    var pressed by remember { mutableStateOf(false) }
    val pressOffsetPx  by animateIntAsState(if (pressed)  8 else 16)
    val pressBlur      by animateDpAsState(if (pressed) 16.dp else  24.dp)
    val pressScale     by animateFloatAsState(if (pressed) .98f else 1f)


    // ( 1 ) Magic 1: body/background gradient
    var shape    = RoundedCornerShape(cornerRadius)
    val background = Brush.verticalGradient(
        0f to baseTint.lighten(.20f),
        1f to baseTint.darken(.55f)
    )

    // ( 2 ) Magic 2: Bevel gradient
    val bevel    = Brush.verticalGradient(listOf(bevelLight, bevelDark))
    val rCorner = with(LocalDensity.current) { CornerRadius(cornerRadius.toPx()) }

    /* travelling sweep */
    val sweepX by rememberInfiniteTransition().animateFloat(
        initialValue = -0.4f,
        targetValue  =  1.4f,
        animationSpec = infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
    )

    Box(
        modifier
            .defaultMinSize(minWidth = 120.dp, minHeight = 56.dp)
            /* press squash + shadow */
            .graphicsLayer {
                scaleX = pressScale; scaleY = pressScale
                shadowElevation = 6.dp.toPx()
                clip   = false
                shape  = shape
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                        onClick()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {

        val dynBlur   by animateDpAsState(if (pressed) 24.dp  else 34.dp)
        val dynAlphaT by animateFloatAsState(if (pressed) .18f else .15f)   // top
        val dynAlphaB by animateFloatAsState(if (pressed) .35f else .25f)   // bottom

        /* TOP light-shadow */
        // xx old version, no longer used
//        Box(
//            Modifier.matchParentSize()
//                .offset { IntOffset(-6, -pressOffsetPx) }
//                .blur(pressBlur, BlurredEdgeTreatment.Unbounded)
//                .background(Color.Gray.copy(alpha = 0.4f), shape)
//        )
        Box(
            Modifier.matchParentSize()
                .offset { IntOffset(-16, -pressOffsetPx * 2) }      // pull further up
                .blur(dynBlur, BlurredEdgeTreatment.Unbounded)
                .drawBehind {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            0f to Color.White.copy(alpha = dynAlphaT),
                            1f to Color.Transparent,
                            center = center.copy(y = -6f),
                            radius = size.maxDimension * .9f
                        ),
                        cornerRadius = rCorner
                    )
                }
        )

        /* BOT dark-shadow */
        Box(
            Modifier.matchParentSize()
                .offset { IntOffset(x = 12, y = pressOffsetPx) }
                .blur(pressBlur, BlurredEdgeTreatment.Unbounded)
                .background(Color.DarkGray.copy(alpha = .2f), shape)
        )

        /* body + bevel + gloss + sweep */
        Box(
            Modifier.matchParentSize()
                .background(background, shape)
                .border(2.dp, bevel, shape)
                .drawWithContent {
                    drawContent()

                    /* edge darken left/right */
                    drawRoundRect(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = .3f),
                                Color.Black.copy(alpha = .2f),
                                Color.Black.copy(alpha = .1f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.White.copy(alpha = .25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = .1f),
                                Color.Transparent,
                            )
                        ),
                        cornerRadius = rCorner,
                        blendMode = BlendMode.Multiply
                    )

                    /* top gloss */
                    drawRoundRect(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = .3f),
                            1f to Color.Transparent,
                            endY = size.height * .38f
                        ),
                        cornerRadius = rCorner,
                        blendMode = BlendMode.Darken
                    )

                    /* travelling sweep */
                    val barW  = size.width * .12f
                    val x0    = size.width * sweepX - barW / 2
                    drawRoundRect(
                        Brush.linearGradient(
                            0f   to Color.Transparent,
                            0.35f to sweepTint,
                            0.65f to sweepTint.copy(alpha = .12f),
                            1f   to Color.Transparent,
                            start = Offset(x0, 0f),
                            end   = Offset(x0 + barW, size.height)
                        ),
                        cornerRadius = rCorner,
                        blendMode = BlendMode.Lighten
                    )
                }
        )

        /* label */
        Text(
            text.uppercase(),
            fontFamily = FontFamily.Serif,
            color = Color.Black.copy(alpha = .75f),
            style = MaterialTheme.typography.button,
            fontSize = 24.sp,
            letterSpacing = 0.85.sp,
            modifier = Modifier.align(Alignment.Center)
                .padding(horizontal = 48.dp, vertical = 16.dp)
        )
    }
}

/* tiny color helpers */
private fun Color.lighten(f: Float) = lerp(this, Color.White, f)
private fun Color.darken(f: Float)  = lerp(this, Color.Black, f)

/* ─ helpers ─ */
private fun Color.lighten2(frac: Float) =
    Color(
        red   = red   + (1f - red)   * frac,
        green = green + (1f - green) * frac,
        blue  = blue  + (1f - blue)  * frac,
        alpha = alpha
    )

private fun Color.darken2(frac: Float) =
    Color(
        red   = red   * (1f - frac),
        green = green * (1f - frac),
        blue  = blue  * (1f - frac),
        alpha = alpha
    )

/* glass body ---------------------------------------------------------- */
@Composable
private fun rememberBodyBrush(): Brush = remember {
    Brush.radialGradient(
        0.00f to Color(0x9900B7FF),     // 60 % α aqua core
        0.80f to Color.Transparent,          // fade to clear
        0.96f to Color(0xFF9B00FF),     // neon rim
        1.00f to Color.Transparent
    )
}

/* --- expect helper: each platform supplies its own actual --- */
@Composable
expect fun rememberShader(
    press: Float,
    sweep: Float,
): Brush
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