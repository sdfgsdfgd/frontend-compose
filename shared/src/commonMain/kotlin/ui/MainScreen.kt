package ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.earth
import platform.Video
import platform.WindowMetrics
import ui.login.LoginScreen

// TODO: window drags still cause flicker, only on window drag/autoResizes we suspend video composables, try it
@Composable
fun MainScreen(
    metrics: WindowMetrics,
    autoPlay: Boolean = true,
    isWindowMoving: Boolean = false,
) {
    val winHeightDp = (metrics.sizeDp.height.takeIf { it > 0.dp }
        ?: LocalWindowInfo.current.containerSize.height.dp)

    val videoHeight = 500.dp
    val gapHeight = (winHeightDp - videoHeight).coerceAtLeast(0.dp)
    println("[ kaankaan ] -------> gapHeight: [ $gapHeight ] ------- winHeightDp [ $winHeightDp ] ------ videoHeight [ $videoHeight ]")

    MaterialTheme {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (content, video, gradient) = createRefs()

            // —————————————————  VIDEO  ——————————————————————————————
            Box(
                modifier = Modifier.constrainAs(video) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
                    .fillMaxWidth()
                    .height(videoHeight)
                    .videoVignette()
                    .zIndex(1f)
            ) {
                if (isWindowMoving) // dumb hack required to fix AWT flicker on window drag
                    Box(Modifier.fillMaxSize().zIndex(2f).background(Color.Black))
                Crossfade(
                    targetState = isWindowMoving,
                    animationSpec = tween(durationMillis = 990),
                    modifier = Modifier.fillMaxSize()
                ) { moving ->
                    if (!moving) {
                        Video(
                            source = Res.drawable.earth,
                            modifier = Modifier.fillMaxSize(),
                            autoPlay = autoPlay
                        )
                    } else {
                        Box(
                            Modifier.fillMaxSize().zIndex(2f)
                                .background(Color.Black)
                        )
                    }
                }
            }

            // —————————————————  GradienT  ——————————————————————————
            Box(
                Modifier
                    .constrainAs(gradient) {
                        top.linkTo(parent.top)
                        bottom.linkTo(video.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        height = Dimension.fillToConstraints
                    }.zIndex(1f).fillMaxWidth().height(gapHeight)
            ) {
                FadeStrip(blurRadius = 24.dp)
            }

            // —————————————————  CONTENT  ——————————————————————————
            Box(contentAlignment = Alignment.Center, modifier = Modifier.constrainAs(content) {
                top.linkTo(parent.top)
                bottom.linkTo(parent.bottom)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
                height = Dimension.fillToConstraints
            }.zIndex(2f)) {
                LoginScreen(modifier = Modifier) {
                    println("onAuthenticated == [ ${it.user.name} ] ==")
                }
            }
        }
    }
}
