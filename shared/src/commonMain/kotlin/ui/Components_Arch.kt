package net.sdfgsdfg.ui


import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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
@Composable
fun MetalPowerButton() {
    var isPressed  by remember { mutableStateOf(false) }
    var poweredOn  by remember { mutableStateOf(false) }
    val pressAnim  by animateFloatAsState(
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
                        radius  = size.minDimension * .6f
                    ),
                    blendMode = BlendMode.Overlay
                )
            }
            // 3️⃣ subtle outer ring
            .border(4.dp, Brush.verticalGradient(
                listOf(Color(0xFF2C2C2C), Color(0xFF3C3C3C))), CircleShape)
            // 4️⃣ highlight ring
            .border(1.dp, Brush.verticalGradient(
                listOf(Color.White, Color.Transparent)), CircleShape)
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
                    val center = Offset(center.x * size.width, center.y * size.height)
                    drawRect(color = baseColor)
                    drawRect(
                        brush = Brush.radialGradient(
                            colors = ringColors,
                            tileMode = TileMode.Repeated,
                            center = center,
                            radius = size.width * .2f,
                        ),
                        blendMode = BlendMode.Overlay,
                    )
                    rotate(
                        degrees = highlightRotation,
                        pivot = center
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
                                center = center
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