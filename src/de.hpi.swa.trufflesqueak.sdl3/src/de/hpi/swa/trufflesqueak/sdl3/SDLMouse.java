/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.sdl3;

public class SDLMouse {

    public static final int SDL_SYSTEM_CURSOR_DEFAULT = 0,
                    SDL_SYSTEM_CURSOR_TEXT = 1,
                    SDL_SYSTEM_CURSOR_WAIT = 2,
                    SDL_SYSTEM_CURSOR_CROSSHAIR = 3,
                    SDL_SYSTEM_CURSOR_PROGRESS = 4,
                    SDL_SYSTEM_CURSOR_NWSE_RESIZE = 5,
                    SDL_SYSTEM_CURSOR_NESW_RESIZE = 6,
                    SDL_SYSTEM_CURSOR_EW_RESIZE = 7,
                    SDL_SYSTEM_CURSOR_NS_RESIZE = 8,
                    SDL_SYSTEM_CURSOR_MOVE = 9,
                    SDL_SYSTEM_CURSOR_NOT_ALLOWED = 10,
                    SDL_SYSTEM_CURSOR_POINTER = 11,
                    SDL_SYSTEM_CURSOR_NW_RESIZE = 12,
                    SDL_SYSTEM_CURSOR_N_RESIZE = 13,
                    SDL_SYSTEM_CURSOR_NE_RESIZE = 14,
                    SDL_SYSTEM_CURSOR_E_RESIZE = 15,
                    SDL_SYSTEM_CURSOR_SE_RESIZE = 16,
                    SDL_SYSTEM_CURSOR_S_RESIZE = 17,
                    SDL_SYSTEM_CURSOR_SW_RESIZE = 18,
                    SDL_SYSTEM_CURSOR_W_RESIZE = 19,
                    SDL_SYSTEM_CURSOR_COUNT = 20;

    public static final int SDL_MOUSEWHEEL_NORMAL = 0,
                    SDL_MOUSEWHEEL_FLIPPED = 1;

    public static final int SDL_BUTTON_LEFT = 1,
                    SDL_BUTTON_MIDDLE = 2,
                    SDL_BUTTON_RIGHT = 3,
                    SDL_BUTTON_X1 = 4,
                    SDL_BUTTON_X2 = 5;

    public static final int SDL_BUTTON_LMASK = SDL_BUTTON_MASK(SDL_BUTTON_LEFT),
                    SDL_BUTTON_MMASK = SDL_BUTTON_MASK(SDL_BUTTON_MIDDLE),
                    SDL_BUTTON_RMASK = SDL_BUTTON_MASK(SDL_BUTTON_RIGHT),
                    SDL_BUTTON_X1MASK = SDL_BUTTON_MASK(SDL_BUTTON_X1),
                    SDL_BUTTON_X2MASK = SDL_BUTTON_MASK(SDL_BUTTON_X2);

    protected SDLMouse() {
        throw new UnsupportedOperationException();
    }

    private static int SDL_BUTTON_MASK(final int X) {
        return 1 << (X - 1);
    }

}
