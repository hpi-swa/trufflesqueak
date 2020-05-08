/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.aot;

import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.constant.CConstant;
import org.graalvm.nativeimage.c.constant.CEnum;
import org.graalvm.nativeimage.c.constant.CEnumConstant;
import org.graalvm.nativeimage.c.constant.CEnumValue;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.struct.AllowNarrowingCast;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CFieldAddress;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CTypeConversion.CCharPointerHolder;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.PointerBase;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

/**
 *
 * SDL2 bindings for SubstrateVM (tested with SDL 2.0.2).
 *
 */
@CContext(SDLCContext.class)
public final class SDL {
    /*
     * SDL.h
     */

    @CConstant("SDL_INIT_VIDEO")
    public static native int initVideo();

    @CFunction("SDL_Init")
    public static native int init(int flags);

    @CFunction("SDL_Quit")
    public static native void quit();

    /*
     * SDL_clipboard.h
     */

    @CFunction("SDL_GetClipboardText")
    public static native CCharPointer getClipboardText();

    @CFunction("SDL_SetClipboardText")
    public static native int setClipboardText(CCharPointer text);

    /*
     * SDL_error.h
     */

    @CFunction("SDL_GetError")
    public static native CCharPointer getError();

    @TruffleBoundary
    public static String getErrorString() {
        return CTypeConversion.toJavaString(SDL.getError());
    }

    /*
     * SDL_events.h
     */

    @CEnum
    public enum EventType {
        //@formatter:off
        @CEnumConstant("SDL_FIRSTEVENT") FIRSTEVENT,
        @CEnumConstant("SDL_QUIT") QUIT,
        @CEnumConstant("SDL_APP_TERMINATING") APP_TERMINATING,
        @CEnumConstant("SDL_APP_LOWMEMORY") APP_LOWMEMORY,
        @CEnumConstant("SDL_APP_WILLENTERBACKGROUND") APP_WILLENTERBACKGROUND,
        @CEnumConstant("SDL_APP_DIDENTERBACKGROUND") APP_DIDENTERBACKGROUND,
        @CEnumConstant("SDL_APP_WILLENTERFOREGROUND") APP_WILLENTERFOREGROUND,
        @CEnumConstant("SDL_APP_DIDENTERFOREGROUND") APP_DIDENTERFOREGROUND,
        @CEnumConstant("SDL_WINDOWEVENT") WINDOWEVENT,
        @CEnumConstant("SDL_SYSWMEVENT") SYSWMEVENT,
        @CEnumConstant("SDL_KEYDOWN") KEYDOWN,
        @CEnumConstant("SDL_KEYUP") KEYUP,
        @CEnumConstant("SDL_TEXTEDITING") TEXTEDITING,
        @CEnumConstant("SDL_TEXTINPUT") TEXTINPUT,
        @CEnumConstant("SDL_MOUSEMOTION") MOUSEMOTION,
        @CEnumConstant("SDL_MOUSEBUTTONDOWN") MOUSEBUTTONDOWN,
        @CEnumConstant("SDL_MOUSEBUTTONUP") MOUSEBUTTONUP,
        @CEnumConstant("SDL_MOUSEWHEEL") MOUSEWHEEL,
        @CEnumConstant("SDL_JOYAXISMOTION") JOYAXISMOTION,
        @CEnumConstant("SDL_JOYBALLMOTION") JOYBALLMOTION,
        @CEnumConstant("SDL_JOYHATMOTION") JOYHATMOTION,
        @CEnumConstant("SDL_JOYBUTTONDOWN") JOYBUTTONDOWN,
        @CEnumConstant("SDL_JOYBUTTONUP") JOYBUTTONUP,
        @CEnumConstant("SDL_JOYDEVICEADDED") JOYDEVICEADDED,
        @CEnumConstant("SDL_JOYDEVICEREMOVED") JOYDEVICEREMOVED,
        @CEnumConstant("SDL_CONTROLLERAXISMOTION") CONTROLLERAXISMOTION,
        @CEnumConstant("SDL_CONTROLLERBUTTONDOWN") CONTROLLERBUTTONDOWN,
        @CEnumConstant("SDL_CONTROLLERBUTTONUP") CONTROLLERBUTTONUP,
        @CEnumConstant("SDL_CONTROLLERDEVICEADDED") CONTROLLERDEVICEADDED,
        @CEnumConstant("SDL_CONTROLLERDEVICEREMOVED") CONTROLLERDEVICEREMOVED,
        @CEnumConstant("SDL_CONTROLLERDEVICEREMAPPED") CONTROLLERDEVICEREMAPPED,
        @CEnumConstant("SDL_FINGERDOWN") FINGERDOWN,
        @CEnumConstant("SDL_FINGERUP") FINGERUP,
        @CEnumConstant("SDL_FINGERMOTION") FINGERMOTION,
        @CEnumConstant("SDL_DOLLARGESTURE") DOLLARGESTURE,
        @CEnumConstant("SDL_DOLLARRECORD") DOLLARRECORD,
        @CEnumConstant("SDL_MULTIGESTURE") MULTIGESTURE,
        @CEnumConstant("SDL_CLIPBOARDUPDATE") CLIPBOARDUPDATE,
        @CEnumConstant("SDL_DROPFILE") DROPFILE,
        @CEnumConstant("SDL_RENDER_TARGETS_RESET") RENDER_TARGETS_RESET,
        @CEnumConstant("SDL_USEREVENT") USEREVENT,
        @CEnumConstant("SDL_LASTEVENT") LASTEVENT;
        //@formatter:on

        @CEnumValue
        public native int getCValue();
    }

    @CConstant("SDL_IGNORE")
    public static native int ignore();

    @CStruct(value = "union SDL_Event")
    public interface Event extends PointerBase {
        @CField
        int type();

        @CField
        @AllowNarrowingCast
        CCharPointer padding();
    }

    @CStruct(value = "SDL_MouseMotionEvent", addStructKeyword = true, isIncomplete = true)
    public interface MouseMotionEvent extends PointerBase {
        @CField
        int x();

        @CField
        int y();
    }

    @CStruct(value = "SDL_MouseWheelEvent", addStructKeyword = true, isIncomplete = true)
    public interface MouseWheelEvent extends PointerBase {
        @CField
        int x();

        @CField
        int y();
    }

    @CStruct(value = "SDL_MouseButtonEvent", addStructKeyword = true, isIncomplete = true)
    public interface MouseButtonEvent extends PointerBase {
        @CField
        int type();

        @CField
        byte button();
    }

    @CStruct(value = "SDL_KeyboardEvent", addStructKeyword = true, isIncomplete = true)
    public interface KeyboardEvent extends PointerBase {
        @CFieldAddress
        Keysym keysym();
    }

    @CStruct(value = "SDL_Keysym", addStructKeyword = true, isIncomplete = true)
    public interface Keysym extends PointerBase {
        @CField
        int sym();
    }

    @CStruct(value = "SDL_TextInputEvent", addStructKeyword = true, isIncomplete = true)
    public interface TextInputEvent extends PointerBase {
        @CFieldAddress
        CCharPointer text();
    }

    @CStruct(value = "SDL_WindowEvent", addStructKeyword = true, isIncomplete = true)
    public interface WindowEvent extends PointerBase {
        @CField
        byte event();

        @CField
        int data1();

        @CField
        int data2();
    }

    @CFunction("SDL_EventState")
    public static native int eventState(int type, int state);

    @CFunction("SDL_PollEvent")
    public static native int pollEvent(Event event);

    /*
     * SDL_hints.h
     */

    // Checkstyle: stop
    public static final String HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK = "SDL_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK";
    public static final String HINT_RENDER_SCALE_QUALITY = "SDL_RENDER_SCALE_QUALITY";
    public static final String HINT_RENDER_VSYNC = "SDL_RENDER_VSYNC";
    public static final String HINT_VIDEO_X11_NET_WM_PING = "SDL_VIDEO_X11_NET_WM_PING";
    // Checkstyle: resume

    @CFunction("SDL_SetHint")
    public static native int setHint(CCharPointer key, CCharPointer value);

    public static int setHint(final String key, final String value) {
        try (CCharPointerHolder keyPointerHolder = CTypeConversion.toCString(key)) {
            try (CCharPointerHolder valuePointerHolder = CTypeConversion.toCString(value)) {
                return SDL.setHint(keyPointerHolder.get(), valuePointerHolder.get());
            }
        }
    }

    /*
     * SDL_keycode.h
     */

    @CConstant("SDLK_DOWN")
    public static native int kDown();

    @CConstant("SDLK_LEFT")
    public static native int kLeft();

    @CConstant("SDLK_RIGHT")
    public static native int kRight();

    @CConstant("SDLK_UP")
    public static native int kUp();

    @CConstant("SDLK_HOME")
    public static native int kHome();

    @CConstant("SDLK_END")
    public static native int kEnd();

    @CConstant("SDLK_INSERT")
    public static native int kInsert();

    @CConstant("SDLK_PAGEUP")
    public static native int kPageup();

    @CConstant("SDLK_PAGEDOWN")
    public static native int kPagedown();

    @CConstant("SDLK_LSHIFT")
    public static native int kLShift();

    @CConstant("SDLK_RSHIFT")
    public static native int kRShift();

    @CConstant("SDLK_LCTRL")
    public static native int kLCtrl();

    @CConstant("SDLK_RCTRL")
    public static native int kRCtrl();

    @CConstant("SDLK_LALT")
    public static native int kLAlt();

    @CConstant("SDLK_RALT")
    public static native int kRAlt();

    @CConstant("SDLK_LGUI")
    public static native int kLGui();

    @CConstant("SDLK_RGUI")
    public static native int kRGui();

    @CConstant("SDLK_DELETE")
    public static native int kDelete();

    @CConstant("SDLK_BACKSPACE")
    public static native int kBackspace();

    @CConstant("SDLK_PAUSE")
    public static native int kPause();

    @CConstant("SDLK_CAPSLOCK")
    public static native int kCapslock();

    @CConstant("SDLK_NUMLOCKCLEAR")
    public static native int kNumlockclear();

    @CConstant("SDLK_SCROLLLOCK")
    public static native int kScrolllock();

    @CConstant("SDLK_PRINTSCREEN")
    public static native int kPrintscreen();

    @CConstant("KMOD_NONE")
    public static native int kmodNone();

    @CConstant("KMOD_LSHIFT")
    public static native int kmodLShift();

    @CConstant("KMOD_RSHIFT")
    public static native int kmodRShift();

    @CConstant("KMOD_LCTRL")
    public static native int kmodLCtrl();

    @CConstant("KMOD_RCTRL")
    public static native int kmodRCtrl();

    @CConstant("KMOD_LALT")
    public static native int kmodLAlt();

    @CConstant("KMOD_RALT")
    public static native int kmodRAlt();

    @CConstant("KMOD_LGUI")
    public static native int kmodLGui();

    @CConstant("KMOD_RGUI")
    public static native int kmodRGui();

    @CConstant("KMOD_CAPS")
    public static native int kmodCaps();

    @CConstant("KMOD_CTRL")
    public static native int kmodCtrl();

    @CConstant("KMOD_SHIFT")
    public static native int kmodShift();

    @CConstant("KMOD_ALT")
    public static native int kmodAlt();

    @CConstant("KMOD_GUI")
    public static native int kmodGui();

    /*
     * SDL_keyboard.h
     */

    @CFunction("SDL_GetModState")
    public static native int getModState();

    /*
     * SDL_mouse.h
     */

    @CConstant("SDL_BUTTON_LEFT")
    public static native int buttonLeft();

    @CConstant("SDL_BUTTON_MIDDLE")
    public static native int buttonMiddle();

    @CConstant("SDL_BUTTON_RIGHT")
    public static native int buttonRight();

    @CConstant("SDL_BUTTON_X1")
    public static native int buttonX1();

    @CConstant("SDL_BUTTON_X2")
    public static native int buttonX2();

    @CConstant("SDL_BUTTON_LMASK")
    public static native int buttonLMask();

    @CConstant("SDL_BUTTON_MMASK")
    public static native int buttonMMask();

    @CConstant("SDL_BUTTON_RMASK")
    public static native int buttonRMask();

    @CConstant("SDL_BUTTON_X1MASK")
    public static native int buttonX1Mask();

    @CConstant("SDL_BUTTON_X2MASK")
    public static native int buttonX2Mask();

    @CStruct(value = "SDL_CURSOR", addStructKeyword = true, isIncomplete = true)
    public interface Cursor extends PointerBase {
        /* Opaque. */
    }

    @CFunction("SDL_CreateCursor")
    public static native Cursor createCursor(CCharPointer data, CCharPointer mask, int w, int h, int hotX, int hotY);

    @CFunction("SDL_SetCursor")
    public static native void setCursor(Cursor cursor);

    @CFunction("SDL_FreeCursor")
    public static native void freeCursor(Cursor cursor);

    /*
     * SDL_pixels.h
     */

    @CEnum
    public enum Pixelformat {
        //@formatter:off
        @CEnumConstant("SDL_PIXELFORMAT_UNKNOWN") UNKNOWN,
        @CEnumConstant("SDL_PIXELFORMAT_INDEX1LSB") INDEX1LSB,
        @CEnumConstant("SDL_PIXELFORMAT_INDEX1MSB") INDEX1MSB,
        @CEnumConstant("SDL_PIXELFORMAT_INDEX4LSB") INDEX4LSB,
        @CEnumConstant("SDL_PIXELFORMAT_INDEX4MSB") INDEX4MSB,
        @CEnumConstant("SDL_PIXELFORMAT_INDEX8") INDEX8,
        @CEnumConstant("SDL_PIXELFORMAT_RGB332") RGB332,
        @CEnumConstant("SDL_PIXELFORMAT_RGB444") RGB444,
        @CEnumConstant("SDL_PIXELFORMAT_RGB555") RGB555,
        @CEnumConstant("SDL_PIXELFORMAT_BGR555") BGR555,
        @CEnumConstant("SDL_PIXELFORMAT_ARGB4444") ARGB4444,
        @CEnumConstant("SDL_PIXELFORMAT_RGBA4444") RGBA4444,
        @CEnumConstant("SDL_PIXELFORMAT_ABGR4444") ABGR4444,
        @CEnumConstant("SDL_PIXELFORMAT_BGRA4444") BGRA4444,
        @CEnumConstant("SDL_PIXELFORMAT_ARGB1555") ARGB1555,
        @CEnumConstant("SDL_PIXELFORMAT_RGBA5551") RGBA5551,
        @CEnumConstant("SDL_PIXELFORMAT_ABGR1555") ABGR1555,
        @CEnumConstant("SDL_PIXELFORMAT_BGRA5551") BGRA5551,
        @CEnumConstant("SDL_PIXELFORMAT_RGB565") RGB565,
        @CEnumConstant("SDL_PIXELFORMAT_BGR565") BGR565,
        @CEnumConstant("SDL_PIXELFORMAT_RGB24") RGB24,
        @CEnumConstant("SDL_PIXELFORMAT_BGR24") BGR24,
        @CEnumConstant("SDL_PIXELFORMAT_RGB888") RGB888,
        @CEnumConstant("SDL_PIXELFORMAT_RGBX8888") RGBX8888,
        @CEnumConstant("SDL_PIXELFORMAT_BGR888") BGR888,
        @CEnumConstant("SDL_PIXELFORMAT_BGRX8888") BGRX8888,
        @CEnumConstant("SDL_PIXELFORMAT_ARGB8888") ARGB8888,
        @CEnumConstant("SDL_PIXELFORMAT_RGBA8888") RGBA8888,
        @CEnumConstant("SDL_PIXELFORMAT_ABGR8888") ABGR8888,
        @CEnumConstant("SDL_PIXELFORMAT_BGRA8888") BGRA8888,
        @CEnumConstant("SDL_PIXELFORMAT_ARGB2101010") ARGB2101010,
        @CEnumConstant("SDL_PIXELFORMAT_YV12") YV12,
        @CEnumConstant("SDL_PIXELFORMAT_IYUV") IYUV,
        @CEnumConstant("SDL_PIXELFORMAT_YUY2") YUY2,
        @CEnumConstant("SDL_PIXELFORMAT_UYVY") UYVY,
        @CEnumConstant("SDL_PIXELFORMAT_YVYU") YVYU;
        //@formatter:on

        @CEnumValue
        public native int getCValue();
    }

    /*
     * SDL_rect.h
     */

    @CStruct(value = "SDL_Rect", addStructKeyword = true)
    public interface Rect extends PointerBase {
        @CField
        int getx();

        @CField
        void setx(int value);

        @CField
        int gety();

        @CField
        void sety(int value);

        @CField
        int getw();

        @CField
        void setw(int value);

        @CField
        int geth();

        @CField
        void seth(int value);
    }

    @CFunction("SDL_UnionRect")
    public static native void unionRect(Rect a, Rect b, Rect result);

    /*
     * SDL_renderer.h
     */

    @CConstant("SDL_RENDERER_SOFTWARE")
    public static native int rendererSoftware();

    @CConstant("SDL_RENDERER_ACCELERATED")
    public static native int rendererAccelerated();

    @CConstant("SDL_RENDERER_PRESENTVSYNC")
    public static native int rendererPresentvsync();

    @CConstant("SDL_RENDERER_TARGETTEXTURE")
    public static native int rendererTargettexture();

    @CConstant("SDL_TEXTUREACCESS_STATIC")
    public static native int textureaccessStatic();

    @CConstant("SDL_TEXTUREACCESS_STREAMING")
    public static native int textureaccessStreaming();

    @CConstant("SDL_TEXTUREACCESS_TARGET")
    public static native int textureaccessTarget();

    @CStruct(value = "SDL_RENDERER", addStructKeyword = true, isIncomplete = true)
    public interface Renderer extends PointerBase {
        /* Opaque. */
    }

    @CStruct(value = "SDL_TEXTURE", addStructKeyword = true, isIncomplete = true)
    public interface Texture extends PointerBase {
        /* Opaque. */
    }

    @CFunction("SDL_CreateRenderer")
    public static native Renderer createRenderer(Window window, int index, int flags);

    @CFunction("SDL_CreateTexture")
    public static native Texture createTexture(Renderer renderer, int format, int access, int w, int h);

    @CFunction("SDL_DestroyTexture")
    public static native void destroyTexture(Texture texture);

    @CFunction("SDL_LockTexture")
    public static native int lockTexture(Texture texture, Rect rect, WordPointer pixels, CIntPointer pitch);

    @CFunction("SDL_RenderClear")
    public static native int renderClear(Renderer renderer);

    @CFunction("SDL_RenderCopy")
    public static native int renderCopy(PointerBase renderer, PointerBase texture, PointerBase srcrect, PointerBase dstrect);

    @CFunction("SDL_RenderPresent")
    public static native void renderPresent(Renderer renderer);

    @CFunction("SDL_SetRenderDrawColor")
    public static native int setRenderDrawColor(Renderer renderer, int r, int g, int b, int a);

    @CFunction("SDL_UnlockTexture")
    public static native void unlockTexture(Texture texture);

    @CFunction("SDL_UpdateTexture")
    public static native int updateTexture(Texture texture, Rect rect, VoidPointer pixels, int pitch);

    /*
     * SDL_video.h
     */

    @CEnum
    public enum WindowFlags {
        //@formatter:off
        @CEnumConstant("SDL_WINDOW_FULLSCREEN") FULLSCREEN,
        @CEnumConstant("SDL_WINDOW_OPENGL") OPENGL,
        @CEnumConstant("SDL_WINDOW_SHOWN") SHOWN,
        @CEnumConstant("SDL_WINDOW_HIDDEN") HIDDEN,
        @CEnumConstant("SDL_WINDOW_BORDERLESS") BORDERLESS,
        @CEnumConstant("SDL_WINDOW_RESIZABLE") RESIZABLE,
        @CEnumConstant("SDL_WINDOW_MINIMIZED") MINIMIZED,
        @CEnumConstant("SDL_WINDOW_MAXIMIZED") MAXIMIZED,
        @CEnumConstant("SDL_WINDOW_INPUT_GRABBED") INPUT_GRABBED,
        @CEnumConstant("SDL_WINDOW_INPUT_FOCUS") INPUT_FOCUS,
        @CEnumConstant("SDL_WINDOW_MOUSE_FOCUS") MOUSE_FOCUS,
        @CEnumConstant("SDL_WINDOW_FULLSCREEN_DESKTOP") FULLSCREEN_DESKTOP,
        @CEnumConstant("SDL_WINDOW_FOREIGN") FOREIGN,
        @CEnumConstant("SDL_WINDOW_ALLOW_HIGHDPI") ALLOW_HIGHDPI;
        //@formatter:on

        @CEnumValue
        public native int getCValue();
    }

    @CEnum
    public enum WindowEventID {
        //@formatter:off
        @CEnumConstant("SDL_WINDOWEVENT_NONE") NONE,
        @CEnumConstant("SDL_WINDOWEVENT_SHOWN") SHOWN,
        @CEnumConstant("SDL_WINDOWEVENT_HIDDEN") HIDDEN,
        @CEnumConstant("SDL_WINDOWEVENT_EXPOSED") EXPOSED,
        @CEnumConstant("SDL_WINDOWEVENT_MOVED") MOVED,
        @CEnumConstant("SDL_WINDOWEVENT_RESIZED") RESIZED,
        @CEnumConstant("SDL_WINDOWEVENT_SIZE_CHANGED") SIZE_CHANGED,
        @CEnumConstant("SDL_WINDOWEVENT_MINIMIZED") MINIMIZED,
        @CEnumConstant("SDL_WINDOWEVENT_MAXIMIZED") MAXIMIZED,
        @CEnumConstant("SDL_WINDOWEVENT_RESTORED") RESTORED,
        @CEnumConstant("SDL_WINDOWEVENT_ENTER") ENTER,
        @CEnumConstant("SDL_WINDOWEVENT_LEAVE") LEAVE,
        @CEnumConstant("SDL_WINDOWEVENT_FOCUS_GAINED") FOCUS_GAINED,
        @CEnumConstant("SDL_WINDOWEVENT_FOCUS_LOST") FOCUS_LOST,
        @CEnumConstant("SDL_WINDOWEVENT_CLOSE") CLOSE;
        //@formatter:on

        @CEnumValue
        public native int getCValue();
    }

    @CStruct(value = "SDL_WINDOW", addStructKeyword = true, isIncomplete = true)
    public interface Window extends PointerBase {
        /* Opaque. */
    }

    @CConstant("SDL_WINDOWPOS_UNDEFINED")
    public static native int windowposUndefined();

    @CFunction("SDL_CreateWindow")
    public static native Window createWindow(CCharPointer data, int x, int y, int w, int h, int windowResizable);

    @CFunction("SDL_DestroyWindow")
    public static native void destroyWindow(Window window);

    @CFunction("SDL_GetWindowSize")
    public static native void getWindowSize(Window window, CIntPointer w, CIntPointer h);

    @CFunction("SDL_GL_SetSwapInterval")
    public static native int glSetSwapInterval(int interval);

    @CFunction("SDL_SetWindowFullscreen")
    public static native int setWindowFullscreen(Window window, int flags);

    @CFunction("SDL_SetWindowTitle")
    public static native void setWindowTitle(Window window, CCharPointer title);
}
