/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <media/stagefright/ColorConverter.h>
#include <media/stagefright/MediaDebug.h>

#if defined (TARGET_OMAP4) && defined (OMAP_ENHANCEMENT)
#include <OMX_TI_IVCommon.h>
#endif

namespace android {

static const int OMX_QCOM_COLOR_FormatYVU420SemiPlanar = 0x7FA30C00;

ColorConverter::ColorConverter(
        OMX_COLOR_FORMATTYPE from, OMX_COLOR_FORMATTYPE to)
    : mSrcFormat(from),
      mDstFormat(to),
      mClip(NULL) {
}

ColorConverter::~ColorConverter() {
    delete[] mClip;
    mClip = NULL;
}

bool ColorConverter::isValid() const {
    if (mDstFormat != OMX_COLOR_Format16bitRGB565) {
        return false;
    }

    switch (mSrcFormat) {
        case OMX_COLOR_FormatYUV420Planar:
        case OMX_COLOR_FormatCbYCrY:
        case OMX_QCOM_COLOR_FormatYVU420SemiPlanar:
        case OMX_COLOR_FormatYUV420SemiPlanar:
<<<<<<< HEAD
=======
        case QOMX_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka:
#if defined(OMAP_ENHANCEMENT) && defined(TARGET_OMAP4)
        case OMX_COLOR_FormatYUV420PackedSemiPlanar:
        case OMX_TI_COLOR_FormatYUV420PackedSemiPlanar_Sequential_TopBottom:
#endif
>>>>>>> 1a6862f... Updated OMAP support
            return true;

        default:
            return false;
    }
}

#if defined(OMAP_ENHANCEMENT) && defined(TARGET_OMAP4)
void ColorConverter::convert(
        size_t width, size_t height,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip,
        size_t dwidth, size_t dheight, size_t nOffset, bool interlaced) {
#else
void ColorConverter::convert(
        size_t width, size_t height,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip) {
#endif
    CHECK_EQ(mDstFormat, OMX_COLOR_Format16bitRGB565);

    switch (mSrcFormat) {
        case OMX_COLOR_FormatYUV420Planar:
            convertYUV420Planar(
                    width, height, srcBits, srcSkip, dstBits, dstSkip);
            break;

        case OMX_COLOR_FormatCbYCrY:
            convertCbYCrY(
                    width, height, srcBits, srcSkip, dstBits, dstSkip);
            break;

        case OMX_QCOM_COLOR_FormatYVU420SemiPlanar:
            convertQCOMYUV420SemiPlanar(
                    width, height, srcBits, srcSkip, dstBits, dstSkip);
            break;

        case OMX_COLOR_FormatYUV420SemiPlanar:
            convertYUV420SemiPlanar(
                    width, height, srcBits, srcSkip, dstBits, dstSkip);
            break;

<<<<<<< HEAD
=======
        case QOMX_COLOR_FormatYUV420PackedSemiPlanar64x32Tile2m8ka:
            convertNV12Tile(
                    width, height, srcBits, srcSkip, dstBits, dstSkip);
            break;
#if defined(OMAP_ENHANCEMENT) && defined(TARGET_OMAP4)
        case OMX_COLOR_FormatYUV420PackedSemiPlanar:
        case OMX_TI_COLOR_FormatYUV420PackedSemiPlanar_Sequential_TopBottom:
            convertYUV420PackedSemiPlanar(
                    width, height, dwidth, dheight,nOffset,srcBits, srcSkip, dstBits, dstSkip, interlaced);
            break;
#endif
>>>>>>> 1a6862f... Updated OMAP support
        default:
        {
            CHECK(!"Should not be here. Unknown color conversion.");
            break;
        }
    }
}

void ColorConverter::convertCbYCrY(
        size_t width, size_t height,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip) {
    CHECK_EQ(srcSkip, 0);  // Doesn't really make sense for YUV formats.
    CHECK(dstSkip >= width * 2);
    CHECK((dstSkip & 3) == 0);

    uint8_t *kAdjustedClip = initClip();

    uint32_t *dst_ptr = (uint32_t *)dstBits;

    const uint8_t *src = (const uint8_t *)srcBits;

    for (size_t y = 0; y < height; ++y) {
        for (size_t x = 0; x < width; x += 2) {
            signed y1 = (signed)src[2 * x + 1] - 16;
            signed y2 = (signed)src[2 * x + 3] - 16;
            signed u = (signed)src[2 * x] - 128;
            signed v = (signed)src[2 * x + 2] - 128;

            signed u_b = u * 517;
            signed u_g = -u * 100;
            signed v_g = -v * 208;
            signed v_r = v * 409;

            signed tmp1 = y1 * 298;
            signed b1 = (tmp1 + u_b) / 256;
            signed g1 = (tmp1 + v_g + u_g) / 256;
            signed r1 = (tmp1 + v_r) / 256;

            signed tmp2 = y2 * 298;
            signed b2 = (tmp2 + u_b) / 256;
            signed g2 = (tmp2 + v_g + u_g) / 256;
            signed r2 = (tmp2 + v_r) / 256;

            uint32_t rgb1 =
                ((kAdjustedClip[r1] >> 3) << 11)
                | ((kAdjustedClip[g1] >> 2) << 5)
                | (kAdjustedClip[b1] >> 3);

            uint32_t rgb2 =
                ((kAdjustedClip[r2] >> 3) << 11)
                | ((kAdjustedClip[g2] >> 2) << 5)
                | (kAdjustedClip[b2] >> 3);

            dst_ptr[x / 2] = (rgb2 << 16) | rgb1;
        }

        src += width * 2;
        dst_ptr += dstSkip / 4;
    }
}

void ColorConverter::convertYUV420Planar(
        size_t width, size_t height,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip) {
    CHECK_EQ(srcSkip, 0);  // Doesn't really make sense for YUV formats.
    CHECK(dstSkip >= width * 2);
    CHECK((dstSkip & 3) == 0);

    uint8_t *kAdjustedClip = initClip();

    uint32_t *dst_ptr = (uint32_t *)dstBits;
    const uint8_t *src_y = (const uint8_t *)srcBits;

    const uint8_t *src_u =
        (const uint8_t *)src_y + width * height;

    const uint8_t *src_v =
        (const uint8_t *)src_u + (width / 2) * (height / 2);

    for (size_t y = 0; y < height; ++y) {
        for (size_t x = 0; x < width; x += 2) {
            // B = 1.164 * (Y - 16) + 2.018 * (U - 128)
            // G = 1.164 * (Y - 16) - 0.813 * (V - 128) - 0.391 * (U - 128)
            // R = 1.164 * (Y - 16) + 1.596 * (V - 128)

            // B = 298/256 * (Y - 16) + 517/256 * (U - 128)
            // G = .................. - 208/256 * (V - 128) - 100/256 * (U - 128)
            // R = .................. + 409/256 * (V - 128)

            // min_B = (298 * (- 16) + 517 * (- 128)) / 256 = -277
            // min_G = (298 * (- 16) - 208 * (255 - 128) - 100 * (255 - 128)) / 256 = -172
            // min_R = (298 * (- 16) + 409 * (- 128)) / 256 = -223

            // max_B = (298 * (255 - 16) + 517 * (255 - 128)) / 256 = 534
            // max_G = (298 * (255 - 16) - 208 * (- 128) - 100 * (- 128)) / 256 = 432
            // max_R = (298 * (255 - 16) + 409 * (255 - 128)) / 256 = 481

            // clip range -278 .. 535

            signed y1 = (signed)src_y[x] - 16;
            signed y2 = (signed)src_y[x + 1] - 16;

            signed u = (signed)src_u[x / 2] - 128;
            signed v = (signed)src_v[x / 2] - 128;

            signed u_b = u * 517;
            signed u_g = -u * 100;
            signed v_g = -v * 208;
            signed v_r = v * 409;

            signed tmp1 = y1 * 298;
            signed b1 = (tmp1 + u_b) / 256;
            signed g1 = (tmp1 + v_g + u_g) / 256;
            signed r1 = (tmp1 + v_r) / 256;

            signed tmp2 = y2 * 298;
            signed b2 = (tmp2 + u_b) / 256;
            signed g2 = (tmp2 + v_g + u_g) / 256;
            signed r2 = (tmp2 + v_r) / 256;

            uint32_t rgb1 =
                ((kAdjustedClip[r1] >> 3) << 11)
                | ((kAdjustedClip[g1] >> 2) << 5)
                | (kAdjustedClip[b1] >> 3);

            uint32_t rgb2 =
                ((kAdjustedClip[r2] >> 3) << 11)
                | ((kAdjustedClip[g2] >> 2) << 5)
                | (kAdjustedClip[b2] >> 3);

            dst_ptr[x / 2] = (rgb2 << 16) | rgb1;
        }

        src_y += width;

        if (y & 1) {
            src_u += width / 2;
            src_v += width / 2;
        }

        dst_ptr += dstSkip / 4;
    }
}

void ColorConverter::convertQCOMYUV420SemiPlanar(
        size_t width, size_t height,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip) {
    CHECK_EQ(srcSkip, 0);  // Doesn't really make sense for YUV formats.
    CHECK(dstSkip >= width * 2);
    CHECK((dstSkip & 3) == 0);

    uint8_t *kAdjustedClip = initClip();

    uint32_t *dst_ptr = (uint32_t *)dstBits;
    const uint8_t *src_y = (const uint8_t *)srcBits;

    const uint8_t *src_u =
        (const uint8_t *)src_y + width * height;

    for (size_t y = 0; y < height; ++y) {
        for (size_t x = 0; x < width; x += 2) {
            signed y1 = (signed)src_y[x] - 16;
            signed y2 = (signed)src_y[x + 1] - 16;

            signed u = (signed)src_u[x & ~1] - 128;
            signed v = (signed)src_u[(x & ~1) + 1] - 128;

            signed u_b = u * 517;
            signed u_g = -u * 100;
            signed v_g = -v * 208;
            signed v_r = v * 409;

            signed tmp1 = y1 * 298;
            signed b1 = (tmp1 + u_b) / 256;
            signed g1 = (tmp1 + v_g + u_g) / 256;
            signed r1 = (tmp1 + v_r) / 256;

            signed tmp2 = y2 * 298;
            signed b2 = (tmp2 + u_b) / 256;
            signed g2 = (tmp2 + v_g + u_g) / 256;
            signed r2 = (tmp2 + v_r) / 256;

            uint32_t rgb1 =
                ((kAdjustedClip[b1] >> 3) << 11)
                | ((kAdjustedClip[g1] >> 2) << 5)
                | (kAdjustedClip[r1] >> 3);

            uint32_t rgb2 =
                ((kAdjustedClip[b2] >> 3) << 11)
                | ((kAdjustedClip[g2] >> 2) << 5)
                | (kAdjustedClip[r2] >> 3);

            dst_ptr[x / 2] = (rgb2 << 16) | rgb1;
        }

        src_y += width;

        if (y & 1) {
            src_u += width;
        }

        dst_ptr += dstSkip / 4;
    }
}

void ColorConverter::convertYUV420SemiPlanar(
        size_t width, size_t height,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip) {
    CHECK_EQ(srcSkip, 0);  // Doesn't really make sense for YUV formats.
    CHECK(dstSkip >= width * 2);
    CHECK((dstSkip & 3) == 0);

    uint8_t *kAdjustedClip = initClip();

    uint32_t *dst_ptr = (uint32_t *)dstBits;
    const uint8_t *src_y = (const uint8_t *)srcBits;

    const uint8_t *src_u =
        (const uint8_t *)src_y + width * height;

    for (size_t y = 0; y < height; ++y) {
        for (size_t x = 0; x < width; x += 2) {
            signed y1 = (signed)src_y[x] - 16;
            signed y2 = (signed)src_y[x + 1] - 16;

            signed v = (signed)src_u[x & ~1] - 128;
            signed u = (signed)src_u[(x & ~1) + 1] - 128;

            signed u_b = u * 517;
            signed u_g = -u * 100;
            signed v_g = -v * 208;
            signed v_r = v * 409;

            signed tmp1 = y1 * 298;
            signed b1 = (tmp1 + u_b) / 256;
            signed g1 = (tmp1 + v_g + u_g) / 256;
            signed r1 = (tmp1 + v_r) / 256;

            signed tmp2 = y2 * 298;
            signed b2 = (tmp2 + u_b) / 256;
            signed g2 = (tmp2 + v_g + u_g) / 256;
            signed r2 = (tmp2 + v_r) / 256;

            uint32_t rgb1 =
                ((kAdjustedClip[b1] >> 3) << 11)
                | ((kAdjustedClip[g1] >> 2) << 5)
                | (kAdjustedClip[r1] >> 3);

            uint32_t rgb2 =
                ((kAdjustedClip[b2] >> 3) << 11)
                | ((kAdjustedClip[g2] >> 2) << 5)
                | (kAdjustedClip[r2] >> 3);

            dst_ptr[x / 2] = (rgb2 << 16) | rgb1;
        }

        src_y += width;

        if (y & 1) {
            src_u += width;
        }

        dst_ptr += dstSkip / 4;
    }
}


#if defined(OMAP_ENHANCEMENT) && defined(TARGET_OMAP4)
void ColorConverter::convertYUV420PackedSemiPlanar(
        size_t width, size_t height,
        size_t displaywidth, size_t displayheight, size_t nOffset,
        const void *srcBits, size_t srcSkip,
        void *dstBits, size_t dstSkip, bool interlaced) {

    CHECK((dstSkip & 3) == 0);

    size_t stride = width;

    if(srcSkip)
        stride = srcSkip;

    uint8_t *kAdjustedClip = initClip();

    uint32_t *dst_ptr = (uint32_t *)dstBits;
    const uint8_t *src_y = (const uint8_t *)srcBits;

    uint32_t offx = nOffset  % stride;
    uint32_t offy = nOffset / stride;


    const uint8_t *src_u = (const uint8_t *)(src_y-nOffset) + (stride * height);
    src_u += ( ( stride * (offy/2) ) + offx );

    const uint8_t *src_v = src_u + 1;

    for (size_t y = 0; y < displayheight; ++y) {
        for (size_t x = 0; x < displaywidth; x += 2) {

            signed y1 = (signed)src_y[x] - 16;    //Y pixel
            signed y2 = (signed)src_y[x + 1] - 16; //2nd Y pixel

            signed u = (signed)src_u[x & ~1] - 128;   //U component
            signed v = (signed)src_u[(x & ~1) + 1] - 128; //V component

            signed u_b = u * 517;
            signed u_g = -u * 100;
            signed v_g = -v * 208;
            signed v_r = v * 409;

            signed tmp1 = y1 * 298;
            signed b1 = (tmp1 + u_b) / 256;
            signed g1 = (tmp1 + v_g + u_g) / 256;
            signed r1 = (tmp1 + v_r) / 256;

            signed tmp2 = y2 * 298;
            signed b2 = (tmp2 + u_b) / 256;
            signed g2 = (tmp2 + v_g + u_g) / 256;
            signed r2 = (tmp2 + v_r) / 256;

            uint32_t rgb1 =
                ((kAdjustedClip[r1] >> 3) << 11)
                | ((kAdjustedClip[g1] >> 2) << 5)
                | (kAdjustedClip[b1] >> 3);

            uint32_t rgb2 =
                ((kAdjustedClip[r2] >> 3) << 11)
                | ((kAdjustedClip[g2] >> 2) << 5)
                | (kAdjustedClip[b2] >> 3);

            dst_ptr[x / 2] = (rgb2 << 16) | rgb1;
        }

        if(!interlaced){
            src_y += stride; //increment Y-pixel line
            if(y&1){
              src_u += stride; //increment U-V line
            }
        }
        else{
            /* Interlaced stream. Will have Sequential Top Bottom content*/
            /* Just extrapolate first half contnents from Y,UV planes */
            if(y&1){
                src_y += stride; //increment Y-pixel line, once for two lines
                if((y/2)&1){
                  src_u += stride; //increment U-V line, once for four lines
                }
            }
        }
        dst_ptr += dstSkip / 4;
    }
}
#endif

uint8_t *ColorConverter::initClip() {
    static const signed kClipMin = -278;
    static const signed kClipMax = 535;

    if (mClip == NULL) {
        mClip = new uint8_t[kClipMax - kClipMin + 1];

        for (signed i = kClipMin; i <= kClipMax; ++i) {
            mClip[i - kClipMin] = (i < 0) ? 0 : (i > 255) ? 255 : (uint8_t)i;
        }
    }

    return &mClip[-kClipMin];
}

}  // namespace android
