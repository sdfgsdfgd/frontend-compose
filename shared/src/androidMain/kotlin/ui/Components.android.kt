package ui

import android.graphics.RuntimeShader
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush

@Composable
actual fun rememberShader(
    press: Float,          // ← no longer used, keep for signature
    sweep: Float,
): Brush = remember(sweep) {

    /* ── Android 13+ : AGSL sweep bar ─────────────────────────────── */
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // language=AGSL
        val src = """
            uniform float2 res;   // button width/height  (px)
            uniform float  sweep; // 0‥1  x-position

            half4 main(float2 frag) {
                float x = frag.x / res.x;
                half  a = smoothstep(sweep - .01, sweep + .01, x) * .8;
                return half4(1.0, 1.0, 1.0, a);  // white streak, α-fade
            }
        """.trimIndent()

        val shader = RuntimeShader(src)

        return@remember object : ShaderBrush() {
            override fun createShader(size: Size): Shader = shader.apply {
                setFloatUniform("res",   size.width, size.height)
                setFloatUniform("sweep", sweep)
            }
        }
    }

    /* ── Pre-33 fallback : cheap static linear gradient ───────────── */
    Brush.linearGradient(
        colors  = listOf(
            Color.White.copy(alpha = 0.0f),
            Color.White.copy(alpha = 0.4f),
            Color.White.copy(alpha = 0.0f)
        )
    )
}
