package net.sdfgsdfg

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp

// for Bottom half,  Earth Rectangle
// Change to YELLOW for visual testing
fun Modifier.innerShadow() = then(
    drawWithContent {
        drawContent()  // Draw the original content (video)

        val gradient = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.97f)),
            center = Offset(size.width / 2, size.height / 2),
            tileMode = TileMode.Clamp,
            radius = size.maxDimension / 2,
        )

        // Apply the gradient over the entire rectangle of the video player
        drawRect(
            brush = gradient,
            size = size,
            topLeft = Offset.Zero  // Starts from the edges
        )
    }
)

fun Modifier.topGradientOverlay(videoTopYDp: Dp, density: Density) = this.then(
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