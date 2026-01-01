/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.io;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakIOConstants {

    public static final int CURSOR_WIDTH = 16;
    public static final int CURSOR_HEIGHT = 16;

    public static final int EVENT_SIZE = 8;

    /*
     * All zeros. Since it does not escape EventSensor>>#fetchMoreEvents, there is no need to
     * allocate individual `long[]` arrays to signal no event (reduces memory pressure).
     */
    @CompilationFinal(dimensions = 1) public static final long[] NONE_EVENT = new long[EVENT_SIZE];

    public enum MOUSE_EVENT {
        DOWN,
        MOVE,
        UP
    }

    public static final class DRAG {
        public static final long ENTER = 1;
        public static final long MOVE = 2;
        public static final long LEAVE = 3;
        public static final long DROP = 4;
    }

    public static final class KEYBOARD_EVENT {
        public static final long CHAR = 0;
        public static final long DOWN = 1;
        public static final long UP = 2;
    }

    public static final class KEYBOARD {
        public static final int SHIFT = 8;
        public static final int CTRL = 16;
        public static final int ALT = 32;
        public static final int CMD = 64;
        public static final int ALL = SHIFT + CTRL + ALT + CMD;
    }

    public static final class KEY {
        public static final int DOWN = 31;
        public static final int LEFT = 28;
        public static final int RIGHT = 29;
        public static final int UP = 30;
        public static final int HOME = 1;
        public static final int END = 4;
        public static final int INSERT = 5;
        public static final int BACKSPACE = 8;
        public static final int PAGEUP = 11;
        public static final int PAGEDOWN = 12;
        public static final int RETURN = 13;
        public static final int SHIFT = 16;
        public static final int CTRL = 17;
        public static final int COMMAND = 18;
        public static final int BREAK = 19;
        public static final int CAPSLOCK = 20;
        public static final int ESCAPE = 27;
        public static final int PRINT = 44;
        public static final int DELETE = 127;
        public static final int NUMLOCK = 184;
        public static final int SCROLLLOCK = 212;

        public static final int SHIFT_BIT = 1;
        public static final int CTRL_BIT = 2;
        public static final int OPTION_BIT = 4;
        public static final int COMMAND_BIT = 8;
    }

    public static final class MOUSE {
        public static final int BLUE = 1;
        public static final int YELLOW = 2;
        public static final int RED = 4;
        public static final int ALL = BLUE + YELLOW + RED;
        /* See HandMorph>>#generateMouseWheelEvent:direction: */
        public static final long WHEEL_DELTA_FACTOR = -120;
    }

    public static final class EVENT_TYPE {
        public static final long NONE = 0;
        public static final long MOUSE = 1;
        public static final long KEYBOARD = 2;
        public static final long DRAG_DROP_FILES = 3;
        public static final long MENU = 4;
        public static final long WINDOW = 5;
        public static final long COMPLEX = 6;
        public static final long MOUSE_WHEEL = 7;
    }

    public static final class WINDOW {
        public static final long METRIC_CHANGE = 1;
        public static final long CLOSE = 2;
        public static final long ICONISE = 3;
        public static final long ACTIVATED = 4;
        public static final long PAINT = 5;
        public static final long CHANGED_SCREEN = 6;
        public static final long DEACTIVATED = 7;
    }

    private SqueakIOConstants() {
    }
}
