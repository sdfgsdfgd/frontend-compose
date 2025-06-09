@file:Suppress("unused")

package ui

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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
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

const val GoldUnicode = "\u001B[38;5;214m"
val Gold = Color(0xFFFFAF00)

// region ────[ Button & Text ]────────────────────────────────────────────────────────────
/**
 * Jewel‑grade bevel text (no shaders, pure Compose).
 * Layers
 *   1. **Halo** – soft outer bloom that hugs glyph edges
 *   2. **Rim light** – razor‑thin top highlight
 *   3. **Body** – vertical steel‑like gradient + drop shadow
 *   4. **Inner shadow** – crisp letter‑press crease
 */
@Composable
fun SkeuoText(
    text: String,
    textColor: Color = Color(0xFFE3E3E3),   // neutral steel
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    fontFamily: FontFamily = FontFamily.Serif,
) {
    /* shared metrics ------------------------------------------------ */
    val base = TextStyle(fontSize = fontSize, fontFamily = fontFamily)
    val hi = textColor.lighten2(0.6f)                   // rim light
    val hi2 = textColor.lighten2(0.2f)                   // rim2 light
    val mid = textColor                     // face
    val lo = textColor.darken2(.45f)       // shadowed base

    /* steel-like body fill */
    val fill = Brush.verticalGradient(
        0.00f to hi.copy(alpha = .85f),
        0.20f to hi2.copy(alpha = .65f),
        0.60f to mid,
        1.00f to lo
    )

    Box(modifier.graphicsLayer { clip = false }) {
        /* ─ 1  halo (soft outer bloom) – sits *under* everything ─ */
        Text(
            text = text,
            style = base,
            color = hi.copy(alpha = .14f),
            modifier = Modifier.blur(4.dp)
        )

        /* ─ 2  main body with drop shadow ─ */
        Text(
            text = text,
            style = base.merge(
                TextStyle(
                    brush = fill,
                    shadow = Shadow(
                        color = Color.Black.copy(alpha = .60f),
                        offset = Offset(0f, 2f),
                        blurRadius = 4f
                    )
                )
            )
        )

        /* ─ 3  inner shadow (letter-press crease) ─ */
        Text(
            text = text,
            style = base,
            color = Color.Black.copy(alpha = .90f),
            modifier = Modifier
                .offset(y = 1.dp)
                .blur(1.dp)
        )

        /* ─ 4  rim highlight – razor edge catching the light ─ */
        Text(
            text = text,
            style = base,
            color = hi.copy(alpha = .25f),
            modifier = Modifier
                .offset(x = (-2).dp, y = (-3.5).dp)
                .blur(4.8.dp)
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
    textColor: Color = Color.DarkGray.copy(alpha = .4f),
    modifier: Modifier = Modifier,
    // palette --------------------------------------------------------
    baseTint: Color = Color.Black.copy(alpha = .4f),
    bevelLight: Color = Color.DarkGray.copy(alpha = .30f),
    bevelDark: Color = Color.Black.copy(alpha = .10f),
    sweepTint: Color = Color.DarkGray.copy(alpha = .70f),
    cornerRadius: Dp = 32.dp,
    onClick: () -> Unit,
) {
    /* ─────────── state ─────────── */
    var pressed by remember { mutableStateOf(false) }
    val pressOffsetPx by animateIntAsState(if (pressed) 8 else 16)
    val pressBlur by animateDpAsState(if (pressed) 16.dp else 24.dp)
    val pressScale by animateFloatAsState(if (pressed) .98f else 1f)


    // ( 1 ) Magic 1: body/background gradient
    val shape = RoundedCornerShape(cornerRadius)
    val background = Brush.verticalGradient(
        0f to baseTint.lighten(.20f),
        1f to baseTint.darken(.55f)
    )

    // ( 2 ) Magic 2: Bevel gradient
    val bevel = Brush.verticalGradient(listOf(bevelLight, bevelDark))
    val rCorner = with(LocalDensity.current) { CornerRadius(cornerRadius.toPx()) }

    /* travelling sweep */
    val sweepX by rememberInfiniteTransition().animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(22000, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse)
    )

    Box(
        modifier
            .defaultMinSize(minWidth = 120.dp, minHeight = 56.dp)
            /* press squash + shadow */
            .graphicsLayer {
                scaleX = pressScale; scaleY = pressScale
                shadowElevation = 6.dp.toPx()
                clip = false
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

        val dynBlur by animateDpAsState(if (pressed) 24.dp else 34.dp)
        val dynAlphaT by animateFloatAsState(if (pressed) .18f else .15f)   // top

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
                    val barW = size.width * .12f
                    val x0 = size.width * sweepX - barW / 2
                    drawRoundRect(
                        Brush.linearGradient(
                            0f to Color.Transparent,
                            0.35f to sweepTint,
                            0.65f to sweepTint.copy(alpha = .12f),
                            1f to Color.Transparent,
                            start = Offset(x0, 0f),
                            end = Offset(x0 + barW, size.height)
                        ),
                        cornerRadius = rCorner,
                        blendMode = BlendMode.Lighten
                    )
                }
        )

        /* label */
//        Text(
//            text.uppercase(),
//            fontFamily = FontFamily.Serif,
//            color = textColor,
//            style = MaterialTheme.typography.button,
//            fontSize = 24.sp,
//            letterSpacing = 0.85.sp,
//            modifier = Modifier.align(Alignment.Center)
//                .padding(horizontal = 48.dp, vertical = 16.dp)
//        )
        SkeuoText(
            text = text,
            textColor = textColor,
            fontSize = 24.sp,
//            fontFamily = FontFamily.Cursive,
            modifier = Modifier.align(Alignment.Center)
                .padding(horizontal = 48.dp, vertical = 16.dp)
        )
    }
}

/* tiny color helpers */
private fun Color.lighten(f: Float) = lerp(this, Color.White, f)
private fun Color.darken(f: Float) = lerp(this, Color.Black, f)

/* ─ helpers ─ */
private fun Color.lighten2(frac: Float) =
    Color(
        red = red + (1f - red) * frac,
        green = green + (1f - green) * frac,
        blue = blue + (1f - blue) * frac,
        alpha = alpha
    )

private fun Color.darken2(frac: Float) =
    Color(
        red = red * (1f - frac),
        green = green * (1f - frac),
        blue = blue * (1f - frac),
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

// region ────[ **Glass**, GlassCard, GlassTopBar ]────────────────────────────────────────────────────────────

/*───────────────────────────────────────────────────────────────────────────
   GlassKit – one ultra-lean file, three primitives, infinite reuse
───────────────────────────────────────────────────────────────────────────*/
@Immutable
data class GlassStyle(
    /* BODY & BLOOM ------------------------------------------------------ */
    val bodyTint: Color = Color(0xFF8FB5DA),   // main hue
    val bodyFadeStart: Float = .26f,                // α @ left edge
    val bodyFadeMid: Float = .16f,                // α near right edge
    val bloomAlpha: Float = .30f,                // inner bloom strength
    val bloomRadiusScale: Float = 2.2f,                // bloom radius = r × scale
    val specularAlpha: Float = .12f,                // peak α of razor specular
    val specularTail: Float = .01f,                // faint tail α at bottom

    val alpha1: Float = .26f,
    val alpha2: Float = .0f,
    val alpha3: Float = .16f,
    val alpha4: Float = .0f,

    /* BEVEL METRICS ----------------------------------------------------- */
    val radius: Dp = 14.dp,               // corner radius
    val cornerRadius: Dp = 14.dp,               // corner radius
    val rimGap: Dp = 1.8.dp,              // air gap between rims
    val rimStroke: Dp = 0.6.dp,              // rim stroke width

    /* OUTER RIM GLOW ---------------------------------------------------- */
    val rimBaseAlpha: Float = .12f,                // when progress = 0
    val rimGlowDelta: Float = .10f,                // added α at progress = 1

    /* DARK CREASE ------------------------------------------------------- */
    val creaseAlpha: Float = .30f,

    /* INNER RIM SWEEP --------------------------------------------------- */
    val innerRimColor: Color = Color(0xFFC9E9FF),
    val innerRimSpan: Float = 340f,
    val innerRimCenter: Float = 120f,
    val innerRimAlpha: Float = .84f,

    /* EDGE TICK --------------------------------------------------------- */
    val tickAlpha: Float = .48f,
    val tickSweepDeg: Float = 65f,
    val tickStroke: Float = 0.2f                 // px, not dp – keep tiny
)

// TODO-1: clear unused, match properly to inner rims
/*───────────────────────────────────────────────────────────────────────────────
   glassPainter(…)  –  ONE SKIN TO RULE THEM ALL
   ---------------------------------------------------------------------------
   A **pure** DrawScope lambda that renders a smoked-glass surface with:
   1. Body tint  — left-to-right colour fade + radial bloom
   2. Specular   — razor-thin highlight for curved-glass illusion
   3. Bevel stack
        • bright outer rim (animation-ready:   alpha = .12 + .10 × progress)
        • air gap                                 (lets the rim “float”)
        • dark crease                             (depth/shadow)
        • icy inner rim  (sweep-gradient fades 360°)
   4. Edge tick — tiny white arc on the top-left quadrant

   Tweak knobs
   ───────────
   • `color`        body tint + bloom hue
   • `radius`       corner round-ness (dp)  ← comes from the call site
   • `progress`     0‒1, drives outer-rim brightness (selection / hover)

   Usage
   ─────
       val skin = glassPainter(Color(0xFF8FB5DA))   // cyan-ice
       Box(
           Modifier
               .drawBehind { skin(progress = 1f, radius = 14.dp) }
       )

   Performance
   ───────────
   • All maths done *once* per draw pass; no object allocations inside loops
   • Compatible with `drawWithCache { onDrawBehind {…} }` if you want caching
   • Works on any shape/size; caller provides radius & optional slice mask

   @param color   Base tint; affects body gradient, bloom & spec tail
   @return        A GlassPainter ready for Modifier.drawBehind/Modifier.glass
───────────────────────────────────────────────────────────────────────────────*/
typealias GlassPainter = DrawScope.(progress: Float, style: GlassStyle) -> Unit

fun glassPainter(style: GlassStyle = GlassStyle()): GlassPainter = { p, _ ->
    /* px cache */
    val rPx = style.radius.toPx()    // corner radius
    val cornerRadius = style.cornerRadius.toPx()
    val gapPx = style.rimGap.toPx()    // air gap between rims
    val strokePx = style.rimStroke.toPx() // rim stroke width

    /*────────────────────────────────────────────────────────────────────*/
    /* 1 ▸ GLASS BODY  (tint + bloom + central spec)                      */
    /*────────────────────────────────────────────────────────────────────*/

    // 1A — smoked-glass lateral shade
    drawRoundRect(
        brush = Brush.horizontalGradient(
            0f to style.bodyTint.copy(alpha = style.alpha1),    // left edge tint
            .44f to Color.Transparent.copy(alpha = style.alpha2),
            .85f to style.bodyTint.copy(alpha = style.alpha3),  // subtle return on right
            1f to Color.Transparent.copy(alpha = style.alpha4),
        ),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )

    // 1B — cyan bloom hugging the TL bevel
    drawRoundRect(
        brush = Brush.radialGradient(
            listOf(style.bodyTint.copy(alpha = .30f), Color.Transparent),
            center = Offset(rPx * .9f, rPx * .9f),
            radius = rPx * 2.2f
        ),
        blendMode = BlendMode.Plus,
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )

    // 1C — razor-thin specular band for curved surface illusion
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            .11f to Color.White.copy(alpha = .12f),
            .44f to Color.Transparent,
            .88f to style.bodyTint.copy(alpha = .01f),
            1f to Color.Transparent
        ),
        blendMode = BlendMode.Lighten,
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )

    /*────────────────────────────────────────────────────────────────────*/
    /* 2 ▸ BEVEL RIMS  (bright → gap → dark → gap → icy)                  */
    /*────────────────────────────────────────────────────────────────────*/
    fun rim(step: Int, brush: Brush) = drawRoundRect(
        brush = brush,
        topLeft = Offset(gapPx * step, gapPx * step),
        size = Size(
            size.width - 2 * gapPx * step,
            size.height - 2 * gapPx * step
        ),
        style = Stroke(width = strokePx),
        cornerRadius = CornerRadius(cornerRadius - gapPx * step, cornerRadius - gapPx * step)
    )

    // 2a — bright outer rim (animated)
    rim(1, SolidColor(Color.White.copy(alpha = style.rimBaseAlpha + style.rimGlowDelta * p)))

    // 2b — dark crease for depth
    rim(2, SolidColor(Color.Black.copy(alpha = .30f)))

    // 2c — icy inner rim (sweep gradient, fades over 340°)
    rim(
        3,
        Brush.rimSweep(
            highlight = Color(0xFFC9E9FF),
            spanDeg = 340f,
            centerDeg = 120f,
            alphaMax = .84f
        )
    )

    /*────────────────────────────────────────────────────────────────────*/
    /* 3 ▸ EDGE TICK  (top-left arc)                                      */
    /*────────────────────────────────────────────────────────────────────*/
    val off = gapPx * 3                                   // inset matches rim(3)
    drawArc(
        color = Color.White.copy(alpha = .48f),
        startAngle = 180f,                                // left
        sweepAngle = 65f,                                 // up to top
        useCenter = false,
        topLeft = Offset(off, off),
        size = Size(size.width - off * 2, size.height - off * 2),
        style = Stroke(width = 0.2f)
    )
}

/*────────────────────  2.  MODIFIER  ─────────────────────────────────────*/
fun Modifier.glass(
    painter: GlassPainter = glassPainter(),
    progress: Float = 1f,
    style: GlassStyle = GlassStyle(),
    slice: ClosedFloatingPointRange<Float> = 0f..1f
) = drawBehind {
    painter(progress, style)        // single call – painter holds everything

    if (slice != 0f..1f) {
        val topKeep = size.height * slice.start
        val bottomKeep = size.height * slice.endInclusive
        if (topKeep > 0f)
            drawRect(
                color = Color.Transparent,
                size = Size(size.width, topKeep),
                blendMode = BlendMode.Clear
            )
        if (bottomKeep < size.height)
            drawRect(
                color = Color.Transparent,
                topLeft = Offset(0f, bottomKeep),
                size = Size(size.width, size.height - bottomKeep),
                blendMode = BlendMode.Clear
            )
    }
}

@Composable
fun GlassCard(
    selected: Boolean,
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle(),
    onClick: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    val prog by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(.55f, 80f), label = "glassProgress"
    )
    Box(
        modifier
            .glass(progress = prog, style = style)
            .clickable(onClick = onClick)
            .then(modifier)
    ) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

@Composable
fun GlassTopBar(
    height: Dp,
    cutRatio: Float = 0.16f,                      // 16 % trimmed from top
    modifier: Modifier = Modifier,
    style: GlassStyle = GlassStyle(bodyTint = Color.Black),
    progress: Float = 1f,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val painter = remember(style) { glassPainter(style) }  // (Float, Style) -> Unit
    val stretch = 1f / (1f - cutRatio)                     // e.g. 1/(1-0.16) ≈ 1.19

    Box(
        modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .drawBehind {
                withTransform({
                    scale(1f, stretch, pivot = Offset.Zero)        // ❶ stretch down
                    translate(0f, -size.height * cutRatio)         // ❷ lift up cut
                }) {
                    painter(progress, style)                       // background glass
                }
            }
            .then(modifier),
        contentAlignment = Alignment.Center,
        content = content
    )
}

/**
 * Build a sweep brush whose alpha rises with [easing] then falls with the
 * mirrored easing — perfect for rim-lighting that fades out smoothly on both
 * ends and leaves the seam transparent.
 *
 * @param highlight   the colour at full intensity
 * @param spanDeg     total arc in degrees that receives the highlight
 * @param alphaMax    peak alpha inside the span
 * @param easing      shaped ramp; default = FastOutSlowIn
 */
fun Brush.Companion.rimSweep(
    highlight: Color,
    spanDeg: Float = 320f,          // how wide the bright arc is
    centerDeg: Float = 120f,        // where it’s brightest (0° = right, CCW)
    alphaMax: Float = .60f,
    seamAlpha: Float = .08f,        // tiny colour at the seam so it never vanishes
    easing: Easing = FastOutSlowInEasing,
): Brush {
    // ----- convert angles to normalized t 0‒1
    val span = spanDeg.coerceIn(1f, 360f)
    val halfArc = span / 2f
    val startDeg = (centerDeg - halfArc + 360f) % 360f
    val endDeg = (centerDeg + halfArc) % 360f
    fun d2t(d: Float) = d / 360f          // deg → [0,1]

    // ----- eased alpha at ¼ & ¾ points to get a smooth shoulder
    val shoulder = easing.transform(0.4f)          // 0.4 gives nice curve
    val alphaPeak = alphaMax
    val alphaEdge = alphaMax * shoulder

    return sweepGradient(
        0f to highlight.copy(alpha = seamAlpha),  // 0° = seam
        d2t(startDeg) to highlight.copy(alpha = alphaEdge),
        d2t(centerDeg) to highlight.copy(alpha = alphaPeak),
        d2t(endDeg) to highlight.copy(alpha = alphaEdge),
        1f to highlight.copy(alpha = seamAlpha)   // 360° = same
    )
}

// endregion

// region ────[ Timings  ( Actual Magic ) ]────────────────────────────────────────────────────────────
val SlowOutFastInEasing = Easing { t -> 1 - FastOutSlowInEasing.transform(1 - t) }
/**
 * Two-stage ramp:
 *   ① 0-knee  : darkA → midA   (super-gentle linear fade)
 *   ② knee-1  : midA  → lightA (eased, fast drop-off)
 *
 *
 *          notto: mainly was for eased TransparencY GradienT, butcanbeforanything
 *
 *   [ The Knee ] is not a hard cut, but a smooth transition.
 */
private fun buildStops(
    totalStops: Int = 1024,
    knee: Float = 0.66f,   // 22 % of the strip = slow zone
    lightA: Float = .02f,    // 0.02% opacity practically gone at top
    midA: Float = .90f,    // ~90 % opacity when we hit the knee
    darkA: Float = .99f,    // 99 % at very bottom
//    tailEase:  Easing = FastOutSlowInEasing
    tailEase: Easing = SlowOutFastInEasing // SlowOutFastInEasing (inverted)
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
            midA + (lightA - midA) * e           // .60 → .02 rapidly
        }

        p to Color.Black.copy(alpha = a)
    }
// endregion

// region ────[ Vid Overlays ]────────────────────────────────────────────────────────────

/* ── the fade strip as its own composable / layer ────────────── */
@Composable
fun FadeStrip(blurRadius: Dp = 24.dp) {
    val stops = remember {
        buildStops(
            totalStops = 1024,
            darkA = .995f,
            lightA = .08f        // just a ghost at the ceiling
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
