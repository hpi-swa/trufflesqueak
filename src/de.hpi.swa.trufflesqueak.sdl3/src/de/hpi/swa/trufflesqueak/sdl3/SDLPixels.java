/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.sdl3;

import java.nio.ByteOrder;

public class SDLPixels {

    public static final int SDL_ALPHA_OPAQUE = 255,
                    SDL_ALPHA_TRANSPARENT = 0;

    public static final float SDL_ALPHA_OPAQUE_FLOAT = 1.0f,
                    SDL_ALPHA_TRANSPARENT_FLOAT = 0.0f;

    public static final int SDL_PIXELTYPE_UNKNOWN = 0,
                    SDL_PIXELTYPE_INDEX1 = 1,
                    SDL_PIXELTYPE_INDEX4 = 2,
                    SDL_PIXELTYPE_INDEX8 = 3,
                    SDL_PIXELTYPE_PACKED8 = 4,
                    SDL_PIXELTYPE_PACKED16 = 5,
                    SDL_PIXELTYPE_PACKED32 = 6,
                    SDL_PIXELTYPE_ARRAYU8 = 7,
                    SDL_PIXELTYPE_ARRAYU16 = 8,
                    SDL_PIXELTYPE_ARRAYU32 = 9,
                    SDL_PIXELTYPE_ARRAYF16 = 10,
                    SDL_PIXELTYPE_ARRAYF32 = 11,
                    SDL_PIXELTYPE_INDEX2 = 12;

    public static final int SDL_BITMAPORDER_NONE = 0,
                    SDL_BITMAPORDER_4321 = 1,
                    SDL_BITMAPORDER_1234 = 2;

    public static final int SDL_PACKEDORDER_NONE = 0,
                    SDL_PACKEDORDER_XRGB = 1,
                    SDL_PACKEDORDER_RGBX = 2,
                    SDL_PACKEDORDER_ARGB = 3,
                    SDL_PACKEDORDER_RGBA = 4,
                    SDL_PACKEDORDER_XBGR = 5,
                    SDL_PACKEDORDER_BGRX = 6,
                    SDL_PACKEDORDER_ABGR = 7,
                    SDL_PACKEDORDER_BGRA = 8;

    public static final int SDL_ARRAYORDER_NONE = 0,
                    SDL_ARRAYORDER_RGB = 1,
                    SDL_ARRAYORDER_RGBA = 2,
                    SDL_ARRAYORDER_ARGB = 3,
                    SDL_ARRAYORDER_BGR = 4,
                    SDL_ARRAYORDER_BGRA = 5,
                    SDL_ARRAYORDER_ABGR = 6;

    public static final int SDL_PACKEDLAYOUT_NONE = 0,
                    SDL_PACKEDLAYOUT_332 = 1,
                    SDL_PACKEDLAYOUT_4444 = 2,
                    SDL_PACKEDLAYOUT_1555 = 3,
                    SDL_PACKEDLAYOUT_5551 = 4,
                    SDL_PACKEDLAYOUT_565 = 5,
                    SDL_PACKEDLAYOUT_8888 = 6,
                    SDL_PACKEDLAYOUT_2101010 = 7,
                    SDL_PACKEDLAYOUT_1010102 = 8;

    public static final int SDL_PIXELFORMAT_UNKNOWN = 0,
                    SDL_PIXELFORMAT_INDEX1LSB = 0x11100100,
                    SDL_PIXELFORMAT_INDEX1MSB = 0x11200100,
                    SDL_PIXELFORMAT_INDEX2LSB = 0x1c100200,
                    SDL_PIXELFORMAT_INDEX2MSB = 0x1c200200,
                    SDL_PIXELFORMAT_INDEX4LSB = 0x12100400,
                    SDL_PIXELFORMAT_INDEX4MSB = 0x12200400,
                    SDL_PIXELFORMAT_INDEX8 = 0x13000801,
                    SDL_PIXELFORMAT_RGB332 = 0x14110801,
                    SDL_PIXELFORMAT_XRGB4444 = 0x15120c02,
                    SDL_PIXELFORMAT_XBGR4444 = 0x15520c02,
                    SDL_PIXELFORMAT_XRGB1555 = 0x15130f02,
                    SDL_PIXELFORMAT_XBGR1555 = 0x15530f02,
                    SDL_PIXELFORMAT_ARGB4444 = 0x15321002,
                    SDL_PIXELFORMAT_RGBA4444 = 0x15421002,
                    SDL_PIXELFORMAT_ABGR4444 = 0x15721002,
                    SDL_PIXELFORMAT_BGRA4444 = 0x15821002,
                    SDL_PIXELFORMAT_ARGB1555 = 0x15331002,
                    SDL_PIXELFORMAT_RGBA5551 = 0x15441002,
                    SDL_PIXELFORMAT_ABGR1555 = 0x15731002,
                    SDL_PIXELFORMAT_BGRA5551 = 0x15841002,
                    SDL_PIXELFORMAT_RGB565 = 0x15151002,
                    SDL_PIXELFORMAT_BGR565 = 0x15551002,
                    SDL_PIXELFORMAT_RGB24 = 0x17101803,
                    SDL_PIXELFORMAT_BGR24 = 0x17401803,
                    SDL_PIXELFORMAT_XRGB8888 = 0x16161804,
                    SDL_PIXELFORMAT_RGBX8888 = 0x16261804,
                    SDL_PIXELFORMAT_XBGR8888 = 0x16561804,
                    SDL_PIXELFORMAT_BGRX8888 = 0x16661804,
                    SDL_PIXELFORMAT_ARGB8888 = 0x16362004,
                    SDL_PIXELFORMAT_RGBA8888 = 0x16462004,
                    SDL_PIXELFORMAT_ABGR8888 = 0x16762004,
                    SDL_PIXELFORMAT_BGRA8888 = 0x16862004,
                    SDL_PIXELFORMAT_XRGB2101010 = 0x16172004,
                    SDL_PIXELFORMAT_XBGR2101010 = 0x16572004,
                    SDL_PIXELFORMAT_ARGB2101010 = 0x16372004,
                    SDL_PIXELFORMAT_ABGR2101010 = 0x16772004,
                    SDL_PIXELFORMAT_RGB48 = 0x18103006,
                    SDL_PIXELFORMAT_BGR48 = 0x18403006,
                    SDL_PIXELFORMAT_RGBA64 = 0x18204008,
                    SDL_PIXELFORMAT_ARGB64 = 0x18304008,
                    SDL_PIXELFORMAT_BGRA64 = 0x18504008,
                    SDL_PIXELFORMAT_ABGR64 = 0x18604008,
                    SDL_PIXELFORMAT_RGB48_FLOAT = 0x1a103006,
                    SDL_PIXELFORMAT_BGR48_FLOAT = 0x1a403006,
                    SDL_PIXELFORMAT_RGBA64_FLOAT = 0x1a204008,
                    SDL_PIXELFORMAT_ARGB64_FLOAT = 0x1a304008,
                    SDL_PIXELFORMAT_BGRA64_FLOAT = 0x1a504008,
                    SDL_PIXELFORMAT_ABGR64_FLOAT = 0x1a604008,
                    SDL_PIXELFORMAT_RGB96_FLOAT = 0x1b10600c,
                    SDL_PIXELFORMAT_BGR96_FLOAT = 0x1b40600c,
                    SDL_PIXELFORMAT_RGBA128_FLOAT = 0x1b208010,
                    SDL_PIXELFORMAT_ARGB128_FLOAT = 0x1b308010,
                    SDL_PIXELFORMAT_BGRA128_FLOAT = 0x1b508010,
                    SDL_PIXELFORMAT_ABGR128_FLOAT = 0x1b608010,
                    SDL_PIXELFORMAT_YV12 = 0x32315659,
                    SDL_PIXELFORMAT_IYUV = 0x56555949,
                    SDL_PIXELFORMAT_YUY2 = 0x32595559,
                    SDL_PIXELFORMAT_UYVY = 0x59565955,
                    SDL_PIXELFORMAT_YVYU = 0x55595659,
                    SDL_PIXELFORMAT_NV12 = 0x3231564e,
                    SDL_PIXELFORMAT_NV21 = 0x3132564e,
                    SDL_PIXELFORMAT_P010 = 0x30313050,
                    SDL_PIXELFORMAT_EXTERNAL_OES = 0x2053454f,
                    SDL_PIXELFORMAT_MJPG = 0x47504a4d,
                    SDL_PIXELFORMAT_RGBA32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_ABGR8888 : SDL_PIXELFORMAT_RGBA8888,
                    SDL_PIXELFORMAT_ARGB32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_BGRA8888 : SDL_PIXELFORMAT_ARGB8888,
                    SDL_PIXELFORMAT_BGRA32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_ARGB8888 : SDL_PIXELFORMAT_BGRA8888,
                    SDL_PIXELFORMAT_ABGR32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_RGBA8888 : SDL_PIXELFORMAT_ABGR8888,
                    SDL_PIXELFORMAT_RGBX32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_XBGR8888 : SDL_PIXELFORMAT_RGBX8888,
                    SDL_PIXELFORMAT_XRGB32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_BGRX8888 : SDL_PIXELFORMAT_XRGB8888,
                    SDL_PIXELFORMAT_BGRX32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_XRGB8888 : SDL_PIXELFORMAT_BGRX8888,
                    SDL_PIXELFORMAT_XBGR32 = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? SDL_PIXELFORMAT_RGBX8888 : SDL_PIXELFORMAT_XBGR8888;

    public static final int SDL_COLOR_TYPE_UNKNOWN = 0,
                    SDL_COLOR_TYPE_RGB = 1,
                    SDL_COLOR_TYPE_YCBCR = 2;

    public static final int SDL_COLOR_RANGE_UNKNOWN = 0,
                    SDL_COLOR_RANGE_LIMITED = 1,
                    SDL_COLOR_RANGE_FULL = 2;

    public static final int SDL_COLOR_PRIMARIES_UNKNOWN = 0,
                    SDL_COLOR_PRIMARIES_BT709 = 1,
                    SDL_COLOR_PRIMARIES_UNSPECIFIED = 2,
                    SDL_COLOR_PRIMARIES_BT470M = 4,
                    SDL_COLOR_PRIMARIES_BT470BG = 5,
                    SDL_COLOR_PRIMARIES_BT601 = 6,
                    SDL_COLOR_PRIMARIES_SMPTE240 = 7,
                    SDL_COLOR_PRIMARIES_GENERIC_FILM = 8,
                    SDL_COLOR_PRIMARIES_BT2020 = 9,
                    SDL_COLOR_PRIMARIES_XYZ = 10,
                    SDL_COLOR_PRIMARIES_SMPTE431 = 11,
                    SDL_COLOR_PRIMARIES_SMPTE432 = 12,
                    SDL_COLOR_PRIMARIES_EBU3213 = 22,
                    SDL_COLOR_PRIMARIES_CUSTOM = 31;

    public static final int SDL_TRANSFER_CHARACTERISTICS_UNKNOWN = 0,
                    SDL_TRANSFER_CHARACTERISTICS_BT709 = 1,
                    SDL_TRANSFER_CHARACTERISTICS_UNSPECIFIED = 2,
                    SDL_TRANSFER_CHARACTERISTICS_GAMMA22 = 4,
                    SDL_TRANSFER_CHARACTERISTICS_GAMMA28 = 5,
                    SDL_TRANSFER_CHARACTERISTICS_BT601 = 6,
                    SDL_TRANSFER_CHARACTERISTICS_SMPTE240 = 7,
                    SDL_TRANSFER_CHARACTERISTICS_LINEAR = 8,
                    SDL_TRANSFER_CHARACTERISTICS_LOG100 = 9,
                    SDL_TRANSFER_CHARACTERISTICS_LOG100_SQRT10 = 10,
                    SDL_TRANSFER_CHARACTERISTICS_IEC61966 = 11,
                    SDL_TRANSFER_CHARACTERISTICS_BT1361 = 12,
                    SDL_TRANSFER_CHARACTERISTICS_SRGB = 13,
                    SDL_TRANSFER_CHARACTERISTICS_BT2020_10BIT = 14,
                    SDL_TRANSFER_CHARACTERISTICS_BT2020_12BIT = 15,
                    SDL_TRANSFER_CHARACTERISTICS_PQ = 16,
                    SDL_TRANSFER_CHARACTERISTICS_SMPTE428 = 17,
                    SDL_TRANSFER_CHARACTERISTICS_HLG = 18,
                    SDL_TRANSFER_CHARACTERISTICS_CUSTOM = 31;

    public static final int SDL_MATRIX_COEFFICIENTS_IDENTITY = 0,
                    SDL_MATRIX_COEFFICIENTS_BT709 = 1,
                    SDL_MATRIX_COEFFICIENTS_UNSPECIFIED = 2,
                    SDL_MATRIX_COEFFICIENTS_FCC = 4,
                    SDL_MATRIX_COEFFICIENTS_BT470BG = 5,
                    SDL_MATRIX_COEFFICIENTS_BT601 = 6,
                    SDL_MATRIX_COEFFICIENTS_SMPTE240 = 7,
                    SDL_MATRIX_COEFFICIENTS_YCGCO = 8,
                    SDL_MATRIX_COEFFICIENTS_BT2020_NCL = 9,
                    SDL_MATRIX_COEFFICIENTS_BT2020_CL = 10,
                    SDL_MATRIX_COEFFICIENTS_SMPTE2085 = 11,
                    SDL_MATRIX_COEFFICIENTS_CHROMA_DERIVED_NCL = 12,
                    SDL_MATRIX_COEFFICIENTS_CHROMA_DERIVED_CL = 13,
                    SDL_MATRIX_COEFFICIENTS_ICTCP = 14,
                    SDL_MATRIX_COEFFICIENTS_CUSTOM = 31;

    public static final int SDL_CHROMA_LOCATION_NONE = 0,
                    SDL_CHROMA_LOCATION_LEFT = 1,
                    SDL_CHROMA_LOCATION_CENTER = 2,
                    SDL_CHROMA_LOCATION_TOPLEFT = 3;

    public static final int SDL_COLORSPACE_UNKNOWN = 0,
                    SDL_COLORSPACE_SRGB = 0x120005a0,
                    SDL_COLORSPACE_SRGB_LINEAR = 0x12000500,
                    SDL_COLORSPACE_HDR10 = 0x12002600,
                    SDL_COLORSPACE_JPEG = 0x220004c6,
                    SDL_COLORSPACE_BT601_LIMITED = 0x211018c6,
                    SDL_COLORSPACE_BT601_FULL = 0x221018c6,
                    SDL_COLORSPACE_BT709_LIMITED = 0x21100421,
                    SDL_COLORSPACE_BT709_FULL = 0x22100421,
                    SDL_COLORSPACE_BT2020_LIMITED = 0x21102609,
                    SDL_COLORSPACE_BT2020_FULL = 0x22102609,
                    SDL_COLORSPACE_RGB_DEFAULT = SDL_COLORSPACE_SRGB,
                    SDL_COLORSPACE_YUV_DEFAULT = SDL_COLORSPACE_BT601_LIMITED;

    protected SDLPixels() {
        throw new UnsupportedOperationException();
    }
}
