/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.aot;

import java.util.ArrayDeque;
import java.util.Arrays;

import org.graalvm.nativeimage.PinnedObject;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CIntPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.nativeimage.c.type.CTypeConversion.CCharPointerHolder;
import org.graalvm.nativeimage.c.type.VoidPointer;
import org.graalvm.nativeimage.c.type.WordPointer;
import org.graalvm.word.WordFactory;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.aot.SDL.Cursor;
import de.hpi.swa.graal.squeak.aot.SDL.Event;
import de.hpi.swa.graal.squeak.aot.SDL.KeyboardEvent;
import de.hpi.swa.graal.squeak.aot.SDL.MouseButtonEvent;
import de.hpi.swa.graal.squeak.aot.SDL.MouseMotionEvent;
import de.hpi.swa.graal.squeak.aot.SDL.MouseWheelEvent;
import de.hpi.swa.graal.squeak.aot.SDL.Rect;
import de.hpi.swa.graal.squeak.aot.SDL.Renderer;
import de.hpi.swa.graal.squeak.aot.SDL.TextInputEvent;
import de.hpi.swa.graal.squeak.aot.SDL.Texture;
import de.hpi.swa.graal.squeak.aot.SDL.Window;
import de.hpi.swa.graal.squeak.aot.SDL.WindowEvent;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.io.SqueakDisplayInterface;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEY;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.KEYBOARD_EVENT;
import de.hpi.swa.graal.squeak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.util.OSDetector;

final class Target_de_hpi_swa_graal_squeak_io_SqueakDisplay implements SqueakDisplayInterface {
    private static final String DEFAULT_WINDOW_TITLE = "GraalSqueak + SubstrateVM + SDL2";

    private final SqueakImageContext image;
    private final Rect flipRect = UnmanagedMemory.malloc(SizeOf.unsigned(SDL.Rect.class));
    private final Rect renderRect = UnmanagedMemory.malloc(SizeOf.unsigned(SDL.Rect.class));
    private final Rect nullRect = WordFactory.nullPointer();
    private final WordPointer pixelVoidPP = UnmanagedMemory.malloc(SizeOf.unsigned(WordPointer.class));
    private final CIntPointer pitchIntP = UnmanagedMemory.malloc(SizeOf.unsigned(CIntPointer.class));
    private final ArrayDeque<long[]> deferredEvents = new ArrayDeque<>();
    private final Event event = UnmanagedMemory.malloc(SizeOf.unsigned(Event.class));

    private Window window = WordFactory.nullPointer();
    private Renderer renderer = WordFactory.nullPointer();
    private Texture texture = WordFactory.nullPointer();
    private Cursor cursor = WordFactory.nullPointer();
    private NativeObject bitmap;
    @CompilationFinal private int inputSemaphoreIndex;
    private boolean deferUpdates = false;
    private boolean textureDirty = false;
    private int width;
    private int height;
    private int bpp = 4; // TODO: for 32bit only!

    private int lastMouseXPos;
    private int lastMouseYPos;
    private int button = 0;
    private int key = 0;
    private boolean isKeyDown = false;

    Target_de_hpi_swa_graal_squeak_io_SqueakDisplay(final SqueakImageContext image) {
        this.image = image;
        sdlAssert(SDL.init(SDL.initVideo()) == 0);

        // Do not wait for vsync.
        SDL.setHint(SDL.HINT_RENDER_VSYNC, "0");
        // Nearest pixel sampling.
        SDL.setHint(SDL.HINT_RENDER_SCALE_QUALITY, "0");
        // Disable WM_PING, so the WM does not think it is hung.
        SDL.setHint(SDL.HINT_VIDEO_X11_NET_WM_PING, "0");
        // Ctrl-Click on macOS is right click.
        SDL.setHint(SDL.HINT_MAC_CTRL_CLICK_EMULATE_RIGHT_CLICK, "1");

        // Disable unneeded events to avoid issues (e.g. double clicks).
        SDL.eventState(SDL.EventType.TEXTEDITING.getCValue(), SDL.ignore());
        SDL.eventState(SDL.EventType.FINGERDOWN.getCValue(), SDL.ignore());
        SDL.eventState(SDL.EventType.FINGERUP.getCValue(), SDL.ignore());
        SDL.eventState(SDL.EventType.FINGERMOTION.getCValue(), SDL.ignore());

        // Try to allow late tearing (pushes frames faster).
        if (SDL.glSetSwapInterval(-1) < 0) {
            SDL.glSetSwapInterval(0); // At least try to disable vsync.
        }
    }

    @Override
    public void showDisplayBitsLeftTopRightBottom(final PointersObject destForm, final int left, final int top, final int right, final int bottom) {
        if (left < right && top < bottom && !deferUpdates && destForm.isDisplay()) {
            paintImmediately(left, right, top, bottom);
        }
    }

    @Override
    public void showDisplayRect(final int left, final int right, final int top, final int bottom) {
        assert left < right && top < bottom;
        paintImmediately(left, right, top, bottom);
    }

    private void paintImmediately(final int left, final int right, final int top, final int bottom) {
        copyPixels(left + top * width, right + bottom * width);
        recordDamage(left, top, right - left, bottom - top);
        textureDirty = true;
        render(true);
    }

    @Override
    public void setFullscreen(final boolean enable) {
        if (enable) {
            SDL.setWindowFullscreen(window, SDL.WindowFlags.FULLSCREEN_DESKTOP.getCValue());
        } else {
            SDL.setWindowFullscreen(window, 0);
        }
    }

    @Override
    public void close() {
        if (window.isNonNull()) {
            SDL.destroyWindow(window);
        }
        SDL.quit();
    }

    @Override
    public void open(final PointersObject sqDisplay) {
        bitmap = (NativeObject) sqDisplay.instVarAt0Slow(FORM.BITS);
        if (!bitmap.isIntType()) {
            throw SqueakException.create("Display bitmap expected to be a words object");
        }

        final int depth = (int) (long) sqDisplay.instVarAt0Slow(FORM.DEPTH);
        if (depth != 32) {
            throw SqueakException.create("Expected 32bit display");
        }
        if (window.isNull()) {
            width = (int) (long) sqDisplay.instVarAt0Slow(FORM.WIDTH);
            height = (int) (long) sqDisplay.instVarAt0Slow(FORM.HEIGHT);
            try (CCharPointerHolder title = CTypeConversion.toCString(DEFAULT_WINDOW_TITLE)) {
                window = SDL.createWindow(
                                title.get(),
                                SDL.windowposUndefined(),
                                SDL.windowposUndefined(),
                                width,
                                height,
                                SDL.WindowFlags.RESIZABLE.getCValue());
                sdlAssert(window.isNonNull());
            }
            renderer = SDL.createRenderer(window, -1, SDL.rendererSoftware());
            sdlAssert(renderer.isNonNull());
            texture = SDL.createTexture(renderer, SDL.Pixelformat.ARGB8888.getCValue(), SDL.textureaccessStreaming(), width, height);
            sdlAssert(texture.isNonNull());
            fullDamage();
            getNextEvent(); // Poll and drop fix events for faster window initialization.
        } else {
            resizeTo((int) (long) sqDisplay.instVarAt0Slow(FORM.WIDTH), (int) (long) sqDisplay.instVarAt0Slow(FORM.HEIGHT));
        }
    }

    @Override
    @TruffleBoundary
    public boolean isVisible() {
        return window.isNonNull();
    }

    @Override
    public void setCursor(final int[] cursorWords, final int[] mask, final int width, final int height, final int depth) {
        if (window.isNull()) {
            return;
        }
        if (cursor.isNonNull()) {
            SDL.freeCursor(cursor);
        }
        final int nCursorBytes = cursorWords.length * 2;
        final byte[] cursorBytes = cursorWordsToBytes(nCursorBytes, cursorWords);
        final byte[] maskBytes = cursorWordsToBytes(nCursorBytes, mask);
        try (PinnedObject pinnedCursorBytes = PinnedObject.create(cursorBytes)) {
            try (PinnedObject pinnedMaskBytes = PinnedObject.create(maskBytes)) {
                cursor = SDL.createCursor(pinnedCursorBytes.addressOfArrayElement(0), pinnedMaskBytes.addressOfArrayElement(0), 16, 16, 0, 0);
            }
        }
        if (!sdlError(cursor.isNonNull())) {
            return;
        }
        SDL.setCursor(cursor);
    }

    @Override
    public long[] getNextEvent() {
        return deferredEvents.pollFirst();
    }

    @Override
    @TruffleBoundary
    public void pollEvents() {
        while (SDL.pollEvent(event) != 0) {
            final long time = getEventTime();
            final int eventType = event.type();
            if (eventType == SDL.EventType.MOUSEBUTTONDOWN.getCValue() || eventType == SDL.EventType.MOUSEBUTTONUP.getCValue()) {
                handleMouseButton();
                queueEvent(getNextMouseEvent(time));
            } else if (eventType == SDL.EventType.MOUSEMOTION.getCValue()) {
                handleMouseMove();
                queueEvent(getNextMouseEvent(time));
            } else if (eventType == SDL.EventType.MOUSEWHEEL.getCValue()) {
                queueEvent(getNextMouseWheelEvent(time));
            } else if (eventType == SDL.EventType.KEYDOWN.getCValue()) {
                isKeyDown = true;
                handleKeyboardEvent();
                long[] later = null;
                // No TEXTINPUT event for this key will follow, but Squeak needs a KeyStroke anyway.
                if (!isModifierKey(key) && (OSDetector.SINGLETON.isLinux() && isControlKey(key) ||
                                !OSDetector.SINGLETON.isLinux() && (isControlKey(key) || (SDL.getModState() & ~SDL.kmodShift()) != 0))) {
                    later = getNextKeyEvent(KEYBOARD_EVENT.CHAR, time);
                }
                fixKeyCodeCase();
                queueEvent(getNextKeyEvent(KEYBOARD_EVENT.DOWN, time));
                if (later != null) {
                    queueEvent(later);
                }
            } else if (eventType == SDL.EventType.TEXTINPUT.getCValue()) {
                handleTextInputEvent();
                queueEvent(getNextKeyEvent(KEYBOARD_EVENT.CHAR, time));
            } else if (eventType == SDL.EventType.KEYUP.getCValue()) {
                isKeyDown = false;
                handleKeyboardEvent();
                fixKeyCodeCase();
                queueEvent(getNextKeyEvent(KEYBOARD_EVENT.UP, time));
            } else if (eventType == SDL.EventType.WINDOWEVENT.getCValue()) {
                handleWindowEvent();
            } else if (eventType == SDL.EventType.RENDER_TARGETS_RESET.getCValue()) {
                /* || eventType == SDL.EventType.RENDER_DEVICE_RESET.getCValue() */
                fullDamage();
                render(true);
            } else if (eventType == SDL.EventType.QUIT.getCValue()) {
                throw new SqueakQuit(0);
            }
        }
    }

    @Override
    public void setDeferUpdates(final boolean flag) {
        deferUpdates = flag;
    }

    @Override
    public boolean getDeferUpdates() {
        return deferUpdates;
    }

    @Override
    public void resizeTo(final int newWidth, final int newHeight) {
        if (width == newWidth && height == newHeight) {
            return;
        }
        width = newWidth;
        height = newHeight;
        if (texture.isNonNull()) {
            SDL.destroyTexture(texture);
        }
        texture = SDL.createTexture(renderer, SDL.Pixelformat.ARGB8888.getCValue(), SDL.textureaccessStreaming(), width, height);
        sdlAssert(texture.isNonNull());
        lock();
        fullDamage();
    }

    @Override
    public DisplayPoint getWindowSize() {
        // TODO Auto-generated method stub
        return image.flags.getLastWindowSize();
    }

    @Override
    @TruffleBoundary
    public void setWindowTitle(final String title) {
        try (CCharPointerHolder titleC = CTypeConversion.toCString(title)) {
            SDL.setWindowTitle(window, titleC.get());
        }
    }

    @Override
    public void setInputSemaphoreIndex(final int interruptSemaphoreIndex) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        inputSemaphoreIndex = interruptSemaphoreIndex;
    }

    @Override
    @TruffleBoundary
    public String getClipboardData() {
        return CTypeConversion.toJavaString(SDL.getClipboardText());
    }

    @Override
    @TruffleBoundary
    public void setClipboardData(final String text) {
        try (CCharPointerHolder textPointer = CTypeConversion.toCString(text)) {
            SDL.setClipboardText(textPointer.get());
        }
    }

    @Override
    public void beep() {
        image.printToStdOut((char) 7);
    }

    private static void sdlAssert(final boolean successful) {
        if (!successful) {
            throw SqueakException.create(SDL.getErrorString());
        }
    }

    private boolean sdlError(final boolean successful) {
        if (!successful) {
            image.printToStdErr(SDL.getErrorString());
        }
        return successful;
    }

    private void copyPixels(final int start, final int stop) {
        final int offset = start * bpp;
        assert offset >= 0;
        final int remainingSize = width * height * bpp - offset;
        if (remainingSize <= 0 || start >= stop) {
            image.printToStdOut("remainingSize <= 0", remainingSize <= 0, "start >= stop", start >= stop);
            return;
        }
        final int[] pixels = bitmap.getIntStorage();
        try (PinnedObject pinnedPixels = PinnedObject.create(pixels)) {
            final VoidPointer surfaceBufferPointer = pinnedPixels.addressOfArrayElement(0);
            SDL.updateTexture(texture, nullRect, surfaceBufferPointer, width * bpp);
        }
    }

    private void render(final boolean forced) {
        if (!forced && (deferUpdates || !textureDirty)) {
            return;
        }
        textureDirty = false;
        unlock();
        if (!sdlError(SDL.renderCopy(renderer, texture, renderRect, renderRect) == 0)) {
            return;
        }
        SDL.renderPresent(renderer);
        resetDamage();
        lock();
    }

    private void unlock() {
        SDL.unlockTexture(texture);
    }

    private void lock() {
        sdlError(SDL.lockTexture(texture, nullRect, pixelVoidPP, pitchIntP) == 0);
    }

    private void recordDamage(final int x, final int y, final int w, final int h) {
        flipRect.setx(x);
        flipRect.sety(y);
        flipRect.setw(Math.min(w + 1, width - x));
        flipRect.seth(Math.min(h + 1, height - y));
        SDL.unionRect(flipRect, renderRect, renderRect);
    }

    private void resetDamage() {
        renderRect.setx(0);
        renderRect.sety(0);
        renderRect.setw(0);
        renderRect.seth(0);
    }

    private void fullDamage() {
        renderRect.setx(0);
        renderRect.sety(0);
        renderRect.setw(width);
        renderRect.seth(height);
    }

    private static byte[] cursorWordsToBytes(final int nBytes, final int[] words) {
        final byte[] bytes = new byte[nBytes];
        if (words != null) {
            for (int i = 0; i < words.length; i++) {
                final int word = words[i];
                bytes[i * 2] = (byte) (word >> 24);
                bytes[i * 2 + 1] = (byte) (word >> 16);
            }
        } else {
            Arrays.fill(bytes, (byte) 0);
        }
        return bytes;
    }

    private static boolean isControlKey(final int key) {
        return key < 32 || key == KEY.DELETE || key == KEY.NUMLOCK || key == KEY.SCROLLLOCK;
    }

    private static boolean isModifierKey(final int key) {
        return key == KEY.COMMAND || key == KEY.CTRL || key == KEY.SHIFT;
    }

    private void handleWindowEvent() {
        final WindowEvent windowEvent = (WindowEvent) event;
        final byte eventID = windowEvent.event();
        if (eventID == SDL.WindowEventID.RESIZED.ordinal() || eventID == SDL.WindowEventID.RESIZED.ordinal() || eventID == SDL.WindowEventID.EXPOSED.ordinal()) {
            final int newWidth = windowEvent.data1();
            final int newHeight = windowEvent.data2();
            if (newWidth != width || newHeight != height) {
                // TODO: resizeTo(newWidth, newHeight);
            }
            fullDamage();
            render(true);
        }
    }

    @TruffleBoundary
    private void handleTextInputEvent() {
        final TextInputEvent textInputEvent = (TextInputEvent) event;
        key = CTypeConversion.toJavaString(textInputEvent.text()).charAt(0);
    }

    private long[] getNextKeyEvent(final long event_type, final long time) {
        return new long[]{EVENT_TYPE.KEYBOARD, time, key, event_type, getModifierMask(0), key, 0, 0};
    }

    private void fixKeyCodeCase() {
        if (key <= 255) {
            key = Character.toUpperCase(key);
        }
    }

    private void handleKeyboardEvent() {
        final KeyboardEvent keyboardEvent = (KeyboardEvent) event;
        final int sym = keyboardEvent.keysym().sym();
        key = 0;
        if (sym == SDL.kDown()) {
            key = KEY.DOWN;
        } else if (sym == SDL.kLeft()) {
            key = KEY.LEFT;
        } else if (sym == SDL.kRight()) {
            key = KEY.RIGHT;
        } else if (sym == SDL.kUp()) {
            key = KEY.UP;
        } else if (sym == SDL.kHome()) {
            key = KEY.HOME;
        } else if (sym == SDL.kEnd()) {
            key = KEY.END;
        } else if (sym == SDL.kInsert()) {
            key = KEY.INSERT;
        } else if (sym == SDL.kPageup()) {
            key = KEY.PAGEUP;
        } else if (sym == SDL.kPagedown()) {
            key = KEY.PAGEDOWN;
        } else if (sym == SDL.kLShift() || sym == SDL.kRShift()) {
            key = KEY.SHIFT;
        } else if (sym == SDL.kLCtrl() || sym == SDL.kRCtrl()) {
            key = KEY.CTRL;
        } else if (sym == SDL.kLAlt() || sym == SDL.kRAlt()) {
            key = KEY.COMMAND;
        } else if (OSDetector.SINGLETON.isMacOS() && (sym == SDL.kLGui() || sym == SDL.kRGui())) {
            key = KEY.COMMAND;
        } else if (sym == SDL.kDelete()) {
            key = KEY.DELETE;
        } else if (sym == SDL.kBackspace()) {
            key = KEY.BACKSPACE;
        } else if (sym == SDL.kPause()) {
            key = KEY.BREAK;
        } else if (sym == SDL.kCapslock()) {
            key = KEY.CAPSLOCK;
        } else if (sym == SDL.kNumlockclear()) {
            key = KEY.NUMLOCK;
        } else if (sym == SDL.kScrolllock()) {
            key = KEY.SCROLLLOCK;
        } else if (sym == SDL.kPrintscreen()) {
            key = KEY.PRINT;
        } else {
            key = sym;
        }
    }

    private long[] getNextMouseWheelEvent(final long time) {
        final MouseWheelEvent mouseWheelEvent = (MouseWheelEvent) event;
        final long mods = getModifierMask(3);
        final long btn = getMouseEventButtons(mods);
        return new long[]{EVENT_TYPE.MOUSE_WHEEL, time, mouseWheelEvent.x() * 120, mouseWheelEvent.y() * 120, btn, mods, 0L, 0L};
    }

    private void handleMouseMove() {
        final MouseMotionEvent mouseMotionEvent = (MouseMotionEvent) event;
        lastMouseXPos = mouseMotionEvent.x();
        lastMouseYPos = mouseMotionEvent.y();
    }

    private long[] getNextMouseEvent(final long time) {
        final long mods = getModifierMask(3);
        final long btn = getMouseEventButtons(mods);
        return new long[]{EVENT_TYPE.MOUSE, time, lastMouseXPos, lastMouseYPos, btn, mods, 0L, 0L};
    }

    private long getMouseEventButtons(final long mods) {
        long btn = button;
        if (btn == MOUSE.RED) {
            if ((mods & KEY.CTRL_BIT) != 0) {
                btn = MOUSE.BLUE;
            } else if ((mods & (KEY.COMMAND_BIT | KEY.OPTION_BIT)) != 0) {
                btn = MOUSE.YELLOW;
            }
        }
        return btn;
    }

    private static long getModifierMask(final int shift) {
        final int mod = SDL.getModState();
        int modifier = 0;
        if ((mod & SDL.kmodCtrl()) != 0) {
            modifier |= KEY.CTRL_BIT;
        }
        if ((mod & SDL.kmodShift()) != 0) {
            modifier |= KEY.SHIFT_BIT;
        }
        if ((mod & SDL.kmodCaps()) != 0) {
            modifier |= KEY.SHIFT_BIT;
        }
        if ((mod & SDL.kmodAlt()) != 0) {
            if (OSDetector.SINGLETON.isMacOS()) {
                modifier |= KEY.COMMAND_BIT;
            } else {
                modifier |= KEY.OPTION_BIT;
            }
        }
        if ((mod & SDL.kmodAlt()) != 0) {
            modifier |= KEY.COMMAND_BIT;
        }
        return modifier << shift;
    }

    private void handleMouseButton() {
        final MouseButtonEvent mouseButtonEvent = (MouseButtonEvent) event;
        int btn = mouseButtonEvent.button();
        if (btn == SDL.buttonRight()) {
            btn = MOUSE.BLUE;
        } else if (btn == SDL.buttonMiddle()) {
            btn = MOUSE.YELLOW;
        } else if (btn == SDL.buttonLeft()) {
            btn = isKeyDown && key == KEY.COMMAND ? MOUSE.YELLOW : MOUSE.RED;
        }
        if (event.type() == SDL.EventType.MOUSEBUTTONDOWN.getCValue()) {
            button |= btn;
        } else {
            button &= ~btn;
        }
    }

    private long getEventTime() {
        return System.currentTimeMillis() - image.startUpMillis;
    }

    private void queueEvent(final long[] eventData) {
        deferredEvents.add(eventData);
        if (inputSemaphoreIndex > 0) {
            image.interrupt.signalSemaphoreWithIndex(inputSemaphoreIndex);
        }
    }
}

public final class SqueakDisplaySubstitutions {
}
