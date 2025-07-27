package ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RenderEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.asSkiaPath
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toSkiaRRect
import androidx.compose.ui.graphics.toSkiaRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.sdfgsdfg.resources.Res
import org.jetbrains.skia.DirectContext
import org.jetbrains.skia.FilterBlurMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageFilter
import org.jetbrains.skia.MaskFilter
import org.jetbrains.skia.PaintMode
import org.jetbrains.skia.PaintStrokeCap
import org.jetbrains.skia.PaintStrokeJoin
import org.jetbrains.skia.RuntimeEffect
import org.jetbrains.skia.RuntimeShaderBuilder
import org.jetbrains.skia.Shader
import java.awt.Container
import java.awt.Window
import java.lang.reflect.Field
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.createTempFile
import kotlin.io.path.writeBytes
import kotlin.math.max
import org.jetbrains.skia.Paint as SkiaPaint
import java.lang.reflect.Modifier as JavaModifier

// region ───[  Dynamic Island  -  ( MetaBall effect, States etc..)   ]────────────────────────────────────────────────────────────
@Composable
actual fun MetaContainer(
    modifier: Modifier,
    cutoff: Float,
    content: @Composable BoxScope.() -> Unit
) {
    // language=AGSL
    val shaderSource = """
        uniform shader composable;
        uniform float cutoff;
    
        half4 main(float2 fragCoord) {
            half4 color = composable.eval(fragCoord);
        
            float feather = 0.05; // adjust this value for desired smoothness (smaller = sharper edge)
            color.a = smoothstep(cutoff - feather, cutoff + feather, color.a);
        
            return color;
        }
""".trimIndent()

    val runtimeShader = remember { RuntimeEffect.makeForShader(shaderSource) }
    val builder = remember(runtimeShader, cutoff) {
        RuntimeShaderBuilder(runtimeShader).apply {
            uniform("cutoff", cutoff)
        }
    }

    val imgFilter = remember(builder) {
        ImageFilter.makeRuntimeShader(
            runtimeShaderBuilder = builder,
            shaderName = "composable",
            input = null
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            renderEffect = imgFilter.asComposeRenderEffect()
        },
        content = content
    )
}


@Composable
actual fun Modifier.blurEffect(radius: Dp): Modifier {
    val density = LocalDensity.current
    val pxRadius = with(density) { radius.toPx() }

    return graphicsLayer(
        renderEffect = BlurEffect(pxRadius, pxRadius, edgeTreatment = TileMode.Decal)
    )
}

// endregion

// region ────[  Shadow  -  ( Inner & Outer )   ]───────────────────────────────────────────────────────────────
// xx                    .
//                     .
//    Heavily Modified and Unified version of the 2:
//      Inner https://medium.com/@kappdev/inner-shadow-in-jetpack-compose-d80dcd56f6cf
//      Outer https://medium.com/@kappdev/custom-drop-shadow-from-figma-in-jetpack-compose-for-any-shape-d20fccac4e20
//
// Note: Fcking replaced by Compose 1.9.0  .__.
actual fun DrawScope.drawShadow(
    shadow: Shadow,
    shape: Shape,
    size: Size,
    layoutDirection: LayoutDirection,
    isInner: Boolean
) {
    val shadowSize = Size(size.width + shadow.spread.toPx(), size.height + shadow.spread.toPx())
    val outline = shape.createOutline(shadowSize, layoutDirection, this)

    val skiaPaint = org.jetbrains.skia.Paint().apply {
        color = shadow.color.toArgb()
        isAntiAlias = true
        maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, shadow.blur.toPx())
    }

    drawContext.canvas.nativeCanvas.apply {
        if (isInner) {
            // Inner Shadow with compositing (subtract shadow from content)
            val layerPaint = org.jetbrains.skia.Paint()
            saveLayer(size.toRect().inflate(shadow.blur.toPx() * 4f).toSkiaRect(), layerPaint)

            when (outline) {
                is Outline.Rectangle -> drawRect(outline.rect.toSkiaRect(), skiaPaint)
                is Outline.Rounded -> drawRRect(outline.roundRect.toSkiaRRect(), skiaPaint)
                is Outline.Generic -> drawPath(outline.path.asSkiaPath(), skiaPaint)
            }

            skiaPaint.blendMode = org.jetbrains.skia.BlendMode.CLEAR

            translate(shadow.dx.toPx(), shadow.dy.toPx())
            when (outline) {
                is Outline.Rectangle -> drawRect(outline.rect.toSkiaRect(), skiaPaint)
                is Outline.Rounded -> drawRRect(outline.roundRect.toSkiaRRect(), skiaPaint)
                is Outline.Generic -> drawPath(outline.path.asSkiaPath(), skiaPaint)
            }

            restore()
        } else { // OUTER
            val layerPaint = org.jetbrains.skia.Paint()
            saveLayer(size.toRect().inflate(max(shadow.blur.toPx(), shadow.spread.toPx()) * 8).toSkiaRect(), layerPaint)

            val halfSpreadPx = shadow.spread.toPx() / 2f

            translate(shadow.dx.toPx() - halfSpreadPx, shadow.dy.toPx() - halfSpreadPx)

            // Draw shadow outline larger than original shape
            when (outline) {
                is Outline.Rectangle -> drawRect(outline.rect.toSkiaRect(), skiaPaint)
                is Outline.Rounded -> drawRRect(outline.roundRect.toSkiaRRect(), skiaPaint)
                is Outline.Generic -> drawPath(outline.path.asSkiaPath(), skiaPaint)
            }

            // Now subtract original shape
            skiaPaint.blendMode = org.jetbrains.skia.BlendMode.CLEAR

            translate(-shadow.dx.toPx(), -shadow.dy.toPx())
            when (val originalOutline = shape.createOutline(size, layoutDirection, this@drawShadow)) {
                is Outline.Rectangle -> drawRect(originalOutline.rect.toSkiaRect(), skiaPaint)
                is Outline.Rounded -> drawRRect(originalOutline.roundRect.toSkiaRRect(), skiaPaint)
                is Outline.Generic -> drawPath(originalOutline.path.asSkiaPath(), skiaPaint)
            }

            restore()
        }
    }
}
// endregion

// region xx    ---------------- Helpers of  `.colorCloud()` ---------------------
actual fun DrawScope.drawCombinedGradientStroke(
    path: Path,
    sweepBrush: Brush,
    strokeWidthPx: Float,
    shapeSize: Size,
    shape: Shape
) {
    val sweepShader = sweepBrush.toShader(shapeSize)

    val skiaPaint = SkiaPaint().apply {
        shader = sweepShader
        strokeWidth = strokeWidthPx
        isAntiAlias = true
        mode = PaintMode.STROKE
        strokeJoin = PaintStrokeJoin.ROUND
        strokeCap = PaintStrokeCap.ROUND
        maskFilter = MaskFilter.makeBlur(FilterBlurMode.NORMAL, strokeWidthPx * 0.6f)
    }

    drawContext.canvas.nativeCanvas.drawPath(path.asSkiaPath(), skiaPaint)
}

// Extension helper to convert Compose Brush to Skia Shader
private fun Brush.toShader(size: Size): Shader {
    return when (this) {
        is ShaderBrush -> createShader(size)
        else -> error("Unsupported Brush type. Use ShaderBrush or predefined brushes.")
    }
}
// endregion xx    ---------------- Helpers of  `.colorCloud()` ---------------------

// region  ───────────────────[ Liquid Glass ]─────────────────────────────────────────────
@Composable
actual fun Modifier.liquidGlass(
    state: LiquidGlassProviderState,
    style: LiquidGlassStyle
): Modifier {
    val sizePx = remember { mutableStateOf(Size.Zero) }
    val density = LocalDensity.current

    val shadersCache = remember { LiquidGlassShadersCache() }
    val refractionShader = shadersCache.getRefractionShader(withBleed = style.bleed.opacity > 0f)
    val bleedShader = shadersCache.getBleedShader()
    val materialShader = shadersCache.getMaterialShader()

    val capturedBitmap = remember { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(sizePx.value, state.graphicsLayer) {
        capturedBitmap.value = state.graphicsLayer.toImageBitmap()
    }
    val contentFilter: ImageFilter? = capturedBitmap.value?.let { bmp ->
        println("captured → ${bmp.width}×${bmp.height}")
        val skBitmap = bmp.asSkiaBitmap()
        val skImage = Image.makeFromBitmap(skBitmap)
        ImageFilter.makeImage(skImage)
    }

    val refractionEffect = remember(refractionShader, style, sizePx.value) {
        RuntimeShaderBuilder(refractionShader).apply {
            uniform("size", sizePx.value.width, sizePx.value.height)
            uniform("cornerRadius", style.shape.topStart.toPx(sizePx.value, density))

            uniform("refractionHeight", style.innerRefraction.height.toPx(density, sizePx.value))
            uniform("refractionAmount", style.innerRefraction.amount.toPx(density, sizePx.value))
            uniform("eccentricFactor", style.innerRefraction.eccentricFactor)

            if (style.bleed.opacity > 0f) {
                uniform("bleedOpacity", style.bleed.opacity)
            }
        }
    }

    val bleedEffect = remember(bleedShader, style, sizePx.value) {
        if (style.bleed.opacity > 0f) {
            RuntimeShaderBuilder(bleedShader).apply {
                uniform("size", sizePx.value.width, sizePx.value.height)
                uniform("cornerRadius", style.shape.topStart.toPx(sizePx.value, density))

                uniform("eccentricFactor", style.innerRefraction.eccentricFactor)
                uniform("bleedAmount", style.bleed.amount.toPx(density, sizePx.value))
            }
        } else null
    }

    val materialEffect = remember(materialShader, style) {
//        if (style.material != GlassMaterial.Default) {
        RuntimeShaderBuilder(materialShader).apply {
            uniform("contrast", style.material.contrast)
            uniform("whitePoint", style.material.whitePoint)
            uniform("chromaMultiplier", style.material.chromaMultiplier)
        }
//        } else null
    }

    val composedEffect = remember(refractionEffect, bleedEffect, materialEffect, style, sizePx.value, capturedBitmap.value) {
        // start with the refraction runtime shader effect
        var effect: RenderEffect = ImageFilter
            .makeRuntimeShader(refractionEffect, "image", input = contentFilter)
            .asComposeRenderEffect()

        if (bleedEffect != null) {
            val bleedFx = ImageFilter.makeRuntimeShader(bleedEffect, "image", input = contentFilter)
                .asComposeRenderEffect()

            val blurredBleed = BlurEffect(
                bleedFx,
                with(density) { style.bleed.blurRadius.toPx() },
                with(density) { style.bleed.blurRadius.toPx() },
                TileMode.Clamp
            )

            // blend bleed **over** refraction
            effect = ImageFilter.makeBlend(
                blendMode = org.jetbrains.skia.BlendMode.SRC_OVER,
                fg = blurredBleed.asSkiaImageFilter(), // foreground: over
                bg = effect.asSkiaImageFilter(),       // background: under
                crop = null
            ).asComposeRenderEffect()
        }

        val materialFx = ImageFilter
            .makeRuntimeShader(materialEffect, "image", input = contentFilter)
            .asComposeRenderEffect()

        // chain material **into** the current effect
        effect = materialFx.let { mat ->
            // chain: materialFx feeds into existing 'effect'
            ImageFilter.makeBlend(
                blendMode = org.jetbrains.skia.BlendMode.SRC_OVER,
                bg = mat.asSkiaImageFilter(),
                fg = effect.asSkiaImageFilter(),
                crop = null,
            ).asComposeRenderEffect()
        }

        val contentBlur = with(density) {
            style.material.blurRadius.toPx().takeIf { it > 0f }?.let { radius ->
                BlurEffect(effect, radius, radius, TileMode.Clamp)
            }
        }

        contentBlur ?: effect
    }


    return graphicsLayer {
        compositingStrategy = CompositingStrategy.Offscreen
        renderEffect = composedEffect
    }.drawBehind {
        with(state.graphicsLayer) {
            drawLayer(this)
        }
    }.onSizeChanged { newSize ->
        sizePx.value = Size(newSize.width.toFloat(), newSize.height.toFloat())
    }
}

@Stable
internal class LiquidGlassShadersCache {

    private var _materialShader: RuntimeEffect? = null
    private var _refractionShader: RuntimeEffect? = null
    private var isRefractionShaderWithBleed = false
    private var _bleedShader: RuntimeEffect? = null

    fun getMaterialShader(): RuntimeEffect {
        if (_materialShader == null) {
            _materialShader = RuntimeEffect.makeForShader(LiquidGlassShaders.materialShaderString)
        }
        return _materialShader!!
    }

    fun getRefractionShader(withBleed: Boolean): RuntimeEffect {
        if (_refractionShader == null || isRefractionShaderWithBleed != withBleed) {
            isRefractionShaderWithBleed = withBleed
            _refractionShader = RuntimeEffect.makeForShader(
                if (withBleed) {
                    LiquidGlassShaders.refractionShaderWithBleedString
                } else {
                    LiquidGlassShaders.refractionShaderString
                }
            )
        }
        return _refractionShader!!
    }

    fun getBleedShader(): RuntimeEffect {
        if (_bleedShader == null) {
            _bleedShader = RuntimeEffect.makeForShader(LiquidGlassShaders.bleedShaderString)
        }
        return _bleedShader!!
    }
}

// xx Final Problem of LiquidGlass Multiplatform:
//  .
//  .
//        -->  Performance of Linux/X11 Desktop Pixel Capture Paths:
//  .
//                  Method	                    Perf.	                CPU/GPU Usage	    Latency	        Notes
//                  XGetImage	                Poor 🚨	                CPU heavy 💀	    High	        Slow AF. CPU-bound pixel copy
//                  XShmGetImage (XShm)	        Decent (~60fps) ✅	    Moderate CPU 🔸	    Low-Mid	Shared memory, workable perf
//     GLX (OpenGL + X11 texture_from_pixmap)	Good (60+fps) ✅	Low CPU, GPU heavy 🚀	Low	GPU accelerated. Fastest
//          EGL (Jake’s method)	(IN PR)         Very Good ✅✅	        GPU optimized 🚀	Very Low	Modern GL stack, ideal perf
//  .
//  .
//           --> macOS path:        CGDisplayStream + IOSurface → Metal texture → Skia Image
//  .
//  .
//          [ main GPT threads ]
//     1. https://chatgpt.com/g/g-3X6EMarap-x5/c/687a25d2-9f50-832a-8f52-4c94e5d02edd - dylib, JNI, multipl code so far
//     2. https://chatgpt.com/g/g-3X6EMarap-x5/c/687b482e-1704-8332-81d8-c1cf9be978fa - ( Linux & WL & OSX )
//  .
//  .
//          [ The Chariot ( Code ) ]
//  1. https://github.com/JetBrains/skiko/issues/918
//  2. https://youtrack.jetbrains.com/issue/SKIKO-918/Add-EGL-support
//  4. https://github.com/JetBrains/skiko/pull/1051         - Add linuxArm64 target
//  5. https://github.com/JetBrains/skia-pack/pull/68       - Enable EGL support in Skia Pack
//  6. https://github.com/JetBrains/skiko/pull/1052         - Add support for EGL to Skiko  #1052
//  .
//  .
//  p.s. https://discuss.kotlinlang.org/t/how-to-render-offscreen-using-the-gpu/29721/6


@Suppress("UnsafeDynamicallyLoadedCode")
actual object DesktopCaptureBridge {
    init {
        runBlocking {
            val bytes = Res.readBytes("files/libDesktopCapture.dylib")
            val tmpPath = createTempFile("desktopCapture", ".dylib")
            tmpPath.writeBytes(bytes)
            tmpPath.toFile().deleteOnExit()
            System.load(tmpPath.toString())
        }
    }

    actual external fun hasScreenCapturePermission(): Boolean
    actual external fun createSkiaImageFromIOSurface(surfacePtr: Long, contextPtr: Long): Long
    actual external fun createImageBitmapFromSkiaImage(skImagePtr: Long): ImageBitmap

    //    actual external fun startCapture(onFrameCaptured: (Long) -> Unit)
    actual external fun startCapture(callback: FrameCallback)
}

@Composable
fun rememberScreenPtr(): State<Long> {
    val skPtr = remember { mutableStateOf(0L) }
    val fps = remember { AtomicInteger(0) }

    /*  start native capture once  */
    LaunchedEffect(Unit) {
        DesktopCaptureBridge.startCapture { surfacePtr ->
            fps.incrementAndGet()
            println("🔄 IOSurface 0x${surfacePtr.toString(16)}")

            DesktopCaptureBridge.createSkiaImageFromIOSurface(surfacePtr, 0L)
                .takeIf { it != 0L }
                ?.let { skPtr.value = it }
        }
    }

    /*  1‑second FPS ticker  */
    LaunchedEffect(Unit) {
        while (true) {
            println("💡 FPS=${fps.getAndSet(0)}  SkImage*=${skPtr.value.toString(16)}")
            delay(300)
        }
    }
    return skPtr
}

@Composable
fun LiquidGlassDemoDesktop() {
    val skPtr = remember { mutableStateOf(0L) }
    val fpsCtr = remember { AtomicInteger(0) }

    /** 1 · wait for Metal context, then start capture */
    LaunchedEffect(Unit) {
        val ctxPtr = awaitContextPtr()
        DesktopCaptureBridge.startCapture { srfPtr ->
            fpsCtr.incrementAndGet()
            val imgPtr =
                DesktopCaptureBridge.createSkiaImageFromIOSurface(srfPtr, ctxPtr)
            if (imgPtr != 0L) skPtr.value = imgPtr
        }
    }

    /** 2 · print FPS every second (optional) */
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            println("FPS = ${fpsCtr.getAndSet(0)}")
        }
    }

    /** 3 · draw the latest frame */
    Canvas(Modifier.fillMaxSize()) {
        val ptr = skPtr.value
        if (ptr != 0L) {
            val img = Image.wrapNative(ptr)
            drawIntoCanvas { it.nativeCanvas.drawImage(img, 0f, 0f) }
            img.close()                       // free GPU memory each frame
        }
    }
}

/** Turns the native pointer we get from JNI into an Image wrapper. */
private val imageCtor = Image::class.java.getDeclaredConstructor(Long::class.javaPrimitiveType).apply { isAccessible = true }
fun Image.Companion.wrapNative(ptr: Long): Image = imageCtor.newInstance(ptr)
private suspend fun awaitContextPtr(): Long {
    repeat(600) {                // ~10 s timeout
        currentGrContextPtr().takeIf { it != 0L }?.let { return it }
        delay(16)
    }
    error("Timed out waiting for GrDirectContext*")
}


data class GrContextInfo(val contextPtr: Long, val devicePtr: Long)

/** Exactly what you had before, just without the extra wrappers. */
fun currentGrContextPtr(): Long {
    // Stage 1. visible ComposeWindow ----------------------------------------
    val win = Window.getWindows().firstOrNull { it.isShowing } ?: return 0L
    println("▶️  window = ${win::class.java.name}")

    val rootHit = findSkiaLayer(win, tag = "window-root") ?: run {
        println("⚠️  SkiaLayer not found (yet)"); return 0L
    }
    println("🎯  FIRST hit → ${rootHit.javaClass.name}")


    /* ▼▼▼  reliable unwrap until we reach the real SkiaLayer  */
    var node: Any = rootHit
    while (!node.javaClass.name.contains("SkiaLayer")) {   // ← change is here
        // 1) look for any field whose value *is* a SkiaLayer
        val viaField = node.javaClass.declaredFields.firstOrNull { f ->
            f.isAccessible = true
            val v = runCatching { f.get(node) }.getOrNull()
            v != null && v.javaClass.name.contains("SkiaLayer")
        }?.also { f ->
            val v = f.get(node)
            println("↪️  unwrap via field “${f.name}” → ${v!!.javaClass.name}")
            node = v
        }

        if (viaField != null) {
            Thread.sleep(1_200)   // throttle logs
            continue
        }

        // 2) outer-reference fallback
        val outerF = node.javaClass.declaredFields
            .firstOrNull { it.name.startsWith("this$") }
        if (outerF != null) {
            outerF.isAccessible = true
            node = outerF.get(node) ?: break
            println("↪️  unwrap via outer ${outerF.name} → ${node.javaClass.name}")
            Thread.sleep(1_200)   // throttle logs
            continue
        }

        println("❌  cannot unwrap to SkiaLayer – abort")
        return 0L
    }
    val skiaLayer = node        // may be an anonymous subclass
    println("✅  Real SkiaLayer → ${skiaLayer.javaClass.name}")
    /* ▲▲▲  END OF BLOCK  */


    /* Stage 3 ─ Redrawer (MetalRedrawer / NanoVGRedrawer …)          */
    var redrawer: Any? = null

    /* 3a › public getter (newer Skiko) */
    runCatching {
        redrawer = skiaLayer.javaClass
            .getMethod("getRedrawer")          // may throw
            .also { it.isAccessible = true }
            .invoke(skiaLayer)
    }

    /* 3b › zero-arg method whose *return-type* contains “Redrawer” */
    if (redrawer == null) {
        skiaLayer.javaClass.methods
            .filter { it.parameterCount == 0 && it.returnType.name.contains("Redrawer") }
            .forEach { m ->
                m.isAccessible = true
                val v = m.invoke(skiaLayer)
                println("   • via method ${m.name}() → ${v?.javaClass?.name}")
                if (v != null) redrawer = v
            }
    }

    /* 3c › private field scan (old Skiko) */
    if (redrawer == null) {
        println("   • scanning fields on SkiaLayer …")
        skiaLayer.javaClass.declaredFields.forEach { f ->
            f.isAccessible = true
            val v = f.get(skiaLayer)
            println("     - ${f.name} : ${f.type.name}  = $v")
            if (v != null && f.type.name.contains("Redrawer")) redrawer = v
        }
    }

    if (redrawer == null) {
        println("⚠️  Redrawer still not found – abort")
        return 0L
    }
    println("   • Redrawer  = ${redrawer!!::class.java.name}")


    runBlocking {
        delay(1200)
    }

    /* -- helpers ----------------------------------------------------------- */
    fun Field.trySetAccessible(): Boolean =
        runCatching { isAccessible = true }.isSuccess

    fun Field.longValue(owner: Any): Long = when (type) {
        Long::class.javaPrimitiveType -> getLong(owner)
        java.lang.Long::class.java -> (get(owner) as? Long) ?: 0L
        else -> 0L
    }

    fun findDirectCtx(
        obj: Any?,
        seen: MutableSet<Int> = mutableSetOf(),
        depth: Int = 0,
    ): DirectContext? {
        if (obj == null) return null
        if (obj is DirectContext) return obj

        val id = System.identityHashCode(obj)
        if (!seen.add(id)) return null          // avoid cycles

        // walk only non-JDK classes
        if (obj.javaClass.name.startsWith("java.")) return null

        obj.javaClass.declaredFields.forEach { f ->
            if (JavaModifier.isStatic(f.modifiers)) return@forEach

            /*  SKIP first → then setAccessible  */
            if (f.declaringClass.name.startsWith("java.")) return@forEach
            runCatching { f.isAccessible = true }

            val v = runCatching { f.get(obj) }.getOrNull() ?: return@forEach
            when (v) {
                is DirectContext -> {
                    println("   • DirectCtx via ${".".repeat(depth)}${f.name} → $v")
                    return v
                }

                else -> if (depth < 3)
                    findDirectCtx(v, seen, depth + 1)?.let { return it }
            }
        }
        return null
    }

    /* ---------- Stage 4 + 5 — build a DirectContext from MetalContextHandler, if needed ---- */
    /* ---------- Stage 4 — FAST-PATH: use the SkiaLayer’s own SkikoContext ---------- */
    /* ───── Stage 4 + 5 – locate or fabricate DirectContext ───────────────── */

    val directCtxFast: DirectContext? = findDirectCtx(skiaLayer)

    /* helper: visit ALL fields (also inherited) and recurse into non‑JDK objects */
    fun walk(
        obj: Any?,
        seen: MutableSet<Int>,
        depth: Int = 0,
        onLong: (Field, Any, Long, Int) -> Unit,
        onCtx: (DirectContext) -> Unit
    ) {
        if (obj == null) return
        if (obj is DirectContext) {
            onCtx(obj); return
        }

        val id = System.identityHashCode(obj)
        if (!seen.add(id) || obj.javaClass.name.startsWith("java.")) return   // avoid loops / JDK

        // iterate over *all* declared fields, including super‑classes
        var k: Class<*>? = obj.javaClass
        while (k != null && k != Any::class.java) {
            k.declaredFields.forEach { f ->
                if (JavaModifier.isStatic(f.modifiers)) return@forEach
                if (!f.trySetAccessible()) return@forEach
                val v = f.get(obj) ?: return@forEach

                when (v) {
                    is DirectContext -> onCtx(v)
                    is Long -> onLong(f, obj, v, depth)
                    else -> walk(v, seen, depth + 1, onLong, onCtx)
                }
            }
            k = k.superclass
        }
    }

    /* gather every *non‑zero* long we observe; keep their source/field for logging */
    data class Ptr(val value: Long, val owner: String, val field: String)

    val longs = mutableListOf<Ptr>()

    var directCtx: DirectContext? = directCtxFast
    if (directCtx == null) {
        walk(
            redrawer ?: skiaLayer, mutableSetOf(),
            onLong = { f, o, v, d ->
                if (v != 0L) {
                    longs += Ptr(v, o.javaClass.simpleName, f.name)
                    println("${"  ".repeat(d)}↳ ${o.javaClass.simpleName}.${f.name} = 0x${v.toString(16)}")
                }
            },
            onCtx = { dc -> directCtx = dc }
        )
    }

    /* Fast exit if we found one during the walk */
    if (directCtx != null) {
        println("✅  DirectCtx discovered during walk → $directCtx")
    } else {
        /* pick first two distinct non‑zero longs as {device, queue} */
        val uniq = longs.map { it.value }.distinct().filter { it != 0L }
        val devicePtr = uniq.getOrNull(0) ?: 0L
        val queuePtr = uniq.getOrNull(1) ?: 0L

        if (devicePtr == 0L || queuePtr == 0L) {
            println("⚠️  Unable to harvest two distinct pointers — abort")
            return 0L
        }

        println("   • building DirectContext from harvested pointers")
        println("     device = 0x${devicePtr.toString(16)}   (${longs.first { it.value == devicePtr }.owner}.${longs.first { it.value == devicePtr }.field})")
        println("     queue  = 0x${queuePtr.toString(16)}   (${longs.first { it.value == queuePtr }.owner}.${longs.first { it.value == queuePtr }.field})")

        directCtx = DirectContext.makeMetal(devicePtr, queuePtr)
    }

    println("✅  DirectCtx = $directCtx")

    //
    //
    //
    // xx   🚀🚀✅✅✅ WE ARE HERE -  AT STAGE 6  - we have ALL of the above greenlighted already ✅✅✅🚀🚀
    //
    //
    //
    /* Stage 6 — native pointer (unchanged) */
    val ptrField: Field = generateSequence(directCtx!!.javaClass as Class<*>) { it.superclass }
        .mapNotNull { cls ->                 // walk DirectContext → RefCnt → Any
            runCatching { cls.getDeclaredField("_ptr") }.getOrNull()
        }
        .firstOrNull() ?: error("Internal error: no _ptr field on RefCnt")

    ptrField.trySetAccessible()              // ← use the helper you already defined
    val ptr = ptrField.getLong(directCtx)

    println("✅  GrDirectContext* = 0x${ptr.toString(16)}")
    return ptr
    /* ------------------------------------------------------------------ */
}

/**
 * Walk the subtree of [node] printing every object we inspect.
 * Returns the first object whose class‐name ends with “SkiaLayer”.
 *
 * We also recognise Compose’s “ComposeLayer” wrapper and look through
 * its private `layer` field.
 */
private fun findSkiaLayer(
    node: Any?,
    depth: Int = 0,
    tag: String = "<root>"
): Any? {
    if (node == null) return null

    val indent = "│ ".repeat(depth)
    val cls = node.javaClass.name
    println("$indent└─ $tag → $cls")

    // ①  the object *itself* is the Skia layer
    if (cls.contains("SkiaLayer")) return node

    // ②  ComposeLayer delegates – peek into its private “layer”
    if (cls.endsWith("ComposeLayer")) {
        runCatching {
            val inner = node.javaClass
                .getDeclaredField("layer")
                .apply { isAccessible = true }
                .get(node)
            println("$indent   ↳ inner layer = ${inner?.javaClass?.name}")
            if (inner != null && inner.javaClass.name.endsWith("SkiaLayer"))
                return inner
        }.onFailure { println("$indent   ↳ (no inner layer field)") }
    }

    // ③  Recurse through Swing children if this is a Container
    if (node is Container) {
        node.components.forEachIndexed { i, child ->
            findSkiaLayer(child, depth + 1, "child[$i]")?.let { return it }
        }
    }
    return null
}


// endregion
