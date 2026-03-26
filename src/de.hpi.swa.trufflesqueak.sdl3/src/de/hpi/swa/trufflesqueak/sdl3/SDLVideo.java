/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.sdl3;

public class SDLVideo {

    public static final String SDL_PROP_GLOBAL_VIDEO_WAYLAND_WL_DISPLAY_POINTER = "SDL.video.wayland.wl_display";

    public static final int SDL_SYSTEM_THEME_UNKNOWN = 0,
                    SDL_SYSTEM_THEME_LIGHT = 1,
                    SDL_SYSTEM_THEME_DARK = 2;

    public static final int SDL_ORIENTATION_UNKNOWN = 0,
                    SDL_ORIENTATION_LANDSCAPE = 1,
                    SDL_ORIENTATION_LANDSCAPE_FLIPPED = 2,
                    SDL_ORIENTATION_PORTRAIT = 3,
                    SDL_ORIENTATION_PORTRAIT_FLIPPED = 4;

    public static final long SDL_WINDOW_FULLSCREEN = 0x0000000000000001L,
                    SDL_WINDOW_OPENGL = 0x0000000000000002L,
                    SDL_WINDOW_OCCLUDED = 0x0000000000000004L,
                    SDL_WINDOW_HIDDEN = 0x0000000000000008L,
                    SDL_WINDOW_BORDERLESS = 0x0000000000000010L,
                    SDL_WINDOW_RESIZABLE = 0x0000000000000020L,
                    SDL_WINDOW_MINIMIZED = 0x0000000000000040L,
                    SDL_WINDOW_MAXIMIZED = 0x0000000000000080L,
                    SDL_WINDOW_MOUSE_GRABBED = 0x0000000000000100L,
                    SDL_WINDOW_INPUT_FOCUS = 0x0000000000000200L,
                    SDL_WINDOW_MOUSE_FOCUS = 0x0000000000000400L,
                    SDL_WINDOW_EXTERNAL = 0x0000000000000800L,
                    SDL_WINDOW_MODAL = 0x0000000000001000L,
                    SDL_WINDOW_HIGH_PIXEL_DENSITY = 0x0000000000002000L,
                    SDL_WINDOW_MOUSE_CAPTURE = 0x0000000000004000L,
                    SDL_WINDOW_MOUSE_RELATIVE_MODE = 0x0000000000008000L,
                    SDL_WINDOW_ALWAYS_ON_TOP = 0x0000000000010000L,
                    SDL_WINDOW_UTILITY = 0x0000000000020000L,
                    SDL_WINDOW_TOOLTIP = 0x0000000000040000L,
                    SDL_WINDOW_POPUP_MENU = 0x0000000000080000L,
                    SDL_WINDOW_KEYBOARD_GRABBED = 0x0000000000100000L,
                    SDL_WINDOW_FILL_DOCUMENT = 0x0000000000200000L,
                    SDL_WINDOW_VULKAN = 0x0000000010000000L,
                    SDL_WINDOW_METAL = 0x0000000020000000L,
                    SDL_WINDOW_TRANSPARENT = 0x0000000040000000L,
                    SDL_WINDOW_NOT_FOCUSABLE = 0x0000000080000000L;

    public static final int SDL_WINDOWPOS_UNDEFINED_MASK = 0x1FFF0000,
                    SDL_WINDOWPOS_UNDEFINED = SDL_WINDOWPOS_UNDEFINED_DISPLAY(0),
                    SDL_WINDOWPOS_CENTERED_MASK = 0x2FFF0000,
                    SDL_WINDOWPOS_CENTERED = SDL_WINDOWPOS_CENTERED_DISPLAY(0);

    public static final int SDL_FLASH_CANCEL = 0,
                    SDL_FLASH_BRIEFLY = 1,
                    SDL_FLASH_UNTIL_FOCUSED = 2;

    public static final int SDL_PROGRESS_STATE_INVALID = -1,
                    SDL_PROGRESS_STATE_NONE = 0,
                    SDL_PROGRESS_STATE_INDETERMINATE = 1,
                    SDL_PROGRESS_STATE_NORMAL = 2,
                    SDL_PROGRESS_STATE_PAUSED = 3,
                    SDL_PROGRESS_STATE_ERROR = 4;

    public static final int SDL_GL_RED_SIZE = 0,
                    SDL_GL_GREEN_SIZE = 1,
                    SDL_GL_BLUE_SIZE = 2,
                    SDL_GL_ALPHA_SIZE = 3,
                    SDL_GL_BUFFER_SIZE = 4,
                    SDL_GL_DOUBLEBUFFER = 5,
                    SDL_GL_DEPTH_SIZE = 6,
                    SDL_GL_STENCIL_SIZE = 7,
                    SDL_GL_ACCUM_RED_SIZE = 8,
                    SDL_GL_ACCUM_GREEN_SIZE = 9,
                    SDL_GL_ACCUM_BLUE_SIZE = 10,
                    SDL_GL_ACCUM_ALPHA_SIZE = 11,
                    SDL_GL_STEREO = 12,
                    SDL_GL_MULTISAMPLEBUFFERS = 13,
                    SDL_GL_MULTISAMPLESAMPLES = 14,
                    SDL_GL_ACCELERATED_VISUAL = 15,
                    SDL_GL_RETAINED_BACKING = 16,
                    SDL_GL_CONTEXT_MAJOR_VERSION = 17,
                    SDL_GL_CONTEXT_MINOR_VERSION = 18,
                    SDL_GL_CONTEXT_FLAGS = 19,
                    SDL_GL_CONTEXT_PROFILE_MASK = 20,
                    SDL_GL_SHARE_WITH_CURRENT_CONTEXT = 21,
                    SDL_GL_FRAMEBUFFER_SRGB_CAPABLE = 22,
                    SDL_GL_CONTEXT_RELEASE_BEHAVIOR = 23,
                    SDL_GL_CONTEXT_RESET_NOTIFICATION = 24,
                    SDL_GL_CONTEXT_NO_ERROR = 25,
                    SDL_GL_FLOATBUFFERS = 26,
                    SDL_GL_EGL_PLATFORM = 27;

    public static final int SDL_GL_CONTEXT_PROFILE_CORE = 0x0001,
                    SDL_GL_CONTEXT_PROFILE_COMPATIBILITY = 0x0002,
                    SDL_GL_CONTEXT_PROFILE_ES = 0x0004;

    public static final int SDL_GL_CONTEXT_DEBUG_FLAG = 0x0001,
                    SDL_GL_CONTEXT_FORWARD_COMPATIBLE_FLAG = 0x0002,
                    SDL_GL_CONTEXT_ROBUST_ACCESS_FLAG = 0x0004,
                    SDL_GL_CONTEXT_RESET_ISOLATION_FLAG = 0x0008;

    public static final int SDL_GL_CONTEXT_RELEASE_BEHAVIOR_NONE = 0x0000,
                    SDL_GL_CONTEXT_RELEASE_BEHAVIOR_FLUSH = 0x0001;

    public static final int SDL_GL_CONTEXT_RESET_NO_NOTIFICATION = 0x0000,
                    SDL_GL_CONTEXT_RESET_LOSE_CONTEXT = 0x0001;

    public static final String SDL_PROP_DISPLAY_HDR_ENABLED_BOOLEAN = "SDL.display.HDR_enabled",
                    SDL_PROP_DISPLAY_KMSDRM_PANEL_ORIENTATION_NUMBER = "SDL.display.KMSDRM.panel_orientation",
                    SDL_PROP_DISPLAY_WAYLAND_WL_OUTPUT_POINTER = "SDL.display.wayland.wl_output",
                    SDL_PROP_DISPLAY_WINDOWS_HMONITOR_POINTER = "SDL.display.windows.hmonitor";

    public static final String SDL_PROP_WINDOW_CREATE_ALWAYS_ON_TOP_BOOLEAN = "SDL.window.create.always_on_top",
                    SDL_PROP_WINDOW_CREATE_BORDERLESS_BOOLEAN = "SDL.window.create.borderless",
                    SDL_PROP_WINDOW_CREATE_CONSTRAIN_POPUP_BOOLEAN = "SDL.window.create.constrain_popup",
                    SDL_PROP_WINDOW_CREATE_FOCUSABLE_BOOLEAN = "SDL.window.create.focusable",
                    SDL_PROP_WINDOW_CREATE_EXTERNAL_GRAPHICS_CONTEXT_BOOLEAN = "SDL.window.create.external_graphics_context",
                    SDL_PROP_WINDOW_CREATE_FLAGS_NUMBER = "SDL.window.create.flags",
                    SDL_PROP_WINDOW_CREATE_FULLSCREEN_BOOLEAN = "SDL.window.create.fullscreen",
                    SDL_PROP_WINDOW_CREATE_HEIGHT_NUMBER = "SDL.window.create.height",
                    SDL_PROP_WINDOW_CREATE_HIDDEN_BOOLEAN = "SDL.window.create.hidden",
                    SDL_PROP_WINDOW_CREATE_HIGH_PIXEL_DENSITY_BOOLEAN = "SDL.window.create.high_pixel_density",
                    SDL_PROP_WINDOW_CREATE_MAXIMIZED_BOOLEAN = "SDL.window.create.maximized",
                    SDL_PROP_WINDOW_CREATE_MENU_BOOLEAN = "SDL.window.create.menu",
                    SDL_PROP_WINDOW_CREATE_METAL_BOOLEAN = "SDL.window.create.metal",
                    SDL_PROP_WINDOW_CREATE_MINIMIZED_BOOLEAN = "SDL.window.create.minimized",
                    SDL_PROP_WINDOW_CREATE_MODAL_BOOLEAN = "SDL.window.create.modal",
                    SDL_PROP_WINDOW_CREATE_MOUSE_GRABBED_BOOLEAN = "SDL.window.create.mouse_grabbed",
                    SDL_PROP_WINDOW_CREATE_OPENGL_BOOLEAN = "SDL.window.create.opengl",
                    SDL_PROP_WINDOW_CREATE_PARENT_POINTER = "SDL.window.create.parent",
                    SDL_PROP_WINDOW_CREATE_RESIZABLE_BOOLEAN = "SDL.window.create.resizable",
                    SDL_PROP_WINDOW_CREATE_TITLE_STRING = "SDL.window.create.title",
                    SDL_PROP_WINDOW_CREATE_TRANSPARENT_BOOLEAN = "SDL.window.create.transparent",
                    SDL_PROP_WINDOW_CREATE_TOOLTIP_BOOLEAN = "SDL.window.create.tooltip",
                    SDL_PROP_WINDOW_CREATE_UTILITY_BOOLEAN = "SDL.window.create.utility",
                    SDL_PROP_WINDOW_CREATE_VULKAN_BOOLEAN = "SDL.window.create.vulkan",
                    SDL_PROP_WINDOW_CREATE_WIDTH_NUMBER = "SDL.window.create.width",
                    SDL_PROP_WINDOW_CREATE_X_NUMBER = "SDL.window.create.x",
                    SDL_PROP_WINDOW_CREATE_Y_NUMBER = "SDL.window.create.y",
                    SDL_PROP_WINDOW_CREATE_COCOA_WINDOW_POINTER = "SDL.window.create.cocoa.window",
                    SDL_PROP_WINDOW_CREATE_COCOA_VIEW_POINTER = "SDL.window.create.cocoa.view",
                    SDL_PROP_WINDOW_CREATE_WINDOWSCENE_POINTER = "SDL.window.create.uikit.windowscene",
                    SDL_PROP_WINDOW_CREATE_WAYLAND_SURFACE_ROLE_CUSTOM_BOOLEAN = "SDL.window.create.wayland.surface_role_custom",
                    SDL_PROP_WINDOW_CREATE_WAYLAND_CREATE_EGL_WINDOW_BOOLEAN = "SDL.window.create.wayland.create_egl_window",
                    SDL_PROP_WINDOW_CREATE_WAYLAND_WL_SURFACE_POINTER = "SDL.window.create.wayland.wl_surface",
                    SDL_PROP_WINDOW_CREATE_WIN32_HWND_POINTER = "SDL.window.create.win32.hwnd",
                    SDL_PROP_WINDOW_CREATE_WIN32_PIXEL_FORMAT_HWND_POINTER = "SDL.window.create.win32.pixel_format_hwnd",
                    SDL_PROP_WINDOW_CREATE_X11_WINDOW_NUMBER = "SDL.window.create.x11.window",
                    SDL_PROP_WINDOW_CREATE_EMSCRIPTEN_CANVAS_ID_STRING = "SDL.window.create.emscripten.canvas_id",
                    SDL_PROP_WINDOW_CREATE_EMSCRIPTEN_KEYBOARD_ELEMENT_STRING = "SDL.window.create.emscripten.keyboard_element";

    public static final String SDL_PROP_WINDOW_SHAPE_POINTER = "SDL.window.shape",
                    SDL_PROP_WINDOW_HDR_ENABLED_BOOLEAN = "SDL.window.HDR_enabled",
                    SDL_PROP_WINDOW_SDR_WHITE_LEVEL_FLOAT = "SDL.window.SDR_white_level",
                    SDL_PROP_WINDOW_HDR_HEADROOM_FLOAT = "SDL.window.HDR_headroom",
                    SDL_PROP_WINDOW_ANDROID_WINDOW_POINTER = "SDL.window.android.window",
                    SDL_PROP_WINDOW_ANDROID_SURFACE_POINTER = "SDL.window.android.surface",
                    SDL_PROP_WINDOW_UIKIT_WINDOW_POINTER = "SDL.window.uikit.window",
                    SDL_PROP_WINDOW_UIKIT_METAL_VIEW_TAG_NUMBER = "SDL.window.uikit.metal_view_tag",
                    SDL_PROP_WINDOW_UIKIT_OPENGL_FRAMEBUFFER_NUMBER = "SDL.window.uikit.opengl.framebuffer",
                    SDL_PROP_WINDOW_UIKIT_OPENGL_RENDERBUFFER_NUMBER = "SDL.window.uikit.opengl.renderbuffer",
                    SDL_PROP_WINDOW_UIKIT_OPENGL_RESOLVE_FRAMEBUFFER_NUMBER = "SDL.window.uikit.opengl.resolve_framebuffer",
                    SDL_PROP_WINDOW_KMSDRM_DEVICE_INDEX_NUMBER = "SDL.window.kmsdrm.dev_index",
                    SDL_PROP_WINDOW_KMSDRM_DRM_FD_NUMBER = "SDL.window.kmsdrm.drm_fd",
                    SDL_PROP_WINDOW_KMSDRM_GBM_DEVICE_POINTER = "SDL.window.kmsdrm.gbm_dev",
                    SDL_PROP_WINDOW_COCOA_WINDOW_POINTER = "SDL.window.cocoa.window",
                    SDL_PROP_WINDOW_COCOA_METAL_VIEW_TAG_NUMBER = "SDL.window.cocoa.metal_view_tag",
                    SDL_PROP_WINDOW_OPENVR_OVERLAY_ID_NUMBER = "SDL.window.openvr.overlay_id",
                    SDL_PROP_WINDOW_VIVANTE_DISPLAY_POINTER = "SDL.window.vivante.display",
                    SDL_PROP_WINDOW_VIVANTE_WINDOW_POINTER = "SDL.window.vivante.window",
                    SDL_PROP_WINDOW_VIVANTE_SURFACE_POINTER = "SDL.window.vivante.surface",
                    SDL_PROP_WINDOW_WIN32_HWND_POINTER = "SDL.window.win32.hwnd",
                    SDL_PROP_WINDOW_WIN32_HDC_POINTER = "SDL.window.win32.hdc",
                    SDL_PROP_WINDOW_WIN32_INSTANCE_POINTER = "SDL.window.win32.instance",
                    SDL_PROP_WINDOW_WAYLAND_DISPLAY_POINTER = "SDL.window.wayland.display",
                    SDL_PROP_WINDOW_WAYLAND_SURFACE_POINTER = "SDL.window.wayland.surface",
                    SDL_PROP_WINDOW_WAYLAND_VIEWPORT_POINTER = "SDL.window.wayland.viewport",
                    SDL_PROP_WINDOW_WAYLAND_EGL_WINDOW_POINTER = "SDL.window.wayland.egl_window",
                    SDL_PROP_WINDOW_WAYLAND_XDG_SURFACE_POINTER = "SDL.window.wayland.xdg_surface",
                    SDL_PROP_WINDOW_WAYLAND_XDG_TOPLEVEL_POINTER = "SDL.window.wayland.xdg_toplevel",
                    SDL_PROP_WINDOW_WAYLAND_XDG_TOPLEVEL_EXPORT_HANDLE_STRING = "SDL.window.wayland.xdg_toplevel_export_handle",
                    SDL_PROP_WINDOW_WAYLAND_XDG_POPUP_POINTER = "SDL.window.wayland.xdg_popup",
                    SDL_PROP_WINDOW_WAYLAND_XDG_POSITIONER_POINTER = "SDL.window.wayland.xdg_positioner",
                    SDL_PROP_WINDOW_X11_DISPLAY_POINTER = "SDL.window.x11.display",
                    SDL_PROP_WINDOW_X11_SCREEN_NUMBER = "SDL.window.x11.screen",
                    SDL_PROP_WINDOW_X11_WINDOW_NUMBER = "SDL.window.x11.window",
                    SDL_PROP_WINDOW_EMSCRIPTEN_CANVAS_ID_STRING = "SDL.window.emscripten.canvas_id",
                    SDL_PROP_WINDOW_EMSCRIPTEN_KEYBOARD_ELEMENT_STRING = "SDL.window.emscripten.keyboard_element";

    public static final int SDL_WINDOW_SURFACE_VSYNC_DISABLED = 0,
                    SDL_WINDOW_SURFACE_VSYNC_ADAPTIVE = -1;

    public static final int SDL_HITTEST_NORMAL = 0,
                    SDL_HITTEST_DRAGGABLE = 1,
                    SDL_HITTEST_RESIZE_TOPLEFT = 2,
                    SDL_HITTEST_RESIZE_TOP = 3,
                    SDL_HITTEST_RESIZE_TOPRIGHT = 4,
                    SDL_HITTEST_RESIZE_RIGHT = 5,
                    SDL_HITTEST_RESIZE_BOTTOMRIGHT = 6,
                    SDL_HITTEST_RESIZE_BOTTOM = 7,
                    SDL_HITTEST_RESIZE_BOTTOMLEFT = 8,
                    SDL_HITTEST_RESIZE_LEFT = 9;

    protected SDLVideo() {
        throw new UnsupportedOperationException();
    }

    public static int SDL_WINDOWPOS_UNDEFINED_DISPLAY(final int X) {
        return SDL_WINDOWPOS_UNDEFINED_MASK | X;
    }

    public static boolean SDL_WINDOWPOS_ISUNDEFINED(final int X) {
        return (X & 0xFFFF0000) == SDL_WINDOWPOS_UNDEFINED_MASK;
    }

    public static int SDL_WINDOWPOS_CENTERED_DISPLAY(final int X) {
        return SDL_WINDOWPOS_CENTERED_MASK | X;
    }

    public static boolean SDL_WINDOWPOS_ISCENTERED(final int X) {
        return (X & 0xFFFF0000) == SDL_WINDOWPOS_CENTERED_MASK;
    }
}
