package ui

import androidx.annotation.FloatRange
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.ArcAnimationSpec
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.ExperimentalAnimationSpecApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.animateValue
import androidx.compose.animation.core.animateValueAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.roundToInt

// region ───[ Helpers ]────────────────────────────────────────────────────────────
const val GoldUnicode = "\u001B[38;5;214m"
const val Reset = "\u001B[0m"
// val Gold = Color(0xFFFFAF00)

/* tiny color helpers */
private fun Color.lighten(f: Float) = lerp(this, Color.White, f)
private fun Color.darken(f: Float) = lerp(this, Color.Black, f)
private fun Color.lighten2(frac: Float) = Color(
    red = red + (1f - red) * frac,
    green = green + (1f - green) * frac,
    blue = blue + (1f - blue) * frac,
    alpha = alpha
)

private fun Color.darken2(frac: Float) = Color(
    red = red * (1f - frac),
    green = green * (1f - frac),
    blue = blue * (1f - frac),
    alpha = alpha
)

// endregion

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

    val dynBlur by animateDpAsState(if (pressed) 24.dp else 34.dp)
    val dynAlphaT by animateFloatAsState(if (pressed) .18f else .15f)   // top


    // ( 1 ) Magic 1: body/background gradient
    val shape = RoundedCornerShape(cornerRadius)
    val background = Brush.verticalGradient(
        0f to baseTint.lighten(.20f),
        1f to baseTint.darken(.55f)
    )

    // ( 2 ) Magic 2: Bevel gradient
    val bevel = Brush.verticalGradient(listOf(bevelLight, bevelDark))
    val rCorner = with(LocalDensity.current) { CornerRadius(cornerRadius.toPx()) }

    val topShadow = CustomShadow(
        color = Color.White.copy(alpha = dynAlphaT),
        blur = pressBlur,
        dx = (-16).dp,
        dy = (-pressOffsetPx * 11 / 10).dp
    )

    val bottomShadow = CustomShadow(
        color = Color.DarkGray.copy(alpha = .2f),
        blur = pressBlur,
        dx = 12.dp,
        dy = pressOffsetPx.dp
    )


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
            .customShadow(
                outerShadows = listOf(topShadow, bottomShadow),
                shape = shape
            )
            .background(background, shape)
            .border(2.dp, bevel, shape)
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
        /* body + bevel + gloss + sweep */
        Box(
            Modifier.matchParentSize()
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

// endregion

// region ────[ Liquid Glass ]────────────────────────────────────────────────────────────
fun interface FrameCallback {
    fun onFrameCaptured(ptr: Long)
}

expect object DesktopCaptureBridge {
    fun startCapture(callback: FrameCallback)
    fun createSkiaImageFromIOSurface(surfacePtr: Long, contextPtr: Long): Long
    fun createImageBitmapFromSkiaImage(skImagePtr: Long): ImageBitmap
    fun hasScreenCapturePermission(): Boolean
}

@Composable
expect fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: LiquidGlassStyle,
): Modifier

// Real-time Refraction
//
// JNI Hack !
// LiquidGlass on Desktop&otherplatforms requires gluing screen capture (in mac case Metal/CoreGraphics) into a Compose
// .mm native Objective-C + Metal Dynamic Library .dylib is in --> [ https://chatgpt.com/g/g-3X6EMarap-x5/c/687a25d2-9f50-832a-8f52-4c94e5d02edd ]
//
// Slack Discussion & LiqGlass on Android:  [ https://kotlinlang.slack.com/archives/C0BJ0GTE2/p1752806789218369 ]
@Composable
fun LiquidGlassDemo() {
    val density = LocalDensity.current
    val rmm = rememberGraphicsLayer()
    val position = remember { mutableStateOf(Offset.Zero) }
    val glassState = remember {
        LiquidGlassProviderState(
            graphicsLayer = rmm
        )
    }

//    println("--kaankaankaankaan--> ${DesktopCaptureBridge.hasScreenCapturePermission()}")

//    Box(
//        modifier = Modifier
//            // .drawWithCache {  }  xx Last where we left off, it was saying
//            //                       1. display (capture) desktop  2. display app content  3. apply then the refraction
//            .offset { IntOffset(position.value.x.roundToInt(), position.value.y.roundToInt()) }
//            .size(220.dp)
//            .graphicsLayer {
//                compositingStrategy = CompositingStrategy.Offscreen
//            }
//            .pointerInput(Unit) {
//                detectDragGestures { change, dragAmount ->
//                    position.value += dragAmount
//                    change.consume()
//                }
//            }
//            .onGloballyPositioned { coords ->
//                glassState.rect = coords.boundsInRoot()
//            }
////                            .background(Color.Yellow, RoundedCornerShape(24.dp))
//            .liquidGlass(
//                state = glassState,
//                style = LiquidGlassStyle(
//                    innerRefraction = InnerRefraction.Default,
//                    bleed = Bleed(
//                        amount = RefractionValue(4.dp),
//                        blurRadius = 28.dp,
//                        opacity = 0.8f
//                    ),
//                    material = GlassMaterial(
//                        blurRadius = 14.dp,
//                        tint = Color.White.copy(alpha = 0.4f),
//                        contrast = 0.4f,
//                        whitePoint = 0.2f,
//                        chromaMultiplier = 1.4f
//                    ),
//                    shape = RoundedCornerShape(24.dp),
//                )
//            )
//    )
}

internal object LiquidGlassShaders {

    private val colorShaderUtils = """
    const half3 rgbToY = half3(0.2126, 0.7152, 0.0722);

    float luma(half4 color) {
        return dot(toLinearSrgb(color.rgb), rgbToY);
    }"""

    internal val sdRectangleShaderUtils = """
    float sdRectangle(float2 coord, float2 halfSize) {
        float2 d = abs(coord) - halfSize;
        float outside = length(max(d, 0.0));
        float inside = min(max(d.x, d.y), 0.0);
        return outside + inside;
    }

    float sdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
        float2 innerHalfSize = halfSize - float2(cornerRadius);
        return sdRectangle(coord, innerHalfSize) - cornerRadius;
    }

    float2 gradSdRoundedRectangle(float2 coord, float2 halfSize, float cornerRadius) {
        float2 innerHalfSize = halfSize - float2(cornerRadius);
        float2 cornerCoord = abs(coord) - innerHalfSize;

        float insideCorner = step(0.0, min(cornerCoord.x, cornerCoord.y)); // 1 if in corner
        float xMajor = step(cornerCoord.y, cornerCoord.x); // 1 if x is major
        float2 gradEdge = float2(xMajor, 1.0 - xMajor);
        float2 gradCorner = normalize(cornerCoord);
        return sign(coord) * mix(gradEdge, gradCorner, insideCorner);
    }"""

    private val refractionShaderUtils = """
    $sdRectangleShaderUtils

    float circleMap(float x) {
        return 1.0 - sqrt(1.0 - x * x);
    }

    half4 refractionColor(float2 coord, float2 size, float cornerRadius, float eccentricFactor, float height, float amount) {
        float2 halfSize = size * 0.5;
        float2 centeredCoord = coord - halfSize;
        float sd = sdRoundedRectangle(centeredCoord, halfSize, cornerRadius);

        if (-sd >= height) {
            return image.eval(coord);
        }

        sd = min(sd, 0.0);
        float maxGradRadius = max(min(halfSize.x, halfSize.y), cornerRadius);
        float gradRadius = min(cornerRadius * 1.5, maxGradRadius);
        float2 normal = gradSdRoundedRectangle(centeredCoord, halfSize, gradRadius);

        float refractedDistance = circleMap(1.0 - -sd / height) * amount;
        float2 refractedDirection = normalize(normal + eccentricFactor * normalize(centeredCoord));
        float2 refractedCoord = coord + refractedDistance * refractedDirection;
        /*if (refractedCoord.x < 0.0 || refractedCoord.x >= size.x ||
            refractedCoord.y < 0.0 || refractedCoord.y >= size.y) {
            return half4(0.0, 0.0, 0.0, 1.0);
        }*/

        return image.eval(refractedCoord);
    }"""

    val refractionShaderWithBleedString = """
    uniform shader image;

    uniform float2 size;
    uniform float cornerRadius;

    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform float eccentricFactor;

    uniform float bleedOpacity;

    $colorShaderUtils
    $refractionShaderUtils

    half4 main(float2 coord) {
        half4 color = refractionColor(coord, size, cornerRadius, eccentricFactor, refractionHeight, refractionAmount);
        float luma = luma(color);
        color *= 1.0 - bleedOpacity * luma;
        return color;
    }"""

    val refractionShaderString = """
    uniform shader image;

    uniform float2 size;
    uniform float cornerRadius;

    uniform float refractionHeight;
    uniform float refractionAmount;
    uniform float eccentricFactor;

    $colorShaderUtils
    $refractionShaderUtils

    half4 main(float2 coord) {
        half4 color = refractionColor(coord, size, cornerRadius, eccentricFactor, refractionHeight, refractionAmount);
        return color;
    }"""

    val bleedShaderString = """
    uniform shader image;

    uniform float2 size;
    uniform float cornerRadius;

    uniform float eccentricFactor;
    uniform float bleedAmount;

    $colorShaderUtils
    $refractionShaderUtils

    half4 main(float2 coord) {
        half4 color = refractionColor(coord, size, cornerRadius, eccentricFactor, cornerRadius * 3.5, bleedAmount);
        float luma = luma(color);
        color.rgb = mix(color.rgb, half3(1.0), 0.5 * circleMap(1.0 - luma));
        return color;
    }"""

    val materialShaderString = """
    uniform shader image;

    uniform float contrast;
    uniform float whitePoint;
    uniform float chromaMultiplier;

    $colorShaderUtils

    half4 saturateColor(half4 color, float amount) {
        half3 linearSrgb = toLinearSrgb(color.rgb);
        float y = dot(linearSrgb, rgbToY);
        half3 gray = half3(y);
        half3 adjustedLinearSrgb = mix(gray, linearSrgb, amount);
        half3 adjustedSrgb = fromLinearSrgb(adjustedLinearSrgb);
        return half4(adjustedSrgb, color.a);
    }

    half4 main(float2 coord) {
        half4 color = image.eval(coord);

        color = saturateColor(color, chromaMultiplier);

        float3 target = float3(step(0.0, whitePoint));
        color.rgb = mix(color.rgb, target, abs(whitePoint));

        color.rgb = (color.rgb - 0.5) * (1.0 + contrast) + 0.5;

        return color;
    }"""
}

@Composable
fun rememberLiquidGlassProviderState(
    backgroundColor: Color?
): LiquidGlassProviderState {
    val graphicsLayer = rememberGraphicsLayer()
    return remember(backgroundColor, graphicsLayer) {
        LiquidGlassProviderState(
            graphicsLayer = graphicsLayer
        )
    }
}

@Stable
class LiquidGlassProviderState internal constructor(
    internal val graphicsLayer: GraphicsLayer,
    internal val capturedBitmap: MutableState<ImageBitmap?> = mutableStateOf(null)

) {

    internal var rect: Rect? by mutableStateOf(null)
}

@Immutable
data class LiquidGlassStyle(
    val shape: CornerBasedShape,
    val innerRefraction: InnerRefraction = InnerRefraction.Default,
    val material: GlassMaterial = GlassMaterial.Default,
//    val border: GlassBorder = GlassBorder.Default,
    val bleed: Bleed = Bleed.None
)


@Immutable
sealed interface Refraction {

    val height: RefractionValue

    val amount: RefractionValue
}

@Immutable
data class InnerRefraction(
    override val height: RefractionValue,
    override val amount: RefractionValue,
    val eccentricFactor: Float = 1f
) : Refraction {

    companion object {

        @Stable
        val Default: InnerRefraction =
            InnerRefraction(
                height = RefractionValue(8.dp),
                amount = RefractionValue((-16).dp),
                eccentricFactor = 1f
            )
    }
}

@Suppress("FunctionName")
@Stable
fun RefractionValue(value: Dp): RefractionValue.Fixed {
    return RefractionValue.Fixed(value)
}

@Immutable
sealed interface RefractionValue {

    @Stable
    fun toPx(density: Density, size: Size): Float

    @Immutable
    @JvmInline
    value class Fixed(val value: Dp) : RefractionValue {

        override fun toPx(density: Density, size: Size): Float {
            return with(density) { value.toPx() }
        }
    }

    @Immutable
    data object Full : RefractionValue {

        override fun toPx(density: Density, size: Size): Float {
            return -size.minDimension
        }
    }

    @Immutable
    data object Half : RefractionValue {

        override fun toPx(density: Density, size: Size): Float {
            return -size.minDimension / 2f
        }
    }

    @Immutable
    data object None : RefractionValue {

        override fun toPx(density: Density, size: Size): Float {
            return 0f
        }
    }
}

@Immutable
data class Bleed(
    val amount: RefractionValue = RefractionValue.None,
    val blurRadius: Dp = 0.dp,
    @param:FloatRange(from = 0.0, to = 1.0) val opacity: Float = 0f
) {

    companion object {

        @Stable
        val None: Bleed = Bleed()
    }
}

@Immutable
data class GlassMaterial(
    val blurRadius: Dp = 4.dp,
    val tint: Color = Color.Unspecified,
    @param:FloatRange(from = -1.0, to = 1.0) val contrast: Float = 0f,
    @param:FloatRange(from = -1.0, to = 1.0) val whitePoint: Float = 0f,
    @param:FloatRange(from = 0.0, to = 2.0) val chromaMultiplier: Float = 1.5f
) {

    companion object {

        @Stable
        val Default: GlassMaterial = GlassMaterial()
    }
}
// endregion

// region ────[ Caret Mod ]────────────────────────────────────────────────────────────
expect object TimeMark {
    fun nanoTime(): Long
}
// endregion

//  region ────[ Dynamic Island ]────────────────────────────────────────────────────────────
// xx  Ultimate Shaders / Engine for Fluid
//  Matsuoka's Fluid Render Engine
//   -->  https://www.reddit.com/r/GraphicsProgramming/comments/1jh3pd2/splash_a_realtime_fluid_simulation_in_browsers/
//  - Entire List of How to achieve Matsuoka's xtremely Advanced Fluid Render Engine
//      https://chatgpt.com/g/g-3X6EMarap-x5?model=gpt-4-5
//  - Matsuoka - Splash  https://github.com/matsuoka-601/Splash
//  1. Narrow-Range Fluid Edge
//  2. Vertex Particle Splash
//  3. Ray-marched Shadows
//
sealed class IslandState(
    val island: DpSize = DpSize(220.dp, 48.dp),
    val leading: Dp = 0.dp,
    val trailing: Dp = 0.dp,
    val bubble: DpSize = DpSize.Zero,
    val cutOff: Float = .66f
) {
    val hasBubble: Boolean get() = bubble.width > 0.dp

    data object Default : IslandState(
        bubble = DpSize(80.dp, 48.dp)
    )

    data object FaceUnlock : IslandState(
        bubble = DpSize(64.dp, 48.dp),
        island = DpSize(220.dp, 200.dp),
        cutOff = .25f
    )

    data object Split : IslandState(
        island = DpSize(220.dp, 42.dp),
        bubble = DpSize(64.dp, 48.dp),
        cutOff = .4f
    )
}

/**
 * Dynamic Island Composable
 * • Animated island with bubble
 * • Blur effect + MetaBall background
 * • Split state with offset bubble
 *
 * Usage:
 *   - Pass [IslandState] to control the island's state.
 *   - Provide [islandContent] and [bubbleContent] for custom content.
 *
 * Notes:
 *   - Uses `updateTransition` for smooth animations.
 *   - Supports blur effects and split states.
 *
 *                         💎✨
 *
 *
 *   Note from GPT4.5:
 *     -  use updateTransition combined with custom animateDpSize, providing a clean, scalable, and interruption-safe animation framework.
 *       ✅ Adaptive velocity: Springs adjust beautifully when interrupted.
 *       ✅ Fluid & realistic: Natural easing and bounce effects, no more teleports.
 *       ✅ Concise & idiomatic: Exactly how Compose recommends handling complex animations.
 */
// TODO: Bug1:      1..2 px border crop on Island, in split state  ( caused by Shader+Blur stack )
// TODO: Bug2:   (related to bug1?) UPGRADE TO 1.9.0-beta01 causes bubble to disappear at last millis
//                    https://chatgpt.com/g/g-3X6EMarap-x5/c/689c8825-a03c-832b-8031-50ebcb6262b4
// TODO: Upgrade:   coroutine based, interruption support for animations
@Composable
fun DynamicIsland(
    state: IslandState,
    modifier: Modifier = Modifier,
    blurRadius: Dp = 4.dp,
    splitOffset: Dp = 40.dp,
    islandSizeOverride: DpSize? = null,
    islandContent: @Composable BoxScope.() -> Unit = {},
    bubbleContent: @Composable BoxScope.() -> Unit = {}
) {
    val transition = updateTransition(state, label = "DynamicIslandTransition")

    // bubble + offset stay tied to state transitions
    val bubbleSize by transition.animateDpSize(label = "bubbleSize") { it.bubble }

    val animatedOffset by transition.animateDp(
        label = "bubbleOffset",
        transitionSpec = {
            spring(
                stiffness = Spring.StiffnessVeryLow,
                dampingRatio = 0.35f,
                visibilityThreshold = 0.5.dp
            )
        }
    ) { target ->
        when (target) {
            IslandState.Split -> splitOffset
            IslandState.FaceUnlock -> -IslandState.Split.bubble.width
            else -> 0.dp
        }
    }

    // fade ONLY foreground bubble content (not the blurred background)
    val bubbleContentAlpha by transition.animateFloat(
        label = "bubbleContentAlpha",
        transitionSpec = { tween(300, delayMillis = 1000) }
    ) { if (it is IslandState.Split) 1f else 0f }

    // smooth cutoff so shader doesn’t snap at settle
    val cutoff by transition.animateFloat(label = "cutoff") { it.cutOff }

    // island size: state-driven by default, override animates independently
    val stateIsland by transition.animateDpSize(label = "islandSizeFromState") { it.island }
    val targetIsland = islandSizeOverride ?: stateIsland
    val islandSize by animateValueAsState(
        targetValue = targetIsland,
        typeConverter = TwoWayConverter(
            convertToVector = { AnimationVector2D(it.width.value, it.height.value) },
            convertFromVector = { DpSize(it.v1.dp, it.v2.dp) }
        ),
        animationSpec = spring(
            stiffness = Spring.StiffnessVeryLow,
            dampingRatio = 0.45f
        ),
        label = "islandSizeAnim"
    )

    val totalWidth = islandSize.width + bubbleSize.width + splitOffset
    val totalHeight = max(islandSize.height, bubbleSize.height)

    MetaContainer(
        modifier.size(totalWidth, totalHeight),
        cutoff = cutoff
    ) {
        Box(contentAlignment = Alignment.Center) {

            // === single blurred background layer (both black shapes) ===
            // Explicit size so the bubble's OFFSET stays inside this layer's bounds.
            BlurField(
                islandSize = islandSize,
                bubbleSize = bubbleSize,
                bubbleOffset = animatedOffset,
                blurRadius = blurRadius
            )

            // Foreground content (safe to fade)
            Box(
                Modifier.size(islandSize),
                contentAlignment = Alignment.Center,
                content = islandContent
            )
            Box(
                Modifier
                    .size(bubbleSize)
                    .offset(x = islandSize.width / 2 + animatedOffset)
                    .alpha(bubbleContentAlpha),
                contentAlignment = Alignment.Center,
            ) {
                bubbleContent()
            }
        }
    }

    LaunchedEffect(state) { println("[DynamicIsland] state=$state") }
    LaunchedEffect(islandSize, bubbleSize, animatedOffset, bubbleContentAlpha, cutoff) {
        println("[DynamicIsland] island=$islandSize bubble=$bubbleSize x=${islandSize.width / 2 + animatedOffset} contentAlpha=$bubbleContentAlpha cutoff=$cutoff")
    }
}

/**
 * One pinned blurred layer that contains BOTH black shapes.
 * Critical: explicit layer size that accounts for motion + blur.
 */
@Composable
private fun BlurField(
    islandSize: DpSize,
    bubbleSize: DpSize,
    bubbleOffset: Dp,
    blurRadius: Dp
) {
    // Size the BLUR LAYER to fully include: island + bubble at max offset + blur bleed
    val bleed = 2.dp + blurRadius * 2
    val layerWidth = islandSize.width + bubbleSize.width + bubbleOffset + bleed * 2
    val layerHeight = max(islandSize.height, bubbleSize.height) + bleed * 2

    // We draw centered, so offsets are measured from the island center
    Box(
        Modifier
            .size(layerWidth, layerHeight)
            .graphicsLayer {
                // pin to prevent settle-time flatten/merge in 1.9
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .blurEffect(blurRadius),
        contentAlignment = Alignment.Center
    ) {
        // Island (opaque black; no alpha on blurred background)
        Box(
            Modifier
                .size(islandSize)
                .background(Color.Black, RoundedCornerShape(50))
        )
        // Bubble (opaque black; no alpha on blurred background)
        Box(
            Modifier
                .size(bubbleSize)
                .offset(x = islandSize.width / 2 + bubbleOffset)
                .background(Color.Black, CircleShape)
        )
    }

    // Visual sanity check;
    // Box(Modifier.size(layerWidth, layerHeight).border(1.dp, Color.Red))
}

@Composable
fun Transition<IslandState>.animateDpSize(
    label: String,
    target: @Composable (IslandState) -> DpSize
): State<DpSize> = animateValue(
    TwoWayConverter(
        { AnimationVector2D(it.width.value, it.height.value) },
        { DpSize(it.v1.dp, it.v2.dp) }
    ),
    label = label,
    targetValueByState = target,
    transitionSpec = {
        spring(
            stiffness = Spring.StiffnessVeryLow,
            dampingRatio = 0.45f,
            visibilityThreshold = DpSize(1.dp, 1.dp)
        )
    }
)

@Composable
expect fun Modifier.blurEffect(radius: Dp): Modifier

@Composable
expect fun MetaContainer(
    modifier: Modifier = Modifier,
    cutoff: Float = 0.5f,
    content: @Composable BoxScope.() -> Unit
)

@Stable
fun Modifier.islandAutosize(
    base: DpSize,
    maxWidth: Dp,
    maxHeight: Dp,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    onTarget: (DpSize) -> Unit
): Modifier = composed {
    var last by remember { mutableStateOf(IntSize(-1, -1)) }

    this.then(
        Modifier.layout { measurable, constraints ->
            if (constraints.maxWidth == 0 || constraints.maxHeight == 0) {
                val p = measurable.measure(constraints)
                return@layout layout(p.width, p.height) { p.place(0, 0) }
            }

            val baseW = base.width.roundToPx()
            val baseH = base.height.roundToPx()

            val capW = maxWidth.roundToPx().coerceAtLeast(1)
            val capH = maxHeight.roundToPx().coerceAtLeast(1)

            val padX = (contentPadding.calculateLeftPadding(layoutDirection) +
                    contentPadding.calculateRightPadding(layoutDirection)).roundToPx()
            val padY = (contentPadding.calculateTopPadding() +
                    contentPadding.calculateBottomPadding()).roundToPx()

            val heightHint = baseH.coerceAtMost(capH)

            // Padding-aware intrinsics: remove padding before sizing, add back after
            val oneLineWCore = (measurable.maxIntrinsicWidth(heightHint) - padX).coerceAtLeast(1)
            val targetWCore = oneLineWCore
                .coerceAtLeast((baseW - padX).coerceAtLeast(1))
                .coerceAtMost((capW - padX).coerceAtLeast(1))

            val wrappedHCore = (measurable.maxIntrinsicHeight(max(1, targetWCore)) - padY).coerceAtLeast(1)
            val targetHCore = wrappedHCore
                .coerceAtLeast((baseH - padY).coerceAtLeast(1))
                .coerceAtMost((capH - padY).coerceAtLeast(1))

            val targetW = targetWCore
            val targetH = targetHCore
            val oneLineW = oneLineWCore
            val wrappedH = wrappedHCore

            val out = IntSize(
                (targetW + padX).coerceAtLeast(1),
                (targetH + padY).coerceAtLeast(1)
            )

            if (out != last) {
                last = out
                onTarget(DpSize(out.width.toDp(), out.height.toDp()))
            }

            // RE-MEASURE WITH TARGET SIZE
            val targetConstraints = constraints.copy(
                minWidth = out.width,
                maxWidth = out.width,
                minHeight = out.height,
                maxHeight = out.height
            )

            val measuredChild = measurable.measure(targetConstraints)

            // Debugging for Performance ( Check against Stability of Island Resize compositions )
            println("🎯 heightHint: $heightHint (baseH: $baseH, capH: $capH)")
            println("🔍 oneLineW (maxIntrinsicWidth): $oneLineW")
            println("📌 targetW calculated: $targetW (baseW: $baseW, capW: $capW)")
            println("📐 wrappedH (maxIntrinsicHeight): $wrappedH")
            println("📌 targetH calculated: $targetH (baseH: $baseH, capH: $capH)")
            println("⚠️ out (IntSize) calculated: width=${targetW + padX}, height=${targetH + padY}")
            println("✅ targetConstraints: $targetConstraints")
            println("🚨 measuredChild size: ${measuredChild.width} x ${measuredChild.height}")

            layout(measuredChild.width, measuredChild.height) {
                measuredChild.place(0, 0)
            }
        }
    )
}

@Composable
fun Demo1DynamicIsland() {
    var state by remember { mutableStateOf<IslandState>(IslandState.Default) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        DynamicIsland(
            state = state,
            blurRadius = 8.dp,
            splitOffset = 40.dp,
            islandContent = {
                when (state) {
                    IslandState.FaceUnlock -> Icon(Icons.Default.Face, null, tint = Color.White, modifier = Modifier.size(48.dp))
                    IslandState.Split -> Text("🌴", fontSize = 20.sp, color = Color.White)
                    else -> {}
                }
            },
            bubbleContent = {
                if (state is IslandState.Split)
                    Text("⏳", fontSize = 20.sp, color = Color.White)
            }
        )

        Spacer(Modifier.height(40.dp))

        Row {
            Button(onClick = { state = IslandState.Default }) { Text("Default") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { state = IslandState.Split }) { Text("Split") }
            Spacer(Modifier.width(8.dp))
            Button(onClick = { state = IslandState.FaceUnlock }) { Text("Face") }
        }
    }
}

// endregion

// region ───[ Color Cloud Demo    |    Inner+Outer Shadow Demo ]────────────────────────────────────────────────────────────

//────[ Color Cloud ]────────────────────────────────────────────────────────────
@Composable
fun Demo3ColorClouds(
    colors: List<Color> = listOf(Color.Blue, Color.Yellow, Color.Red, Color.Magenta),
) {
    val displacement by rememberSweepAnim()
    val strokeWidthDp by rememberStrokeWidthAnim()
    val globalAlpha by rememberGlobalAlphaAnim()

    Box(
        Modifier
            .width(1280.dp)
            .height(300.dp)
            .background(
                color = Color.Transparent,
                shape = RoundedCornerShape(84.dp) // ← round corners without aggressive clipping
            )
            .colorClouds(
                shape = RoundedCornerShape(84.dp), // ← round corners without aggressive clipping
                colors = colors,
                strokeWidth = strokeWidthDp,
                displacementDegrees = displacement,
                coverageDegrees = 360f,
                fadeDegrees = 32f,
                globalAlpha = globalAlpha
            )
    )
}

@Composable
fun rememberSweepAnim(
    sweepPeriodMs: Int = 42_000,
    sweepRange: ClosedFloatingPointRange<Float> = 40f..920f,
    easing: Easing = FastOutSlowInEasing
): State<Float> {
    val transition = rememberInfiniteTransition(label = "sweep")
    return transition.animateFloat(
        initialValue = sweepRange.start,
        targetValue = sweepRange.endInclusive,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = sweepPeriodMs, easing = easing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "displacementDeg"
    )
}

@OptIn(ExperimentalAnimationSpecApi::class)
@Composable
fun rememberStrokeWidthAnim(
    min: Dp = 28.dp,
    max: Dp = 38.dp,
    periodMs: Int = 16_000
): State<Dp> {
    val transition = rememberInfiniteTransition(label = "stroke")
    val animatedFloat = transition.animateFloat(
        initialValue = min.value,
        targetValue = max.value,
        animationSpec = infiniteRepeatable(
            animation = ArcAnimationSpec(
                durationMillis = periodMs,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "strokeWidth"
    )
    return derivedStateOf { animatedFloat.value.dp }
}


@Composable
fun rememberGlobalAlphaAnim(
    min: Float = 0.20f,
    max: Float = 1f,
    periodMs: Int = 3_800
): State<Float> {
    val transition = rememberInfiniteTransition(label = "alpha")
    return transition.animateFloat(
        initialValue = min,
        targetValue = max,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = periodMs, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "globalAlpha"
    )
}

//────[ Inner & Outer Shadow ]────────────────────────────────────────────────────────────
@Composable
fun Demo4CustomShadows() {
    Box(
        Modifier
            .size(480.dp)
            .customShadow(
                outerShadows = listOf(
                    CustomShadow(
                        color = Color.Cyan.copy(alpha = 0.5f),
                        blur = 32.dp,
                        spread = 18.dp,
                        dx = (-20).dp,
                        dy = (-20).dp
                    ),
                    CustomShadow(
                        color = Color.Magenta.copy(alpha = 0.2f),
                        blur = 26.dp,
                        spread = 34.dp,
                        dx = (44).dp,
                        dy = (44).dp
                    )
                ),
                innerShadows = listOf(
                    CustomShadow(
                        color = Color.Black.copy(alpha = 0.5f),
                        blur = 14.dp,
                        inset = true,
                        dx = (-24).dp,
                        dy = (-38).dp
                    ),
                    CustomShadow(
                        color = Color.White.copy(alpha = 0.5f),
                        blur = 16.dp,
                        inset = true,
                        dx = 16.dp,
                        dy = 24.dp
                    ),
                    CustomShadow(
                        color = Color.Yellow.copy(alpha = 0.4f),
                        blur = 12.dp,
                        inset = true,
                        dx = 22.dp,
                        dy = 12.dp,
                        spread = 32.dp
                    )
                ),
                shape = RoundedCornerShape(32.dp)
            )
            .background(Color(0x000000).copy(alpha = 0.01f), RoundedCornerShape(32.dp))
    )
}

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






fun Modifier.reserveScaleBleed(maxScale: Float): Modifier =
    layout { measurable, constraints ->
        val p = measurable.measure(constraints)
        val dx = (((maxScale - 1f) * p.width) / 2f).roundToInt().coerceAtLeast(0)
        val dy = (((maxScale - 1f) * p.height) / 2f).roundToInt().coerceAtLeast(0)
        layout(p.width + dx * 2, p.height + dy * 2) {
            p.placeRelative(dx, dy)
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
    val interactionSource = remember { MutableInteractionSource() }

    val prog by animateFloatAsState(
        if (selected) 1f else 0f,
        spring(.55f, 80f), label = "glassProgress"
    )

    // Unified breathing animation for selected cards
    val breathingProgress by if (selected) {
        rememberInfiniteTransition(label = "breathing").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathingProgress"
        )
    } else {
        rememberUpdatedState(0f)
    }

    val breathingScale = 1f + breathingProgress * 0.02f
    val shadowOffset = breathingProgress * 2f

    val cardShape = RoundedCornerShape(style.radius)

    Box(
        modifier
            .padding(horizontal = 24.dp, vertical = 4.dp)
//            .reserveScaleBleed(1.02f)
            .glass(progress = prog, style = style)
            .graphicsLayer {
                scaleX = breathingScale
                scaleY = breathingScale
                clip = false
                compositingStrategy = CompositingStrategy.Offscreen
            }
            .then(
                if (selected) {
                    Modifier.innerShadow(cardShape) {
                        radius = 8f
                        spread = 2f
                        color = Color.Green
                        alpha = 2.25f
                        blendMode = BlendMode.SrcOver
                        offset = Offset(0f, shadowOffset)
                    }
                } else Modifier
            )
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    color = Color.Blue.copy(alpha = 0.6f),
                    bounded = false,
                ),
                onClick = onClick
            )
    ) {
        Box(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}


//@Composable                ORIGINAL VERSION
//fun GlassCard(
//    selected: Boolean,
//    modifier: Modifier = Modifier,
//    style: GlassStyle = GlassStyle(),
//    onClick: () -> Unit = {},
//    content: @Composable BoxScope.() -> Unit
//) {
//    val interactionSource = remember { MutableInteractionSource() }
//
//    val prog by animateFloatAsState(
//        if (selected) 1f else 0f,
//        spring(.55f, 80f), label = "glassProgress"
//    )
//    Box(
//        modifier
//            .glass(progress = prog, style = style)
//            .clickable(
//                interactionSource = interactionSource,
//                indication = ripple(
//                    color = Color.Blue.copy(alpha = 0.6f),
//                    bounded = false,
//                ),
//                onClick = onClick
//            ).then(modifier)
//    ) {
//        Box(
//            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 10.dp),
//            contentAlignment = Alignment.Center,
//            content = content
//        )
//    }
//}

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
            }.then(modifier),
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

// region ────[ Easers / Timers - Build Stops (knee etc..)   ( Actual Magic ) ]────────────────────────────────────────────────────────────
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
    darkA: Float = 1f,    // 100% at very bottom
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
            totalStops = 256,
            darkA = .995f,
            lightA = .02f        // just a ghost at the ceiling
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
            .blur(blurRadius)  // xx Unnecessary ? ask o3 , even w/ 64 stops it might be pixel perfect on desktop ?
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