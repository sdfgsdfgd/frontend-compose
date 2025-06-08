package net.sdfgsdfg

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import platform.AppDirs
import platform.DeepLinkHandler
import platform.LocalPlatformContext
import platform.LocalWindowMetrics
import platform.PlatformContext
import platform.rememberWindowMetrics
import ui.MainScreen

@Preview
@Composable
fun AppAndroidPreview() {
    MainActivity()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        AppDirs.init(this)

        setContent {
            CompositionLocalProvider(
                LocalPlatformContext provides PlatformContext(this),
                LocalWindowMetrics provides rememberWindowMetrics(),
            ) {
                MainScreen(
                    metrics = LocalWindowMetrics.current,
                    autoPlay = true
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.dataString?.let(DeepLinkHandler::onNewUri)
    }
}
