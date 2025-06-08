package ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.earth
import platform.Video
import platform.WindowMetrics
import ui.login.LoginScreen

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
            val (content, video, gradient) = createRefs()

            // —————————————————  CONTENT  ——————————————————————————
            // TODO: Navigation wrapper instead of LoginScreen, or a container that crossfades sequentially
            LoginScreen(
                modifier = Modifier.constrainAs(content) {
                    top.linkTo(parent.top)
                    bottom.linkTo(video.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.matchParent
                    verticalBias = 0.35f
                }.zIndex(1f)
            ) {
                println("onAuthenticated  === ===[ ${it.user.name} ]=== ===")
            }

            // —————————————————  VIDEO  ——————————————————————————————
            Box(
                modifier = Modifier.constrainAs(video) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }.fillMaxWidth()
                    .height(videoHeight)
                    .videoVignette()
//                    .aspectRatio(matchHeightConstraintsFirst = true, ratio = 16f / 9f)
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
                    bottom.linkTo(video.top)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    height = Dimension.fillToConstraints
                }.fillMaxWidth()
            ) {
                FadeStrip(blurRadius = 24.dp)
            }
        }
//        temporaryOverlays()
    }
}
