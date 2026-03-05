package de.hpi.swa.trufflesqueak.io;

import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.EVENT_TYPE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE;
import de.hpi.swa.trufflesqueak.io.SqueakIOConstants.MOUSE_EVENT;
import org.lwjgl.sdl.SDL_MouseButtonEvent;
import org.lwjgl.sdl.SDL_MouseMotionEvent;
import org.lwjgl.sdl.SDL_MouseWheelEvent;
import org.lwjgl.sdl.SDLMouse;

public final class SqueakMouse {
    private final SqueakDisplay display;

    public SqueakMouse(final SqueakDisplay display) {
        this.display = display;
    }

    public void processMouseMotion(final SDL_MouseMotionEvent event) {
        recordMouseEvent(MOUSE_EVENT.MOVE, event.x(), event.y(), 0);
    }

    public void processMouseButtonDown(final SDL_MouseButtonEvent event) {
        recordMouseEvent(MOUSE_EVENT.DOWN, event.x(), event.y(), event.button());
    }

    public void processMouseButtonUp(final SDL_MouseButtonEvent event) {
        recordMouseEvent(MOUSE_EVENT.UP, event.x(), event.y(), event.button());
    }

    public void processMouseWheel(final SDL_MouseWheelEvent event) {
        display.addEvent(EVENT_TYPE.MOUSE_WHEEL, 0L, (long) (event.y() * MOUSE.WHEEL_DELTA_FACTOR), display.buttons >> 3, 0L);
    }

    private void recordMouseEvent(final MOUSE_EVENT type, final float x, final float y, final int sdlButton) {
//        final double scale = display.getDisplayScale();
        final double scale = 1.0d;
        final int scaledX = (int) (x * scale);
        final int scaledY = (int) (y * scale);

        final int currentButtons = display.buttons & MOUSE.ALL;

        final int newButtonState = switch (type) {
            case DOWN -> currentButtons | mapButton(sdlButton);         // Add the new button
            case MOVE -> currentButtons;                                // Keep existing buttons
            case UP -> currentButtons & ~mapButton(sdlButton);          // Remove ONLY the released button
        };

        // Merge the new mouse button state with the existing keyboard modifiers
        display.buttons = newButtonState | (display.buttons & ~MOUSE.ALL);

        display.addEvent(EVENT_TYPE.MOUSE, scaledX, scaledY, display.buttons & MOUSE.ALL, display.buttons >> 3);
    }

    private static int mapButton(final int sdlButton) {
        return switch (sdlButton) {
            case SDLMouse.SDL_BUTTON_LEFT -> MOUSE.RED;
            case SDLMouse.SDL_BUTTON_MIDDLE -> MOUSE.YELLOW;
            case SDLMouse.SDL_BUTTON_RIGHT -> MOUSE.BLUE;
            default -> 0;
        };
    }
}