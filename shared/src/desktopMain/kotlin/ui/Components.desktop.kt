package net.sdfgsdfg.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ShaderBrush
import org.jetbrains.skia.Data
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.Shader
import java.nio.ByteBuffer
import java.nio.ByteOrder

@Composable
actual fun rememberShader(
    press: Float,   // unused, kept for signature parity
    sweep: Float
): Brush = remember(sweep) {

    // language=AGSL
    val src = """
        uniform float2 res;    // px
        uniform float  sweep;  // 0‥1

        half4 main(float2 frag) {
            float x = frag.x / res.x;
            float d = abs(x - sweep);                 // distance to bar
            half  a = clamp((.01 - d) / .01, 0.0, 1.0) * .8; // 2 % total width
            return half4(1.0, 1.0, 1.0, a);           // thin white gloss
        }
    """.trimIndent()

    val fx = RuntimeEffect.makeForShader(src)

    object : ShaderBrush() {
        override fun createShader(size: Size): Shader =
            fx.makeShader(
                Data.makeFromBytes(
                    ByteBuffer.allocate(12)
                        .order(ByteOrder.nativeOrder())
                        .putFloat(size.width)         // res.x
                        .putFloat(size.height)        // res.y
                        .putFloat(sweep)              // sweep
                        .array()
                ),
                null, null
            )
    }
}