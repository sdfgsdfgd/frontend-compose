#ifndef IOSURFACE_CAPTURE_H
#define IOSURFACE_CAPTURE_H

#include <CoreGraphics/CoreGraphics.h>
#include <CoreFoundation/CoreFoundation.h>
#include <IOSurface/IOSurfaceRef.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef void (^CaptureCallback)(IOSurfaceRef surface);

void StartCapture(CaptureCallback callback);

#ifdef __cplusplus
}
#endif
#endif /* IOSURFACE_CAPTURE_H */