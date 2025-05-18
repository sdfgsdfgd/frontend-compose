package net.sdfgsdfg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import net.sdfgsdfg.platform.AndroidVideo
import net.sdfgsdfg.platform.LocalWindowMetrics
import net.sdfgsdfg.platform.rememberWindowMetrics
import net.sdfgsdfg.platform.toPlayablePath
import net.sdfgsdfg.resources.Res
import net.sdfgsdfg.resources.earth

@Preview
@Composable
fun AppAndroidPreview() {
    MainActivity()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(
                LocalWindowMetrics provides rememberWindowMetrics()
            ) {
                MainScreen(
                    metrics = LocalWindowMetrics.current,
                    autoPlay = true
                )
            }
        }

    }
}
