package ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ui.DynamicIsland
import ui.IslandState
import ui.TimeMark
import ui.islandAutosize
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

// ———————————————————————————————————————————————
// DynamicIsland + LuxuryInput integration
// ———————————————————————————————————————————————
// ———————————————————————————————————————————————
// One-call demo you can drop anywhere
//    Uses existing DynamicIsland + IslandState
// ———————————————————————————————————————————————
@Composable
fun Demo2DynamicIslandWLuxuryInput() {
    var text by remember { mutableStateOf(TextFieldValue("")) }
    var islandState by remember { mutableStateOf<IslandState>(IslandState.Split) }

    Column(
        Modifier
            .wrapContentSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DynamicIslandWithLuxuryInput(
            state = islandState,
            value = text,
            onValueChange = { text = it },
            onSend = { text = TextFieldValue("") }
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier
                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { islandState = IslandState.Default }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Compact", color = Color.White) }
            Box(
                Modifier
                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { islandState = IslandState.Split }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("Split", color = Color.White) }
            Box(
                Modifier
                    .background(Color(0x22FFFFFF), RoundedCornerShape(8.dp))
                    .clickable { islandState = IslandState.FaceUnlock }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) { Text("FaceUnlock", color = Color.White) }
        }
    }
}

@Composable
fun DynamicIslandWithLuxuryInput(
    state: IslandState,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Type something…",
    blurRadius: Dp = 4.dp,
    splitOffset: Dp = 40.dp,
    onSend: (() -> Unit)? = null
) {
    // We want parent bounds (window-wide). Use BoxWithConstraints at the wrapper level.
    BoxWithConstraints(modifier) {
        println("📌 BoxWithConstraints recomposed, maxWidth: $maxWidth")

        val stableMaxWidth = remember { maxWidth }   // cache maxWidth ONCE
        val stableMaxHeight = remember { maxHeight }
        var islandState by remember(state) { mutableStateOf(state.island) }
        println("🔥 Island state updated: $islandState")

        val contentPad = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
        val lanePad = PaddingValues(horizontal = 24.dp, vertical = 8.dp)

        DynamicIsland(
            state = state,
            blurRadius = blurRadius,
            splitOffset = splitOffset,
            islandSizeOverride = islandState.also { println("🛑 islandSizeOverride applied: $it") },
            islandContent = {
                LuxuryInput(
                    value = value,
                    onValueChange = onValueChange,
                    placeholder = placeholder,
                    singleLine = false,
                    modifier = Modifier.islandAutosize(
                        base = state.island,
                        maxWidth = stableMaxWidth.also { println("🌟 maxWidth passed to islandAutosize: $it") },
                        maxHeight = stableMaxHeight,
                        contentPadding = contentPad
                    ) { islandState = it },
                    lanePadding = lanePad
                )
            },
            bubbleContent = {
                if (onSend != null) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .background(Color(0x22FFFFFF), CircleShape)
                            .clickable { onSend() },
                        contentAlignment = Alignment.Center
                    ) { SendGlyph(tint = Color.White, glyphSize = 16.dp) }
                }
            }
        )
    }
}

@Composable
private fun SendGlyph(tint: Color, glyphSize: Dp) {
    Canvas(Modifier.size(glyphSize)) {
        val s = this.size                 // Size in px from DrawScope
        val d = min(s.width, s.height)

        val p = Path().apply {
            moveTo(0.05f * d, 0.85f * d)
            lineTo(0.95f * d, 0.50f * d)
            lineTo(0.05f * d, 0.15f * d)
            close()
        }
        drawPath(p, tint)
    }
}

data class CaretSpec(
    val color: Color = Color(0xAFB8FA10),   // #b8fa10af
    val glowSoft: Color = Color(0x808A6534),
    val glowStrong: Color = Color(0xB80C0C05),
    val glowHighlight: Color = Color(0x4DF5B504),
    val widthDp: Float = 2f,
    val blinkMillis: Int = 1150
)

/**
 * LuxuryInput — realistic saber / lantern style caret with breathing triple-glow and 3 inner layers (hail skeuo).
 *
 * ### What it is
 * Custom BasicTextField that hides the system caret and renders a physically-plausible,
 * animated “blade” directly on the text layer via `drawWithContent`, then finishes with a
 * tiny offscreen inner-shadow stack for depth and rim light.
 *
 * ### Rendering pipeline (top → bottom in this file)
 * 1) Text + selection (`drawContent()`).
 * 2) Caret hull (vertical screen-tinted gradient; feathered so ends never drop to 0).
 * 3) End-cap occlusion + horizontal bevel (Multiply) to keep glyphs readable under the bar.
 * 4) Triple, breathing capsule glow (radial, additive) with two eased knees.
 * 5) Offscreen inner-shadow stack (bevel occlusion + colored rim + tip falloff).
 *
 * ### Animation channels
 * - `focusFade`: smooth appear/disappear.
 * - `blinkAlpha`: 1 → 0.5 → 1 loop (gated by focus).
 * - `glowProgress`: 0 ↔ 1 breathe loop driving glow size/intensity and inner-shadow radius/spread.
 * - Speed-tuned caret motion: spring stiffness/damping derived from travel speed.
 *
 * ### Knobs
 * - `caretSpec.widthDp`,
 * - `caretSpec.color`,
 * - `caretSpec.blinkMillis`
 * - `caretSpec.glowSoft/Strong/Highlight`  hues
 * - Inner-shadow passes near the end (alpha/spread/radius/blendMode)
 */
@Composable
fun LuxuryInput(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    caretSpec: CaretSpec = CaretSpec(),
    placeholder: String = "Type…",
    textStyle: TextStyle = TextStyle(
        color = Color(0xCC8A6534),
        fontFamily = FontFamily.Monospace,
        fontSize = 16.sp,
        textAlign = TextAlign.Start
    ),
    singleLine: Boolean = false,
    lanePadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 18.dp)
) {
    var focused by remember { mutableStateOf(false) }
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    var containerOrigin by remember { mutableStateOf(Offset.Zero) }
    var textContentOrigin by remember { mutableStateOf(Offset.Zero) }

    // 1) Smooth focus fade (separate specs for in/out)
    val focusFade by updateTransition(targetState = focused, label = "focus")
        .animateFloat(
            transitionSpec = {
                if (targetState) tween(222, easing = FastOutSlowInEasing)
                else tween(666, easing = FastOutSlowInEasing)
            },
            label = "focusFade"
        ) { isFocused -> if (isFocused) 1f else 0f }

    // Gate oscillators so we don’t churn offscreen. todo: ??? huh? check necessity
    val runOsc = focusFade > 0.01f
    val osc = rememberInfiniteTransition(label = "osc")
    val blinkAlpha by if (runOsc) osc.animateFloat(
        initialValue = 1f, targetValue = 1f, label = "blinkAlpha",
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = caretSpec.blinkMillis
                1f at 0; 0.5f at caretSpec.blinkMillis / 2; 1f at caretSpec.blinkMillis
            }
        )
    ) else rememberUpdatedState(1f)

    // Glow Layer
    val glowProgress by if (runOsc) osc.animateFloat(
        initialValue = 0f, targetValue = 1f, label = "glowProgress",
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2250
                0f at 0 using FastOutSlowInEasing
                1f at 400 using FastOutSlowInEasing
                0.96f at 750 using LinearEasing
                1f at 1050 using LinearEasing
                0.98f at 1150 using LinearEasing
                0f at 2250 using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        )
    ) else rememberUpdatedState(0f)

    /**
     *  What we have so far:
     *   Stable target: caretTarget: MutableState<Rect> decouples events from motion.   ✔️
     *   Continuous tick: compute dtMs and adapt spring from remaining error speed. ✔️   ( no continuum ..? )
     *   Three Animatables (X, Y, H), relaunched every tick with animateTo              ⚠️ (cancels prior run each frame)
     *   delay(16L) cadence.    ⚠️ (not vsync; can phase‑fight the renderer)
     *
     * Next upgrades (sorted):
     * 1) Animatable(Offset) for XY atomicity
     * 2) Hysteresis (0.25px XY, 0.5px H) + grid quantization
     * 3) Spec smoothing for t (low-pass or bands)
     * 4) Focus gating of the loop
     * 5) Space-run bias (+0.1 to t)
     * 6) Lag telemetry & first-target snap
     * 7) Scroll-offset-aware caret draw (or % of line position preserved on scroll)
     */
    // TODO-1: Atomicity purist: if you want literal atomic XYH updates, use Animatable(Offset) + Animatable(height)
    //    with a TwoWayConverter so the solver is truly vectorized. Today’s visual sync is good, but this is bulletproof.
    // TODO-2: Spec smoothing: recomputing stiffness/damping each tick can produce tiny parameter jitter.
    //  Fix: low‑pass t (e.g., t = lerp(prevT, newT, 0.25f)), or quantize to a few bands.
    // TODO-3: Adaptive floor for space‑hold. If last typed char is " ", bias your t upward by +0.10 so the chase feels a hair more assertive during runs of spaces (while normal typing stays calmer).
    //
    // ---------- SPEED‑TUNED CARET MOTION ----------
    val d = LocalDensity.current
    val caretWidthPx = with(d) { caretSpec.widthDp.dp.toPx() }
    val minCaretHeightPx = with(d) { 18.dp.toPx() }

    // Animated caret channels
    val caretX = remember { Animatable(0f) }
    val caretY = remember { Animatable(0f) }
    val caretH = remember { Animatable(minCaretHeightPx) }

    // Target rect derived from layout + selection (lighter)
    val targetRect by remember(value.selection, layout) {
        derivedStateOf {
            val it = layout ?: return@derivedStateOf null
            val i = value.selection.start.coerceIn(0, it.layoutInput.text.text.length)
            runCatching { it.getCursorRect(i) }.getOrNull()
        }
    }

    val caretTarget = remember { mutableStateOf(Rect.Zero) }

    LaunchedEffect(Unit) { // Continuous animation loop
        var lastMoveNanos = TimeMark.nanoTime()

        while (true) {
            val now = TimeMark.nanoTime()
            val dtMs = ((now - lastMoveNanos) / 1_000_000L).coerceAtLeast(1L)

            val target = caretTarget.value

            val dx = target.left - caretX.value
            val dy = target.top - caretY.value
            val dist = sqrt(dx * dx + dy * dy)
            val pxPerMs = dist / dtMs.toFloat()

            val t = (pxPerMs / 2.5f).coerceIn(0f, 1f)
            val stiffness = lerp(220f, 1600f, t)
            val damping = lerp(0.90f, 0.70f, t)

            val spec = spring(
                stiffness = stiffness,
                dampingRatio = damping,
                visibilityThreshold = 0.5f
            )

            val hSpec = spring(
                stiffness = stiffness * 0.9f,
                dampingRatio = damping,
                visibilityThreshold = 0.5f
            )

            // Launch animations towards mutable target
            launch { caretX.animateTo(target.left, animationSpec = spec) }
            launch { caretY.animateTo(target.top, animationSpec = spec) }
            launch { caretH.animateTo(max(target.height, minCaretHeightPx), animationSpec = hSpec) }

            lastMoveNanos = now
            delay(16L) // ~60fps smoothness; tweak as needed
        }
    }

    // Lightweight target updates without breaking animations
    LaunchedEffect(targetRect, focusFade) {
        val r = targetRect ?: return@LaunchedEffect

        if (focusFade <= 0.01f) {
            caretX.snapTo(r.left)
            caretY.snapTo(r.top)
            caretH.snapTo(max(r.height, minCaretHeightPx))
        }

        caretTarget.value = r
    }
    // ---------- END SPEED‑TUNED CARET MOTION ----------

    Box(
        modifier
            .background(Color(0x33000000), RoundedCornerShape(10.dp))
            .wrapContentHeight(Alignment.Top)
            .wrapContentWidth(Alignment.Start)
            .padding(horizontal = 24.dp)
            .onGloballyPositioned { containerOrigin = it.positionInRoot() }
            .graphicsLayer { clip = false },
        contentAlignment = Alignment.TopStart
    ) {
        Box(
            Modifier
                .padding(lanePadding)
                .graphicsLayer { clip = false }
        ) {
            BasicTextField(
                value = value,
                onValueChange = {
                    println("Text changed: ${it.text}")

                    onValueChange(it)
                },
                singleLine = singleLine,
                textStyle = textStyle,
                cursorBrush = SolidColor(Color.Transparent), // hide native caret
                onTextLayout = {
                    // xx Below checks are necessary instead of doing `layout = it  (nl)`  because otherwise
                    //  --> `.islandAutosize()`  calls intrinsics on BasicTextField (maxIntrinsicWidth/Height) every measure
                    //  -->  guarded onTextLayout (ignore minWidth==0 || minHeight==0 || size==0, and de-dupe).
                    //            stops caret/layout state from reacting to pre-passes
                    val c = it.layoutInput.constraints
                    val isPrepass = c.minWidth == 0 || c.minHeight == 0 || it.size.width == 0 || it.size.height == 0
                    if (isPrepass) return@BasicTextField

                    // Optional: de-dupe
                    val same = layout?.size == it.size &&
                            layout?.layoutInput?.constraints == it.layoutInput.constraints &&
                            layout?.layoutInput?.text?.text == it.layoutInput.text.text
                    if (!same) {
                        layout = it
                        println("Final layout accepted: ${it.size}  constraints=$c")
                    }
                },
                modifier = modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box(
                        Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.text.isEmpty()) {
                            Text(
                                placeholder,
                                color = Color(0x99B4B4B4),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                        Box(
                            Modifier.onGloballyPositioned { coords ->
                                textContentOrigin = coords.positionInRoot() - containerOrigin
                            }
                        ) {
                            inner()
                        }
                    }
                }
            )
        }

        // Caret hull / occlusion without blur
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; clip = false }
        ) {
            val env = (blinkAlpha * focusFade).coerceIn(0f, 1f)
            if (env <= 0.01f) return@Canvas

            val left = caretX.value + textContentOrigin.x
            val top = caretY.value + textContentOrigin.y
            val h = caretH.value
            val leftSnapped = round(left)
            val topSnapped = round(top)
            val widthSnapped = max(1f, round(caretWidthPx))

                        // Layer 1 - Caret: FEATHERED HULL + symmetric top/bottom fade (no mid ridge)
            drawRect(
                brush = Brush.verticalGradient(
                    0.05f to caretSpec.color.copy(alpha = 0.12f),
                    0.50f to caretSpec.color.copy(alpha = 0.55f),
                    0.95f to caretSpec.color.copy(alpha = 0.12f)
                ),
                topLeft = Offset(leftSnapped - 0.5f, topSnapped),
                size = Size(widthSnapped + 1f, h),
                blendMode = BlendMode.Screen,
                alpha = env
            )

                        // Layer 2: end-cap occlusion so tips don’t look chopped
            drawRect(
                brush = Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.00f to Color.Black.copy(alpha = 0.35f),
                        0.18f to Color.Transparent,
                        0.82f to Color.Transparent,
                        1.00f to Color.Black.copy(alpha = 0.35f)
                    ),
                    startY = top,
                    endY = top + h
                ),
                topLeft = Offset(leftSnapped, top),
                size = Size(widthSnapped, h),
                blendMode = BlendMode.Multiply,
                alpha = env
            )

            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Black.copy(alpha = 0.28f),
                    0.5f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.28f)
                ),
                topLeft = Offset(leftSnapped, top),
                size = Size(widthSnapped, h),
                blendMode = BlendMode.Multiply,
                alpha = 0.40f * env
            )
        }

        // Glow capsule overlay (blurred)
        Canvas(
            Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen; clip = false }
                .blur(radius = 3.25.dp, edgeTreatment = BlurredEdgeTreatment.Unbounded)
        ) {
                // α drive = blink * focus (matches original pulse)
            val env = (blinkAlpha * focusFade).coerceIn(0f, 1f)
            if (env <= 0.01f) return@Canvas

            fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
            fun tint(c: Color, r: Float, g: Float, b: Float) = Color(
                red = (c.red * r).coerceIn(0f, 1f),
                green = (c.green * g).coerceIn(0f, 1f),
                blue = (c.blue * b).coerceIn(0f, 1f),
                alpha = c.alpha
            )

            val eRaw = glowProgress.coerceIn(0f, 1f)
            val e = FastOutSlowInEasing.transform(eRaw)
            val knee1 = lerp(0.18f, 0.30f, e)
            val knee2 = lerp(0.60f, 0.95f, e)
            val vScale = 1.15f

                // Late-gate for highlight (wobble cadence)
            val k = ((eRaw - 0.85f) / 0.15f).coerceIn(0f, 1f)

            // Local-space coordinates (will clip at parent bounds, but shows reliably)
            val left = caretX.value + textContentOrigin.x
            val top = caretY.value + textContentOrigin.y
            val h = caretH.value

            // tiny seam feather to cross-fade layers without changing timing
            val feather = 0.035f
            fun stopsFeathered(color: Color, aCore: Float, aMid: Float, amp: Float) = arrayOf(
                0f to color.copy(alpha = aCore * amp),
                knee1 to color.copy(alpha = aMid * amp),
                (knee2 - feather).coerceIn(0f, 1f) to color.copy(alpha = (aMid * 0.08f) * amp),
                knee2 to Color.Transparent
            )

            fun drawGlowCapsule(
                basePad: Float,
                growPad: Float,
                color: Color,
                aCore: Float,
                aMid: Float,
                amp: Float,
                dx: Float = 0f,
                dy: Float = 0f,
                vS: Float = vScale
            ) {
                val pad = (basePad + growPad * e) * focusFade
                val w1 = caretWidthPx + pad * 2f
                val h1 = h + pad * 2f * vS
                val r = min(w1, h1) / 2f
                val gradR = 0.5f * sqrt(w1 * w1 + h1 * h1) * 0.90f

                drawRoundRect(
                    brush = Brush.radialGradient(
                        colorStops = stopsFeathered(color, aCore, aMid, amp),
                        center = Offset(left + caretWidthPx / 2f + dx, top + h / 2f + dy),
                        radius = gradR
                    ),
                    topLeft = Offset(left - pad + dx, top - pad * vS + dy),
                    size = Size(w1, h1),
                    cornerRadius = CornerRadius(r, r),
                    blendMode = BlendMode.Plus // keep additive glow character
                )
            }

            // Inner two — pads (wobble), α = env
            drawGlowCapsule(2.dp.toPx(), 5.dp.toPx(), caretSpec.glowSoft, 0.32f, 0.20f, env)
            drawGlowCapsule(6.dp.toPx(), 10.dp.toPx(), caretSpec.glowStrong, 0.26f, 0.14f, env)

            // === Outer highlight ===
            if (eRaw > 0.85f) {
                // 1) Stronger crest (amplitude + a touch bigger near crest)
                val crestAmp = lerp(1f, 1.90f, FastOutSlowInEasing.transform(k)) // up to +90% α
                val crestGrow = lerp(1f, 2.20f, FastOutSlowInEasing.transform(k)) // up to +120% radius
                drawGlowCapsule(
                    basePad = 9.dp.toPx(),
                    growPad = 9.dp.toPx() * k * crestGrow,
                    color = caretSpec.glowHighlight,
                    aCore = 0.32f * k * crestAmp,
                    aMid = 0.12f * k * crestAmp,
                    amp = env,
                    vS = vScale + 0.06f * FastOutSlowInEasing.transform(k) // subtle vertical anisotropy
                )

                // 2) Subtle chromatic fringe at the crest (lens realism)
                val fAlpha = 0.07f * env * k                            // very low α; just a hint
                val fOff = with(d) { (0.6f * k).dp.toPx() }           // px offset grows at crest
                val cR = tint(caretSpec.glowHighlight, 1.20f, 0.97f, 0.92f) // warm/red bias
                val cB = tint(caretSpec.glowHighlight, 0.92f, 0.97f, 1.20f) // cool/blue bias
                drawGlowCapsule(9.dp.toPx(), 9.dp.toPx() * k, cR, 0.14f * k, 0.06f * k, fAlpha, dx = +fOff)
                drawGlowCapsule(9.dp.toPx(), 9.dp.toPx() * k, cB, 0.14f * k, 0.06f * k, fAlpha, dx = -fOff)
            }
        }

        // Layer 5 (offscreen): inner-shadow stack for bevel + colored rim + tip falloff
        if (focusFade > 0.01f) {
            val env = (blinkAlpha * focusFade).coerceIn(0f, 1f)
            val t = FastOutSlowInEasing.transform(glowProgress.coerceIn(0f, 1f))
            val wPx = max(1f, round(caretWidthPx))
            val hPx = caretH.value
            val shape = RoundedCornerShape(percent = 50)
            val xOffsetDp = with(d) { (caretX.value + textContentOrigin.x).toDp() }
            val yOffsetDp = with(d) { (caretY.value + textContentOrigin.y).toDp() }

            // Inline overlay: bevel + occlusion that "breathes" with glow
            Box(
                Modifier
                    .offset(x = xOffsetDp, y = yOffsetDp)
                    .size(with(d) { wPx.toDp() }, with(d) { hPx.toDp() })
                    .clip(shape)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    // 5.1 occlusion ridge (depth). Strong at low e, backs off at peak.
                    .innerShadow(shape) {
                        radius = with(d) { lerp(2.dp, 8.dp, t).toPx() }
                        spread = 2f
                        color = Color.Black
                        alpha = (0.94f - 0.42f * t) * env   // strong at low e, eases at peak
                        blendMode = BlendMode.Multiply
                        offset = Offset.Zero
                    }
                    // 5.2 colored rim (carved light).  Softlight/Plus  also looks nice
                    .innerShadow(shape) {
                        val e = t
                        radius = with(d) { lerp(1.dp, 12.dp, e).toPx() }
                        spread = lerp(1f, 14f, e)
                        color = caretSpec.color
                        alpha = env * lerp(0.06f, 0.22f, e)                  // breathe with focus/blink
                        blendMode = BlendMode.Multiply // OR --> if (e < 0.75f) BlendMode.Softlight else BlendMode.Plus
                        offset = Offset.Zero
                    }
                    // 5.3 subtle tip falloff to round the ends more when glow dips
                    .innerShadow(shape) {
                        radius = with(d) { lerp(1.dp, 4.dp, t).toPx() }
                        spread = lerp(0f, 4f, t)
                        color = Color.Black
                        alpha = lerp(0.01f, 0.16f, t)  // xx    stop was    0.18f * env
                        blendMode = BlendMode.Multiply
                        offset = Offset.Zero
                    }
            )
        }
    }
}
