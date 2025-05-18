package net.sdfgsdfg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import net.sdfgsdfg.platform.LocalWindowMetrics
import net.sdfgsdfg.platform.rememberWindowMetrics

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
