package ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.random.Random

// region The Cheatsheet of Layman

/**
 * | Modifier                                 | Layer          | Use‑Case                                                                                             |
 * | ---------------------------------------- | -------------- | ---------------------------------------------------------------------------------------------------- |
 * | `drawBehind { ... }`                     | Behind         | Backgrounds, halos under content, base shapes                                                        |
 * | `drawWithContent { drawContent(); ... }` | Front          | Overlays, press effects, glow, adornment                                                             |
 * | `drawWithCache { ... }`                  | Behind/Front\* | Cache brushes, gradients, paths. Use `onDrawBehind` or `onDrawWithContent`. Efficient heavy drawing. |
 * | `graphicsLayer { ... }`                  | Layer          | Shadows, blur, elevation, transforms, opacity masking                                                |
 * | `clip(shape)`                            | Mask           | Constrain drawing inside a shape                                                                     |
 * | `pointerInput { ... }`                   | Input          | Gesture-based custom painting                                                                        |
 * | `layout { ... }` or `Modifier.layout`    | Layout         | Custom measurement, size, offset manipulation                                                        |
 *
 */
// endregion

// region    ⏻   MetalPowerButton   ⏻
/**
 *  Simple container
 *
 * Box(modifier = Modifier.width(364.dp).background(Color(0xFF121212)).padding(12.dp)
 *             .zIndex(12f)
 *             .constrainAs(bottom) {
 *                 top.linkTo(topConstrain.bottom)
 *                 start.linkTo(parent.start, margin = 12.dp)
 *             }) {
 *             MetalPowerButton()
 *         }
 */
@Composable
fun MetalPowerButton() {
    var isPressed by remember { mutableStateOf(false) }
    var poweredOn by remember { mutableStateOf(false) }
    val pressAnim by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh)
    )

    Box(
        Modifier
            .size(128.dp)
            .graphicsLayer {
                val s = lerp(1f, .95f, pressAnim)
                scaleX = s; scaleY = s
            }
            // 1️⃣ texture FIRST
            .brushedMetal(
                baseColor = Color(0xFF3C3C3C),
                shape = CircleShape,
                highlightRotation = 0f        // hook up your angle here
            )
            // 2️⃣ radial press shadow
            .drawWithContent {
                drawContent()
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = .6f * pressAnim)),
                        center = center,
                        radius = size.minDimension * .6f
                    ),
                    blendMode = BlendMode.Overlay
                )
            }
            // 3️⃣ subtle outer ring
            .border(
                4.dp, Brush.verticalGradient(
                    listOf(Color(0xFF2C2C2C), Color(0xFF3C3C3C))
                ), CircleShape
            )
            // 4️⃣ highlight ring
            .border(
                1.dp, Brush.verticalGradient(
                    listOf(Color.White, Color.Transparent)
                ), CircleShape
            )
            // 5️⃣ hit-test
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                        poweredOn = !poweredOn
                    }
                )
            }
    ) {
        Icon(
            imageVector = PowerSettingsIcon,
            contentDescription = null,
            tint = Color(0x22E8EAED),
            modifier = Modifier.size(64.dp).align(Alignment.Center)
        )

        // LED
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
                .size(10.dp)
                .background(Color(0xFF2C2C2C), CircleShape)
        ) {
            if (poweredOn) {
                Box(
                    Modifier.align(Alignment.Center)
                        .size(4.dp)
                        .background(Color.Green, CircleShape)
                )
                Box(
                    Modifier.align(Alignment.Center)
                        .size(12.dp)
                        .blur(8.dp)
                        .background(Color.Green, CircleShape)
                )
            }
        }
    }
}

@Composable
fun Modifier.brushedMetal(
    baseColor: Color = Color(0xFF9A9A9A),
    shape: Shape = RectangleShape,
    ringAlpha: Float = .2f,
    ringCount: Int = 40,
    highlightAlpha: Float = .5f,
    highlightCount: Int = 3,
    highlightRotation: Float = 0f,
    center: Offset = Offset(.5f, .5f),
): Modifier {
    val highlightColor = remember(baseColor, highlightAlpha) {
        lerp(baseColor, Color.White, .5f).copy(alpha = highlightAlpha)
    }
    val ringColors = remember(ringCount) {
        val ringColor = lerp(baseColor, Color.Black, .5f).copy(alpha = ringAlpha)
        buildList {
            (0..ringCount).forEach {
                (0..Random.nextInt(2, 19)).forEach { add(Color.Transparent) }
                (0..Random.nextInt(0, 3)).forEach { add(ringColor) }
            }
        }
    }
    return this
        .drawWithCache {
            val path = Path().apply {
                addOutline(
                    shape.createOutline(size, layoutDirection, Density(density))
                )
            }
            onDrawBehind {
                clipPath(path) {
                    val centerCircle = Offset(center.x * size.width, center.y * size.height)
                    drawRect(color = baseColor)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = ringColors,
                            tileMode = TileMode.Repeated,
                            center = centerCircle,
                            radius = size.width * .2f,
                        ),
                        blendMode = BlendMode.Overlay,
                    )
                    rotate(
                        degrees = highlightRotation,
                        pivot = centerCircle
                    ) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = buildList {
                                    add(highlightColor)
                                    repeat(highlightCount) {
                                        add(highlightColor.copy(alpha = 0f))
                                        if (it < highlightCount - 1) add(highlightColor)
                                    }
                                    add(highlightColor)
                                },
                                center = centerCircle
                            ),
                            radius = size.width * size.height
                        )
                    }
                }
            }
        }
}

val PowerSettingsIcon: ImageVector
    get() {
        if (_powerSettingsIcon != null) {
            return _powerSettingsIcon!!
        }
        _powerSettingsIcon = Builder(
            name = "Power settings icon", defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp, viewportWidth = 24.0f, viewportHeight = 24.0f
        ).apply {
            path(
                fill = SolidColor(Color(0xFFe8eaed)), stroke = null, strokeLineWidth = 0.0f,
                strokeLineCap = Butt, strokeLineJoin = Miter, strokeLineMiter = 4.0f,
                pathFillType = NonZero
            ) {
                moveTo(12.0f, 3.0f)
                curveToRelative(-0.55f, 0.0f, -1.0f, 0.45f, -1.0f, 1.0f)
                verticalLineToRelative(8.0f)
                curveToRelative(0.0f, 0.55f, 0.45f, 1.0f, 1.0f, 1.0f)
                reflectiveCurveToRelative(1.0f, -0.45f, 1.0f, -1.0f)
                lineTo(13.0f, 4.0f)
                curveToRelative(0.0f, -0.55f, -0.45f, -1.0f, -1.0f, -1.0f)
                close()
                moveTo(17.14f, 5.86f)
                curveToRelative(-0.39f, 0.39f, -0.38f, 1.0f, -0.01f, 1.39f)
                curveToRelative(1.13f, 1.2f, 1.83f, 2.8f, 1.87f, 4.57f)
                curveToRelative(0.09f, 3.83f, -3.08f, 7.13f, -6.91f, 7.17f)
                curveTo(8.18f, 19.05f, 5.0f, 15.9f, 5.0f, 12.0f)
                curveToRelative(0.0f, -1.84f, 0.71f, -3.51f, 1.87f, -4.76f)
                curveToRelative(0.37f, -0.39f, 0.37f, -1.0f, -0.01f, -1.38f)
                curveToRelative(-0.4f, -0.4f, -1.05f, -0.39f, -1.43f, 0.02f)
                curveTo(3.98f, 7.42f, 3.07f, 9.47f, 3.0f, 11.74f)
                curveToRelative(-0.14f, 4.88f, 3.83f, 9.1f, 8.71f, 9.25f)
                curveToRelative(5.1f, 0.16f, 9.29f, -3.93f, 9.29f, -9.0f)
                curveToRelative(0.0f, -2.37f, -0.92f, -4.51f, -2.42f, -6.11f)
                curveToRelative(-0.38f, -0.41f, -1.04f, -0.42f, -1.44f, -0.02f)
                close()
            }
        }
            .build()
        return _powerSettingsIcon!!
    }

private var _powerSettingsIcon: ImageVector? = null


// endregion

// region  [ Custom Shadow ]   Modifier   ( on anything )
@Immutable
data class CustomShadow(
    val color: Color = Color.Black,
    val blur: Dp = 8.dp,      // softness
    val spread: Dp = 0.dp,      // grow / shrink before blur
    val dx: Dp = 0.dp,      // X-offset
    val dy: Dp = 4.dp,      // Y-offset
    val inset: Boolean = false    // false = outer, true = inner
)

expect fun DrawScope.customShadow(
    customShadow: CustomShadow,
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    isInner: Boolean
)

// xx Before Deprecate&Delete keep the custom version here in case maybe there is need for a custom knobs ....
// ──  ( 1 ) drawShadow implementation ────────────────────────────────────────────────
fun Modifier.customShadow(
    innerShadows: List<CustomShadow> = emptyList(),
    outerShadows: List<CustomShadow> = emptyList(),
    shape: Shape = RectangleShape // or RoundedCornerShape(12.dp), CircleShape, etc.
) = this
    .drawBehind {
        outerShadows.forEach {
            customShadow(it, shape, size, layoutDirection, isInner = false)
        }
    }
    .drawWithContent {
        drawContent()
        innerShadows.forEach {
            customShadow(it, shape, size, layoutDirection, isInner = true)
        }
    }

@Composable
fun ButtonCustomShadow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)

    /* 2 - body gradient + gloss */
    val bodyGrad = Brush.verticalGradient(
        listOf(Color(0xFF474747), Color(0xFF2E2E2E))
    )
    val gloss = Brush.verticalGradient(
        0f to Color.White.copy(alpha = .35f),
        .45f to Color.Transparent,
        1f to Color.Transparent
    )

    Box(
        modifier
            .customShadow(
                outerShadows = listOf(
                    CustomShadow(
                        color = Color.White.copy(alpha = .25f),
                        blur = 12.dp,         // softness
                        spread = 8.dp,        // reaches further out
                        dx = (-8).dp,
                        dy = (-8).dp,         // slight top bias
                    )
                ),
                innerShadows = listOf(
                    CustomShadow(
                        color = Color.Black.copy(alpha = .85f),
                        blur = 12.dp,
                        inset = true,
                        dy = 12.dp,
                        dx = 8.dp,
                    ),
                    CustomShadow(
                        color = Color.White.copy(alpha = .45f),
                        blur = 8.dp,
                        inset = true,
                        dy = (-6).dp,
                        dx = (-6).dp,
                    )
                ),
                shape = shape
            )
            .background(bodyGrad, shape)
            .border(1.dp, Color.White.copy(alpha = .12f), shape) // ← chef’s-kiss bevel  ( o3:  0.08 - 0.18 is the sweet spot )
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), color = Color.White, fontSize = 18.sp)
    }
}
// endregion

// region Color Cloud
/**
 *
 * ──  ( 2 ) color cloud ─────────────────────────────────────────────
 *
 *
 * Applies a dynamically displaced, softly faded sweep gradient outline ("cloud-like" effect) around the given shape.
 *
 * Gradient fades smoothly in and out for each color, creating gentle transitions rather than sharp boundaries.
 *
 * @param colors List of colors evenly distributed around the sweep gradient.
 * @param strokeWidth Thickness of the gradient outline.
 * @param shape Shape of the outline onto which the gradient is applied.
 * @param coverageDegrees Angular span (in degrees) of the gradient around the shape (≤360°).
 * @param displacementDegrees Rotation of the gradient, offsetting its starting point.
 * @param fadeDegrees Degrees used for smooth fading transitions of each color segment.
 */
fun Modifier.colorClouds(
    colors: List<Color>,
    strokeWidth: Dp = 32.dp,
    shape: Shape = RoundedCornerShape(8.dp),
    coverageDegrees: Float = 310f,
    displacementDegrees: Float = 105f,
    fadeDegrees: Float = 32f,
    globalAlpha: Float,
): Modifier = padding(strokeWidth * 1.5f)
    .graphicsLayer {
        alpha = globalAlpha
        clip = false
        compositingStrategy = CompositingStrategy.Offscreen
    }.drawWithCache {
        val strokePx = strokeWidth.toPx()

        // ⭐️ MAGIC FIX RIGHT HERE: EXPAND PATH TO ACCOMMODATE THE BLUR ⭐️
        val blurPadding = strokePx * 0.4f
        val expandedSize = Size(size.width + blurPadding, size.height + blurPadding)
        val center = Offset(expandedSize.width / 2f, expandedSize.height / 2f)

//    xx      val center = Offset(size.width / 2f, size.height / 2f)     // REPLACED OLD ONE

        val sweepFraction = coverageDegrees.coerceAtMost(360f) / 360f
        val fadeFraction = fadeDegrees / 360f
        val displacementFraction = (displacementDegrees % 360f) / 360f

        val gradientStops = mutableListOf<Pair<Float, Color>>()

        val segmentFraction = sweepFraction / colors.size

        colors.forEachIndexed { i, color ->
            val startFraction = i * segmentFraction
            val endFraction = (i + 1) * segmentFraction

            // Smooth fade-in
            gradientStops += startFraction to color.copy(alpha = 0f)
            gradientStops += (startFraction + fadeFraction * 0.5f) to color.copy(alpha = 0.08f)  // intermediate step!
            gradientStops += (startFraction + fadeFraction) to color.copy(alpha = 0.3f)

            // Stable mid-color ( The Knee )
            gradientStops += ((startFraction + endFraction) / 2) to color.copy(alpha = 0.4f)

            // Smooth fade-out
            gradientStops += (endFraction - fadeFraction) to color.copy(alpha = 0.3f)
            gradientStops += (endFraction - fadeFraction * 0.5f) to color.copy(alpha = 0.08f)   // intermediate step!
            gradientStops += endFraction to color.copy(alpha = 0f)
        }

        val normalizedStops = gradientStops.flatMap { (fraction, color) ->
            val displacedFraction = (fraction + displacementFraction) % 1f
            listOf(
                displacedFraction to color,
                displacedFraction + 1f to color
            )
        }.sortedBy { it.first }

        // Sweep gradient brush centered within the component
        val gradientBrush = Brush.sweepGradient(
            colorStops = normalizedStops.toTypedArray(),
            center = center
        )

        val outlinePath = Path().apply {
            addOutline(shape.createOutline(size, layoutDirection, this@drawWithCache))
            translate(Offset(blurPadding / 2f, blurPadding / 2f))
        }

        onDrawBehind {
            drawCombinedGradientStroke(
                path = outlinePath,
                sweepBrush = gradientBrush,
                strokeWidthPx = strokePx,
                shapeSize = expandedSize,
                shape = shape
            )
        }
    }

expect fun DrawScope.drawCombinedGradientStroke(
    path: Path,
    sweepBrush: Brush,
    strokeWidthPx: Float,
    shapeSize: Size,
    shape: Shape
)

// endregion
