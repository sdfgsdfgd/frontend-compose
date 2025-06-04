package net.sdfgsdfg.ui

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
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import kotlin.random.Random

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
@Suppress("unused")
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

// region Customized Shadow Button
@Immutable
data class Shadow(
    val color: Color = Color.Black,
    val blur: Dp = 8.dp,
    val spread: Dp = 0.dp,
    val dx: Dp = 0.dp,
    val dy: Dp = 4.dp,
    val inset: Boolean = false,
)

/**
 *
 * Usage :
 *
 * Box(
 *     Modifier
 *         .size(160.dp)
 *         .boxShadow(
 *             Shadow(color = Color.Black.copy(.35f), blur = 16.dp, spread = 4.dp, dy = 8.dp),
 *             Shadow(color = Color.White.copy(.45f), blur = 10.dp, inset = true, dy = (-2).dp),
 *             shape = RoundedCornerShape(32.dp)
 *         )
 *         .background(Color(0xFF2A2A2A), RoundedCornerShape(32.dp))
 * )
 *
 *
 *           + also:
 *
 *
 *    Animate any knob (blur, dx, dy, spread, color) with animate*AsState; the modifier keeps up.
 *
 */

/** Attach any number of [Shadow]s to this node – zero platform code. */
fun Modifier.shadowCustom(
    vararg shadows: Shadow,
    shape: Shape = RectangleShape,
    clipContent: Boolean = false,
): Modifier = this.then(
    Modifier.drawWithContent {
        // 1) OUTER shadows – fake by drawing coloured rects, then blurring them
        shadows.filter { !it.inset }.forEach { s ->
            drawIntoCanvas { canvas ->
                val spreadPx = s.spread.toPx()
                val rect = Rect(
                    left = -spreadPx + s.dx.toPx(),
                    top = -spreadPx + s.dy.toPx(),
                    right = size.width + spreadPx + s.dx.toPx(),
                    bottom = size.height + spreadPx + s.dy.toPx()
                )
                canvas.save()
                canvas.clipPath(Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) }, ClipOp.Difference)
                canvas.drawRect(rect, Paint().apply { color = s.color })
                canvas.restore()
            }
        }

        // 2) draw original content
        drawContent()

        // 3) INNER shadows – draw coloured rect *inside*, invert with saveLayer α-inversion, blur
        shadows.filter { it.inset }.forEach { s ->
            val spreadPx = s.spread.toPx()
            val insetRect = Rect(
                left = spreadPx + s.dx.toPx(),
                top = spreadPx + s.dy.toPx(),
                right = size.width - spreadPx + s.dx.toPx(),
                bottom = size.height - spreadPx + s.dy.toPx()
            )

            drawIntoCanvas { canvas ->
                // isolate
                canvas.saveLayer(bounds = insetRect, paint = Paint())
                // hole punch
                canvas.drawRect(insetRect, Paint().apply { color = s.color })
                // keep only intersection
                canvas.clipPath(Path().apply { addOutline(shape.createOutline(size, layoutDirection, this@drawWithContent)) })
                canvas.restore()
            }
        }
    }.blur(   // one blur pass per *layer*; keep radii small for perf
        radius = shadows.maxOfOrNull { it.blur } ?: 0.dp,
        edgeTreatment = BlurredEdgeTreatment.Unbounded
    )
).let { if (clipContent) it.clip(shape) else it }

// endregion

// region Button with custom shadow
@Composable
fun ButtonCustomShadow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)

    /* 1 - shadows */
    val outerGlow = Shadow(
        color = Color.White.copy(alpha = .55f),
        blur = 24.dp,         // softness
        spread = 6.dp,          // reaches further out
        dy = (-2).dp,       // slight top bias
    )
    val innerDrop = Shadow(
        color = Color.Black.copy(alpha = .45f),
        blur = 14.dp,
        inset = true,
        dy = 2.dp
    )

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
            .shadowCustom(outerGlow, innerDrop, shape = shape)
            .background(bodyGrad, shape)
            .border(1.dp, Color.White.copy(alpha = .12f), shape) // ← chef’s-kiss bevel
            .drawWithContent {
                drawContent()                   // button fill
                drawRoundRect(                 // glossy strip
                    brush = gloss,
                    cornerRadius = CornerRadius(28.dp.toPx())
                )
            }
            .clickable(onClick = onClick)
            .padding(horizontal = 32.dp, vertical = 18.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), color = Color.White, fontSize = 18.sp)
    }
}
// endregion