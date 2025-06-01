package net.sdfgsdfg

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import net.sdfgsdfg.platform.LocalPlatformContext
import net.sdfgsdfg.platform.LocalWindowMetrics
import net.sdfgsdfg.platform.PlatformContext
import net.sdfgsdfg.platform.rememberWindowMetrics
import ui.login.AppDirs
import ui.login.DeepLinkHandler

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
