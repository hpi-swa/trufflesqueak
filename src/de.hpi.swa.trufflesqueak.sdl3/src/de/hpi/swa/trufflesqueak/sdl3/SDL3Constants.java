/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.sdl3;

public class SDL3Constants {
    // SDL_events.h
    public static final int SDL_EVENT_FIRST = 0;
    public static final int SDL_EVENT_QUIT = 0x100;
    public static final int SDL_EVENT_WINDOW_EXPOSED = 0x204;
    public static final int SDL_EVENT_WINDOW_RESIZED = 0x206;
    public static final int SDL_EVENT_WINDOW_MINIMIZED = 0x209;
    public static final int SDL_EVENT_WINDOW_MOUSE_LEAVE = 0x20D;
    public static final int SDL_EVENT_WINDOW_FOCUS_GAINED = 0x20E;
    public static final int SDL_EVENT_WINDOW_FOCUS_LOST = 0x20F;
    public static final int SDL_EVENT_WINDOW_CLOSE_REQUESTED = 0x210;
    public static final int SDL_EVENT_WINDOW_DISPLAY_CHANGED = 0x213;
    public static final int SDL_EVENT_KEY_DOWN = 0x300;
    public static final int SDL_EVENT_KEY_UP = 0x301;
    public static final int SDL_EVENT_TEXT_EDITING = 0x302;
    public static final int SDL_EVENT_TEXT_INPUT = 0x303;
    public static final int SDL_EVENT_MOUSE_MOTION = 0x400;
    public static final int SDL_EVENT_MOUSE_BUTTON_DOWN = 0x401;
    public static final int SDL_EVENT_MOUSE_BUTTON_UP = 0x402;
    public static final int SDL_EVENT_MOUSE_WHEEL = 0x403;
    public static final int SDL_EVENT_FINGER_DOWN = 0x700;
    public static final int SDL_EVENT_FINGER_UP = 0x701;
    public static final int SDL_EVENT_FINGER_MOTION = 0x702;
    public static final int SDL_EVENT_DROP_FILE = 0x1000;
    public static final int SDL_EVENT_DROP_BEGIN = 0x1002;
    public static final int SDL_EVENT_DROP_COMPLETE = 0x1003;
    public static final int SDL_EVENT_DROP_POSITION = 0x1004;
    public static final int SDL_EVENT_RENDER_TARGETS_RESET = 0x2000;
    public static final int SDL_EVENT_RENDER_DEVICE_RESET = 0x2001;
    public static final int SDL_EVENT_USER = 0x8000;
    public static final int SDL_EVENT_LAST = 0xFFFF;
    public static final int SDL_GETEVENT = 2;

    // SDL_hints.h
    public static final String SDL_HINT_MAC_BACKGROUND_APP = "SDL_MAC_BACKGROUND_APP";
    public static final String SDL_HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK = "SDL_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK";
    public static final String SDL_HINT_RENDER_VSYNC = "SDL_RENDER_VSYNC";
    public static final String SDL_HINT_VIDEO_X11_NET_WM_PING = "SDL_VIDEO_X11_NET_WM_PING";

    // SDL_init.h
    public static final int SDL_INIT_VIDEO = 0x00000020;

    // SDL_keycode.h
    public static final int SDLK_RETURN = 0x0000000d;
    public static final int SDLK_ESCAPE = 0x0000001b;
    public static final int SDLK_BACKSPACE = 0x00000008;
    public static final int SDLK_TAB = 0x00000009;
    public static final int SDLK_SPACE = 0x00000020;
    public static final int SDLK_DELETE = 0x0000007f;
    public static final int SDLK_INSERT = 0x40000049;
    public static final int SDLK_HOME = 0x4000004A;
    public static final int SDLK_PAGEUP = 0x4000004B;
    public static final int SDLK_END = 0x4000004D;
    public static final int SDLK_PAGEDOWN = 0x4000004E;
    public static final int SDLK_RIGHT = 0x4000004F;
    public static final int SDLK_LEFT = 0x40000050;
    public static final int SDLK_DOWN = 0x40000051;
    public static final int SDLK_UP = 0x40000052;
    public static final int SDLK_KP_DIVIDE = 0x40000054;
    public static final int SDLK_KP_MULTIPLY = 0x40000055;
    public static final int SDLK_KP_MINUS = 0x40000056;
    public static final int SDLK_KP_PLUS = 0x40000057;
    public static final int SDLK_KP_ENTER = 0x40000058;
    public static final int SDLK_KP_1 = 0x40000059;
    public static final int SDLK_KP_2 = 0x4000005A;
    public static final int SDLK_KP_3 = 0x4000005B;
    public static final int SDLK_KP_4 = 0x4000005C;
    public static final int SDLK_KP_5 = 0x4000005D;
    public static final int SDLK_KP_6 = 0x4000005E;
    public static final int SDLK_KP_7 = 0x4000005F;
    public static final int SDLK_KP_8 = 0x40000060;
    public static final int SDLK_KP_9 = 0x40000061;
    public static final int SDLK_KP_0 = 0x40000062;
    public static final int SDLK_KP_PERIOD = 0x40000063;
    public static final int SDLK_LCTRL = 0x400000E0;
    public static final int SDLK_LSHIFT = 0x400000E1;
    public static final int SDLK_LALT = 0x400000E2;
    public static final int SDLK_LGUI = 0x400000E3;
    public static final int SDLK_RCTRL = 0x400000E4;
    public static final int SDLK_RSHIFT = 0x400000E5;
    public static final int SDLK_RALT = 0x400000E6;
    public static final int SDLK_RGUI = 0x400000E7;
    public static final int SDL_KMOD_LSHIFT = 0x0001;
    public static final int SDL_KMOD_RSHIFT = 0x0002;
    public static final int SDL_KMOD_LCTRL = 0x0040;
    public static final int SDL_KMOD_RCTRL = 0x0080;
    public static final int SDL_KMOD_LALT = 0x0100;
    public static final int SDL_KMOD_RALT = 0x0200;
    public static final int SDL_KMOD_LGUI = 0x0400;
    public static final int SDL_KMOD_RGUI = 0x0800;
    public static final int SDL_KMOD_MODE = 0x4000;

    // SDL_mouse.h
    public static final int SDL_BUTTON_LEFT = 1;
    public static final int SDL_BUTTON_MIDDLE = 2;
    public static final int SDL_BUTTON_RIGHT = 3;

    // SDL_pixels.h
    public static final int SDL_PIXELFORMAT_ARGB8888 = 0x16362004;

    // SDL_render.h
    public static final int SDL_TEXTUREACCESS_STREAMING = 1;

    // SDL_surface.h
    public static final int SDL_SCALEMODE_NEAREST = 0;

    // SDL_video.h
    public static final long SDL_WINDOW_RESIZABLE = 0x0000000000000020L;
    public static final long SDL_WINDOW_HIGH_PIXEL_DENSITY = 0x0000000000002000L;

    private SDL3Constants() {
    }
}
