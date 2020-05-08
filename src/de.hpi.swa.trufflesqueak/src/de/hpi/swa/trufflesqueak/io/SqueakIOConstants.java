/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.io;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakIOConstants {

    public static final int CURSOR_WIDTH = 16;
    public static final int CURSOR_HEIGHT = 16;

    @CompilationFinal(dimensions = 1) private static final int[] PIXEL_LOOKUP_1BIT = {0xffffffff, 0xff000000};

    @CompilationFinal(dimensions = 1) private static final int[] PIXEL_LOOKUP_2BIT = {0xff000000, 0xff848484, 0xffc6c6c6, 0xffffffff};

    @CompilationFinal(dimensions = 1) private static final int[] PIXEL_LOOKUP_4BIT = {
                    0xff000000, 0xff000084, 0xff008400, 0xff008484,
                    0xff840000, 0xff840084, 0xff848400, 0xff848484,
                    0xffc6c6c6, 0xff0000ff, 0xff00ff00, 0xff00ffff,
                    0xffff0000, 0xffff00ff, 0xffffff00, 0xffffffff
    };

    @CompilationFinal(dimensions = 1) private static final int[] PIXEL_LOOKUP_8BIT = {
                    0xffffffff, 0xff000000, 0xffffffff, 0xff7f7f7f, 0xffff0000, 0xff00ff00,
                    0xff0000ff, 0xff00ffff, 0xffffff00, 0xffff00ff, 0xff1f1f1f, 0xff3f3f3f,
                    0xff5f5f5f, 0xff9f9f9f, 0xffbfbfbf, 0xffdfdfdf, 0xff070707, 0xff0f0f0f,
                    0xff171717, 0xff272727, 0xff2f2f2f, 0xff373737, 0xff474747, 0xff4f4f4f,
                    0xff575757, 0xff676767, 0xff6f6f6f, 0xff777777, 0xff878787, 0xff8f8f8f,
                    0xff979797, 0xffa7a7a7, 0xffafafaf, 0xffb7b7b7, 0xffc7c7c7, 0xffcfcfcf,
                    0xffd7d7d7, 0xffe7e7e7, 0xffefefef, 0xfff7f7f7, 0xff000000, 0xff003200,
                    0xff006500, 0xff009800, 0xff00cb00, 0xff00ff00, 0xff000032, 0xff003232,
                    0xff006532, 0xff009832, 0xff00cb32, 0xff00ff32, 0xff000065, 0xff003265,
                    0xff006565, 0xff009865, 0xff00cb65, 0xff00ff65, 0xff000098, 0xff003298,
                    0xff006598, 0xff009898, 0xff00cb98, 0xff00ff98, 0xff0000cb, 0xff0032cb,
                    0xff0065cb, 0xff0098cb, 0xff00cbcb, 0xff00ffcb, 0xff0000ff, 0xff0032ff,
                    0xff0065ff, 0xff0098ff, 0xff00cbff, 0xff00ffff, 0xff320000, 0xff323200,
                    0xff326500, 0xff329800, 0xff32cb00, 0xff32ff00, 0xff320032, 0xff323232,
                    0xff326532, 0xff329832, 0xff32cb32, 0xff32ff32, 0xff320065, 0xff323265,
                    0xff326565, 0xff329865, 0xff32cb65, 0xff32ff65, 0xff320098, 0xff323298,
                    0xff326598, 0xff329898, 0xff32cb98, 0xff32ff98, 0xff3200cb, 0xff3232cb,
                    0xff3265cb, 0xff3298cb, 0xff32cbcb, 0xff32ffcb, 0xff3200ff, 0xff3232ff,
                    0xff3265ff, 0xff3298ff, 0xff32cbff, 0xff32ffff, 0xff650000, 0xff653200,
                    0xff656500, 0xff659800, 0xff65cb00, 0xff65ff00, 0xff650032, 0xff653232,
                    0xff656532, 0xff659832, 0xff65cb32, 0xff65ff32, 0xff650065, 0xff653265,
                    0xff656565, 0xff659865, 0xff65cb65, 0xff65ff65, 0xff650098, 0xff653298,
                    0xff656598, 0xff659898, 0xff65cb98, 0xff65ff98, 0xff6500cb, 0xff6532cb,
                    0xff6565cb, 0xff6598cb, 0xff65cbcb, 0xff65ffcb, 0xff6500ff, 0xff6532ff,
                    0xff6565ff, 0xff6598ff, 0xff65cbff, 0xff65ffff, 0xff980000, 0xff983200,
                    0xff986500, 0xff989800, 0xff98cb00, 0xff98ff00, 0xff980032, 0xff983232,
                    0xff986532, 0xff989832, 0xff98cb32, 0xff98ff32, 0xff980065, 0xff983265,
                    0xff986565, 0xff989865, 0xff98cb65, 0xff98ff65, 0xff980098, 0xff983298,
                    0xff986598, 0xff989898, 0xff98cb98, 0xff98ff98, 0xff9800cb, 0xff9832cb,
                    0xff9865cb, 0xff9898cb, 0xff98cbcb, 0xff98ffcb, 0xff9800ff, 0xff9832ff,
                    0xff9865ff, 0xff9898ff, 0xff98cbff, 0xff98ffff, 0xffcb0000, 0xffcb3200,
                    0xffcb6500, 0xffcb9800, 0xffcbcb00, 0xffcbff00, 0xffcb0032, 0xffcb3232,
                    0xffcb6532, 0xffcb9832, 0xffcbcb32, 0xffcbff32, 0xffcb0065, 0xffcb3265,
                    0xffcb6565, 0xffcb9865, 0xffcbcb65, 0xffcbff65, 0xffcb0098, 0xffcb3298,
                    0xffcb6598, 0xffcb9898, 0xffcbcb98, 0xffcbff98, 0xffcb00cb, 0xffcb32cb,
                    0xffcb65cb, 0xffcb98cb, 0xffcbcbcb, 0xffcbffcb, 0xffcb00ff, 0xffcb32ff,
                    0xffcb65ff, 0xffcb98ff, 0xffcbcbff, 0xffcbffff, 0xffff0000, 0xffff3200,
                    0xffff6500, 0xffff9800, 0xffffcb00, 0xffffff00, 0xffff0032, 0xffff3232,
                    0xffff6532, 0xffff9832, 0xffffcb32, 0xffffff32, 0xffff0065, 0xffff3265,
                    0xffff6565, 0xffff9865, 0xffffcb65, 0xffffff65, 0xffff0098, 0xffff3298,
                    0xffff6598, 0xffff9898, 0xffffcb98, 0xffffff98, 0xffff00cb, 0xffff32cb,
                    0xffff65cb, 0xffff98cb, 0xffffcbcb, 0xffffffcb, 0xffff00ff, 0xffff32ff,
                    0xffff65ff, 0xffff98ff, 0xffffcbff, 0xffffffff
    };
    @CompilationFinal(dimensions = 1) public static final int[][] PIXEL_LOOKUP_TABLE = {
                    PIXEL_LOOKUP_1BIT,
                    PIXEL_LOOKUP_2BIT,
                    null,
                    PIXEL_LOOKUP_4BIT,
                    null,
                    null,
                    null,
                    PIXEL_LOOKUP_8BIT
    };

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
        public static final long INTERRUPT_KEYCODE = ((long) CMD >> 3 << 8) + '.'; // "CMD + ."
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
    }

    private SqueakIOConstants() {
    }
}
