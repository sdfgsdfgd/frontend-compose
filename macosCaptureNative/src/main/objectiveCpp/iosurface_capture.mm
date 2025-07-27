//  iosurface_capture.mm
//  Builds with:   Xcode 15 SDK  +  Skia m122  +  C++17
//  2025-07                                           ────────────────

#import <Foundation/Foundation.h>
#import <Metal/Metal.h>
#import <CoreVideo/CoreVideo.h>
#import <CoreGraphics/CoreGraphics.h>
#import <IOSurface/IOSurface.h>
#import <dispatch/dispatch.h>
#import <ScreenCaptureKit/ScreenCaptureKit.h>

#include "iosurface_capture.h"

/* ── Skia --------------------------------------------------------- */
//#include "modules/skcms/skcms.h"   // makes <skcms.h> visible
#include "include/core/SkColorSpace.h"
#include "include/core/SkBitmap.h"
#include "include/core/SkSurface.h"
#include "include/core/SkImageInfo.h"
#include "modules/skcms/skcms.h"
#include "include/core/SkPixmap.h"
#include "include/gpu/GrDirectContext.h"
//#include "include/gpu/mtl/GrMtlGpu.h"
//#include "include/gpu/GrDirectContextPriv.h"
#include "include/gpu/GrBackendSurface.h"
#include "include/gpu/GrTypes.h"
#include "include/gpu/mtl/GrMtlTypes.h"         // GrMtlTextureInfo
#include "include/gpu/mtl/GrMtlBackendContext.h"
//#include "modules/skimage/include/SkImageGanesh.h" // SkImages::BorrowTextureFrom // xx turned out to be  skia m122+
#include "include/core/SkImage.h"
#include "include/ports/SkCFObject.h"

#include "jni.h"

/**
 *      xx   Stage 1. Upgrade to SKC ( Screen Capture Kit ) APIs  <-- CGDisplayStream/IOSurface APIs deprecated in macOS 15+
 *       .
 *              [ ongoing ]-->    https://chatgpt.com/c/68842bf0-c938-8329-8c66-09409bac1bab?model=gpt-4-5  ( Title: MPS vs Skia comparison )
 *       .
 *       .
 *              📌 Where exactly are you in your questchain?
 *       .
 *               [✅ COMPLETED] JNI Assembly of ObjC++ code
 *               [✅ COMPLETED] JNI Linking against runtime binaries/sysheaders of Sys+Metal+Skia
 *               [✅ COMPLETED] Capture desktop pixels via ScreenCaptureKit into IOSurfaceRef (already works!)
 *               [✅ COMPLETED] Create Metal textures from IOSurfaceRef (already works!)
 *       .
 *               [🟡 CURRENTLY STUCK] Wrap Metal texture into Skia SkImage without crashing due to Metal device mismatch.
 *       .
 *               [🟢 NEXT STEP] Pass Skia SkImage into Compose Canvas.
 *               [🟢 NEXT STEP] Apply Compose/SkSL refraction shaders on this Canvas.
 *       .
 *       .
 *       .      [ LuXuRY Upgrades ]
 *           Stage 2. Use MPS ( Metal Performance Shaders )         -  Skia+Compose+MPS                 Insane GPU-native performance. Close to 0 latency.
 *           Stage 3. Use Core Animation (QuartzCore) (w/ MPS)      -  Skia+Compose+MPS+Core Animation  Peak GPU-native performance. Absolute lowest latency.
 *             --> Core Animation  itself is explicitly optimized by Apple to handle real-time fluid animations on GPU without CPU interference.
 *             --> You might achieve extraordinary fluidity using undocumented -[CALayer setContentsChanged:] or private property tricks.
 *             --> Private QuartzCore Filters (CAFilter):
 *                  --> Beyond documented blur/shadow filters (CAGaussianBlur), Apple secretly employs internal GPU-accelerated filters
 *                      for its UI—think liquid translucency, frosted glass effects (CACompositingFilter, CASmoothBackdrop, CAVibrancyBackdrop).
 *                       -> Extracting them via runtime Objective-C introspection (objc_getClass, objc_msgSend) might reveal Apple’s raw GPU beauty—translucency layers,
 *                          real-time frosted-glass shaders, optimized backdrop filters that are ultra-smooth.
 *              https://chatgpt.com/g/g-3X6EMarap-x5/c/6883ffdc-8b20-8325-8e27-cc98ff05d3b4
 */


@class SurfaceOut;               // forward‑declare so the static uses a known type

/* ================================================================= */
/*  Low-level screen capture                                          */
/* ================================================================= */
static id<MTLDevice>        gDev   = nil;
static id<MTLCommandQueue>  gQueue = nil;   // ← new
static SCStream      *gStream;
static SurfaceOut    *gOut;

/* JVM glue -------------------------------------------------------- */
static JavaVM*  gJvm      = nullptr;
static jobject  gCallback = nullptr;

/* ================================================================= */
/*  JVM bootstrap                                                    */
/* ================================================================= */
extern "C" {

JNIEXPORT jint JNICALL
JNI_OnLoad(JavaVM* vm, void*) {
    gJvm = vm;
    return JNI_VERSION_1_6;
}

/* 1 ─ startCapture(callback: (Long) -> Unit) */
JNIEXPORT void JNICALL
Java_ui_DesktopCaptureBridge_startCapture(JNIEnv* env,
jobject /*this*/,
jobject callback) {

    if (gCallback) env->DeleteGlobalRef(gCallback);
        gCallback = env->NewGlobalRef(callback); // <- CRITICAL global ref

        StartCapture(^(IOSurfaceRef srf) {
        JNIEnv* env2;
        gJvm->AttachCurrentThread(reinterpret_cast<void**>(&env2), nullptr);

        jclass cls   = env2->GetObjectClass(gCallback);
        jmethodID id = env2->GetMethodID(cls, "onFrameCaptured", "(J)V"); // <-- CRITICAL fix here

        if (id) {
            env2->CallVoidMethod(
                gCallback, id,
                reinterpret_cast<jlong>(srf));
        }
        gJvm->DetachCurrentThread();
    });
}

/* 2 ─ createImageBitmapFromSkiaImage(imagePtr) : ImageBitmap */
JNIEXPORT jobject JNICALL
        Java_ui_DesktopCaptureBridge_createImageBitmapFromSkiaImage(
        JNIEnv* env, jobject /*this*/, jlong imgPtr) {

auto* img = reinterpret_cast<const SkImage*>(imgPtr);
if (!img) return nullptr;

SkBitmap bm;
bm.allocPixels(SkImageInfo::MakeN32Premul(img->width(), img->height()));
if (!img->readPixels(/*rc*/nullptr, bm.pixmap(), 0, 0)) return nullptr;

const size_t nBytes = bm.computeByteSize();
jbyteArray bytes = env->NewByteArray(nBytes);
env->SetByteArrayRegion(
        bytes, 0, nBytes,
reinterpret_cast<const jbyte*>(bm.getPixels()));

jclass    ktx = env->FindClass(
        "androidx/compose/ui/graphics/ImageBitmapKt");
jmethodID fn  = env->GetStaticMethodID(
        ktx, "ImageBitmap",
        "([BII)Landroidx/compose/ui/graphics/ImageBitmap;");

return env->CallStaticObjectMethod(
        ktx, fn, bytes, bm.width(), bm.height());
}

/* 3 ─ hasScreenCapturePermission(): Boolean */
JNIEXPORT jboolean JNICALL
Java_ui_DesktopCaptureBridge_hasScreenCapturePermission(
        JNIEnv*, jobject /*this*/) {

    /* CGDisplayStream* APIs are deprecated in macOS 15 but they still
       exist when the deployment target is ≤ 14; we just silence the
       warning. */
    CGDisplayStreamRef probe =
            CGDisplayStreamCreateWithDispatchQueue(
                    CGMainDisplayID(), 1, 1,
                    kCVPixelFormatType_32BGRA, nullptr,
                    dispatch_get_main_queue(),
                    ^(CGDisplayStreamFrameStatus, uint64_t,
                      IOSurfaceRef, CGDisplayStreamUpdateRef){});

    const bool ok = probe != nullptr;
    if (probe) CFRelease(probe);
    return ok ? JNI_TRUE : JNI_FALSE;
}

/* 4 ─ createSkiaImageFromIOSurface(surface, ctx) → Long */
JNIEXPORT jlong JNICALL
        Java_ui_DesktopCaptureBridge_createSkiaImageFromIOSurface(
        JNIEnv*, jobject, jlong surfPtr, jlong ctxPtr) {

    IOSurfaceRef srf = reinterpret_cast<IOSurfaceRef>(surfPtr);
    if (!srf) return 0;

    /* 🔎 sanity dump – goes here */
    NSLog(@"📐 sizeof(backend) = %zu", sizeof(GrMtlBackendContext));  // should be 24 on m110 AArch64

    // print both fields’ raw values
    NSLog(@"📥 createSkiaImage ctx=0x%llx  srf=0x%p  %zux%zu",
        ctxPtr, srf,
        IOSurfaceGetWidth(srf), IOSurfaceGetHeight(srf));

    /* --- context ------------------------------------------------ */
    static sk_sp<GrDirectContext> fallback;
    GrDirectContext* ctx = reinterpret_cast<GrDirectContext*>(ctxPtr);

    if (!srf || !ctx) {
        NSLog(@"❌  invalid args (srf %p  ctx %p)", srf, ctx);
        return 0;
    }

    /* --- texture ------------------------------------------------ */
    id<MTLTexture> tex = [gDev
            newTextureWithDescriptor:[MTLTextureDescriptor
                    texture2DDescriptorWithPixelFormat:MTLPixelFormatBGRA8Unorm
                                                 width:IOSurfaceGetWidth(srf)
                                                height:IOSurfaceGetHeight(srf)
                                             mipmapped:NO]
                           iosurface:srf
                               plane:0];

    NSLog(@"🌀  MTLTexture=%p", tex);            // xx           <--   27 JUL 2025 -  CHECKPOINT !!!   WE ARE HERE
    if (!tex) {                                   // 🔥 bail instead of seg‑faulting
        NSLog(@"❌  newTextureWithDescriptor() returned nil");
        return 0;
    }
    GrMtlTextureInfo info{};
    info.fTexture.retain((__bridge void*)tex);

    GrBackendTexture beTex((int)IOSurfaceGetWidth(srf),
                           (int)IOSurfaceGetHeight(srf),
                           GrMipmapped::kNo,
                           info);

    // --- sanity probe (public-API ONLY) -------------------------------------------------
    id<MTLDevice> texDev = tex.device;
    id<MTLDevice> ctxDev = gDev; // use global device directly (already initialized)

    // Simple sanity comparison
    NSLog(@"🔍 devices: ctxDev=%p   texDev=%p   same=%d",
    ctxDev, texDev, ctxDev == texDev);

    NSLog(@"🔍 current thread   isMain=%d   pthread=%p",
    [NSThread isMainThread], pthread_self());

    sk_sp<SkImage> img = SkImage::MakeFromTexture(
            ctx, beTex,
            kTopLeft_GrSurfaceOrigin,
            kBGRA_8888_SkColorType,
            kOpaque_SkAlphaType,
            nullptr);

    NSLog(@"🎯  SkImage=%p", img.get());
    if (!img) {
        NSLog(@"❌  SkImage::MakeFromTexture() failed");
        return 0;
    }

    return reinterpret_cast<jlong>(img.release());
}

} // extern "C"

@interface SurfaceOut : NSObject<SCStreamOutput, SCStreamDelegate>
@property (nonatomic, copy) void (^handler)(IOSurfaceRef srf);
@end

@implementation SurfaceOut
- (void)stream:(SCStream *)s
didOutputSampleBuffer:(CMSampleBufferRef)buf
        ofType:(SCStreamOutputType)type
{
    if (type != SCStreamOutputTypeScreen) return;

    CVPixelBufferRef px = CMSampleBufferGetImageBuffer(buf);
    IOSurfaceRef     srf = CVPixelBufferGetIOSurface(px);
    if (!srf) return;

    CFRetain(srf);          // keep alive outside callback
    self.handler(srf);
    CFRelease(srf);
}
@end

void StartCapture(CaptureCallback cb)
{
    gDev = MTLCreateSystemDefaultDevice();

    [SCShareableContent getShareableContentWithCompletionHandler:^(SCShareableContent *content, NSError *error) {
        if (error || !content) { NSLog(@"SC error %@", error); return; }

        SCDisplay *main = content.displays.firstObject;
        SCContentFilter *filter = [[SCContentFilter alloc] initWithDisplay:main excludingWindows:@[]];

        SCStreamConfiguration *cfg = [SCStreamConfiguration new];
        cfg.width   = main.width;
        cfg.height  = main.height;
        cfg.pixelFormat = kCVPixelFormatType_32BGRA;
        cfg.minimumFrameInterval = CMTimeMake(1, 120);

        gOut = [SurfaceOut new];
        gOut.handler = cb;

        gStream = [[SCStream alloc] initWithFilter:filter
                                     configuration:cfg
                                          delegate:gOut];

        NSError *streamErr = nil;
        [gStream addStreamOutput:gOut                     // ← back to old selector
                            type:SCStreamOutputTypeScreen
              sampleHandlerQueue:dispatch_get_main_queue()
                           error:&streamErr];
        if (streamErr) NSLog(@"addStreamOutput failed: %@", streamErr);

        [gStream startCaptureWithCompletionHandler:^(NSError *e){
            if (e) NSLog(@"SCStream start error %@", e);
        }];
    }];
}
