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
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.earth
import platform.Video
import platform.WindowMetrics
import ui.login.LoginScreen

private val VIDEO_HEIGHT = 500.dp

// TODO: window drags still cause flicker, only on window drag/autoResizes we suspend video composables, try it
@Composable
fun MainScreen(
    metrics: WindowMetrics,
    autoPlay: Boolean = true,
    isWindowMoving: Boolean = false,
) {
    println("-------------[ MainScreen ] -------------")

    MaterialTheme {
        ConstraintLayout(Modifier.fillMaxSize()) {
            val (content, video, gradient) = createRefs()

            // —————————————————  VIDEO  ——————————————————————————————
            Box(
                modifier = Modifier.constrainAs(video) {
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }.fillMaxWidth()
                    .height(VIDEO_HEIGHT)
                    .videoVignette()
            ) {
                if (isWindowMoving) // dumb hack required to fix AWT flicker on window drag
                    Box(Modifier.fillMaxSize().background(Color.Black))
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
                        Box(Modifier.fillMaxSize().background(Color.Black))
                    }
                }
            }

            // —————————————————  GradienT  ——————————————————————————
            Box(Modifier.constrainAs(gradient) {
                top.linkTo(parent.top)
                bottom.linkTo(video.top)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                height = Dimension.fillToConstraints
            }.fillMaxWidth()) {
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
            }) {
                LoginScreen {
                    println("onAuthenticated == [ ${it.user.name} ] ==")
                }
            }
        }
    }
}
