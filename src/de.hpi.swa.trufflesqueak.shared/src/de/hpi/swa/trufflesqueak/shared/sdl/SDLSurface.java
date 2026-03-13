/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared.sdl;

public class SDLSurface {

    public static final int
            SDL_SURFACE_PREALLOCATED = 0x00000001,
            SDL_SURFACE_LOCK_NEEDED  = 0x00000002,
            SDL_SURFACE_LOCKED       = 0x00000004,
            SDL_SURFACE_SIMD_ALIGNED = 0x00000008;

    public static final int
            SDL_SCALEMODE_INVALID  = -1,
            SDL_SCALEMODE_NEAREST  = 0,
            SDL_SCALEMODE_LINEAR   = 1,
            SDL_SCALEMODE_PIXELART = 2;

    public static final int
            SDL_FLIP_NONE                    = 0,
            SDL_FLIP_HORIZONTAL              = 1,
            SDL_FLIP_VERTICAL                = 2,
            SDL_FLIP_HORIZONTAL_AND_VERTICAL = (SDL_FLIP_HORIZONTAL | SDL_FLIP_VERTICAL);

    public static final String
            SDL_PROP_SURFACE_SDR_WHITE_POINT_FLOAT   = "SDL.surface.SDR_white_point",
            SDL_PROP_SURFACE_HDR_HEADROOM_FLOAT      = "SDL.surface.HDR_headroom",
            SDL_PROP_SURFACE_TONEMAP_OPERATOR_STRING = "SDL.surface.tonemap",
            SDL_PROP_SURFACE_HOTSPOT_X_NUMBER        = "SDL.surface.hotspot.x",
            SDL_PROP_SURFACE_HOTSPOT_Y_NUMBER        = "SDL.surface.hotspot.y",
            SDL_PROP_SURFACE_ROTATION_FLOAT          = "SDL.surface.rotation";

    protected SDLSurface() {
        throw new UnsupportedOperationException();
    }
}
