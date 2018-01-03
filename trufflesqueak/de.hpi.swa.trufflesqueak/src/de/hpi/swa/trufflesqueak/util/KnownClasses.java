package de.hpi.swa.trufflesqueak.util;

public final class KnownClasses {
    public static final class ASSOCIATION {
        public static final int KEY = 0;
        public static final int VALUE = 1;
    }

    public static final class BLOCK_CLOSURE {
        public static final int OUTER_CONTEXT = 0;
        public static final int INITIAL_PC = 1;
        public static final int ARGUMENT_COUNT = 2;
        public static final int FIRST_COPIED_VALUE = 3;

    }

    public static final class BLOCK_CONTEXT {
        public static final int CALLER = 0;
        public static final int ARGUMENT_COUNT = 3;
        public static final int INITIAL_PC = 4;
        public static final int HOME = 5;
    }

    public static final class CLASS {
        public static final int SUPERCLASS = 0;
        public static final int METHOD_DICT = 1;
        public static final int FORMAT = 2;
        public static final int NAME = 6;
    }

    public static final class CONTEXT {
        public static final int SENDER = 0;
        public static final int INSTRUCTION_POINTER = 1;
        public static final int STACKPOINTER = 2;
        public static final int METHOD = 3;
        public static final int CLOSURE = 4;
        public static final int RECEIVER = 5;
        public static final int TEMP_FRAME_START = 6;
        public static final int SMALL_FRAMESIZE = 16;
        public static final int LARGE_FRAMESIZE = 56;
    }

    public static final class FORM {
        public static final int BITS = 0;
        public static final int WIDTH = 1;
        public static final int HEIGHT = 2;
        public static final int DEPTH = 3;
        public static final int OFFSET = 4;
    }

    public static final class LINK {
        public static final int NEXT_LINK = 0;
    }

    public static final class LINKED_LIST {
        public static final int FIRST_LINK = 0;
        public static final int LAST_LINK = 1;
    }

    public static final class METHOD_DICT {
        public static final int NAMES = 2;
        public static final int VALUES = 1;
    }

    public static final class MUTEX {
        public static final int OWNER = 2;
    }

    public static final class POINT {
        public static final int X = 0;
        public static final int Y = 1;
        public static final int SIZE = 2;
    }

    public static final class PROCESS {
        public static final int SUSPENDED_CONTEXT = 1;
        public static final int PRIORITY = 2;
        public static final int LIST = 3;
    }

    public static final class PROCESS_SCHEDULER {
        public static final int PROCESS_LISTS = 0;
        public static final int ACTIVE_PROCESS = 1;
    }

    public static final class SEMAPHORE {
        public static final int EXCESS_SIGNALS = 2;
    }

    public static final class WEAK_FINALIZATION_LIST {
        public static final int FIRST = 0;
    }

    public static final class WEAK_FINALIZER_ITEM {
        public static final int LIST = 0;
        public static final int NEXT = 1;
    }
}
