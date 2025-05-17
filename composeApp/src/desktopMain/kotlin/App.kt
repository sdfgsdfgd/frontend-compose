import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kaangpt_kotlin.composeapp.generated.resources.Res
import kaangpt_kotlin.composeapp.generated.resources.compose_multiplatform
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import utils.Progress
import utils.Video
import utils.innerShadow
import utils.topGradientOverlay

@OptIn(ExperimentalComposeUiApi::class)
@Composable
@Preview
fun MainApp() {
    // Allow AWT video to be overlaid with others
    System.setProperty("compose.interop.blending", "true")

    var showContent by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val videoHeight = 500.dp
    val desktopWidth = with(density) { windowInfo.containerSize.width.toDp() }
    val desktopHeight = with(density) { windowInfo.containerSize.height.toDp() }
    val videoTopY = desktopHeight - videoHeight

    // Debug logs
    println("Desktop size (windowInfo.containerSize):       ${windowInfo.containerSize}")
    println("windowInfo.containerSize.width px:             ${windowInfo.containerSize.width}")
    println("windowInfo.containerSize.height px:            ${windowInfo.containerSize.height}")
    println("desktopWidth:                                  $desktopWidth dp")
    println("desktopHeight:                                 $desktopHeight dp")
    println("Video Top Y:                                   $videoTopY dp")
    println("Density:                                       ${density.density}")
    //xx ==============

    println("---------------[ Width x Height] ------------[ ${desktopWidth} ]----------------[ ${desktopHeight} ]-----------------")

    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
//                .innerShadow()
        ) {
            // Remaining 1/3 above the Video, to the top of the screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(videoTopY)
                    .topGradientOverlay(videoTopY, density)
            )

            // Bottom 2/3, the Video
            Video(
                url = "file:////Users/x/Desktop/earth.mp4",
                isResumed = true,
                volume = 1f,
                speed = 1f,
                seek = 0f,
                isFullscreen = false,
                progressState = mutableStateOf(Progress(0f, 100L)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(600.dp)
//                    .height(desktopHeight)
//                    .shadow(10.dp, RoundedCornerShape(10.dp))
                    .innerShadow()
                    .align(Alignment.BottomCenter),
                onFinish = {}
            )

            Box(
                modifier = Modifier.matchParentSize(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    Modifier.padding(top = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        modifier = Modifier.padding(all = 4.dp).align(Alignment.CenterHorizontally),
                        onClick = { showContent = !showContent }
                    ) {
                        Text("Click me!")
                    }

                    AnimatedVisibility(
                        visible = showContent,
                        enter = fadeIn() + expandVertically() // slide, fade, shrink, expand
                    ) {
                        val greeting = remember { "Hello, World!" } // Replace with your actual greeting logic
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(painterResource(Res.drawable.compose_multiplatform), null)
                            Text("Greeting: $greeting", color = Color.DarkGray, fontSize = 48.sp, fontFamily = FontFamily.Cursive)
                        }
                    }
                    Button(
                        modifier = Modifier.padding(all = 4.dp).align(Alignment.CenterHorizontally),
                        onClick = { showSettings = !showSettings }
                    ) {
                        Text("Settings")

                    }
                }
            }
        }
    }
}
