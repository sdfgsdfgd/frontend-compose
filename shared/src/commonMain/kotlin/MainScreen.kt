package net.sdfgsdfg

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import net.sdfgsdfg.platform.Video
import net.sdfgsdfg.platform.WindowMetrics
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.earth
import net.sdfgsdfg.ui.home.GapFadeStrip
import net.sdfgsdfg.ui.home.temporaryOverlays
import net.sdfgsdfg.ui.home.videoVignette

@Composable
fun MainScreen(
    metrics: WindowMetrics,
    autoPlay: Boolean = true
) {
    val winHeightDp = (metrics.sizeDp.height.takeIf { it > 0.dp }
        ?: LocalWindowInfo.current.containerSize.height.dp)

    val videoHeight = 500.dp
    val gapHeight = max(0.dp, winHeightDp - videoHeight)
    println("[ kaankaan ] -------> gapHeight: [ $gapHeight ] ------- winHeightDp [ $winHeightDp ] ------ videoHeight [ $videoHeight ]")

    MaterialTheme {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (video, gradient) = createRefs()

            // —————————————————  VIDEO  ——————————————————————————————
            Box(
                modifier = Modifier.constrainAs(video) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }.fillMaxWidth()
                    .height(videoHeight)
                    .videoVignette()
            ) {
                Video(
                    source = Res.drawable.earth,
                    modifier = Modifier.fillMaxSize(),
                    autoPlay = autoPlay
                )
            }

            // —————————————————  GRADIENT  ——————————————————————————
            Box(
                modifier = Modifier.constrainAs(gradient) {
                    top.linkTo(parent.top)
                    bottom.linkTo(video.top)          // bottom flush with video’s top edge
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.fillToConstraints   // stretch to cover the whole gap
                }.fillMaxWidth()
            ) {
                GapFadeStrip(blurRadius = 12.dp)
            }
        }

        temporaryOverlays()
    }
}

