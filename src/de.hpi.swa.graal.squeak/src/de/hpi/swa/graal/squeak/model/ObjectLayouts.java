package de.hpi.swa.graal.squeak.model;

public final class ObjectLayouts {

    public static final class ADDITIONAL_METHOD_STATE {
        public static final int METHOD = 0;
        public static final int SELECTOR = 1;
    }

    public static final class ASSOCIATION {
        public static final int KEY = 0;
        public static final int VALUE = 1;
    }

    public static final class BIT_BLT {
        public static final int DEST_FORM = 0;
        public static final int SOURCE_FORM = 1;
        public static final int HALFTONE_FORM = 2;
        public static final int COMBINATION_RULE = 3;
        public static final int DEST_X = 4;
        public static final int DEST_Y = 5;
        public static final int WIDTH = 6;
        public static final int HEIGHT = 7;
        public static final int SOURCE_X = 8;
        public static final int SOURCE_Y = 9;
        public static final int CLIP_X = 10;
        public static final int CLIP_Y = 11;
        public static final int CLIP_WIDTH = 12;
        public static final int CLIP_HEIGHT = 13;
        public static final int COLOR_MAP = 14;
    }

    public static final class BLOCK_CLOSURE {
        public static final int OUTER_CONTEXT = 0;
        public static final int START_PC = 1;
        public static final int ARGUMENT_COUNT = 2;
        public static final int FIRST_COPIED_VALUE = 3;
    }

    public static final class BLOCK_CONTEXT { // only used by blockCopy primitive
        public static final int CALLER = 0;
        public static final int ARGUMENT_COUNT = 3;
        public static final int INITIAL_PC = 4;
        public static final int HOME = 5;
    }

    /**
     * Relative offsets to {@link CLASS_DESCRIPTION} for {@link ClassObject}.
     */
    public static final class CLASS {
        public static final int SUBCLASSES = 0;
        public static final int NAME = 1;
    }

    public static final class CLASS_DESCRIPTION {
        public static final int SUPERCLASS = 0;
        public static final int METHOD_DICT = 1;
        public static final int FORMAT = 2;
        public static final int INSTANCE_VARIABLES = 3;
        public static final int ORGANIZATION = 4;
        public static final int SIZE = 5;
    }

    public static final class CONTEXT {
        public static final int SENDER_OR_NIL = 0;
        public static final int INSTRUCTION_POINTER = 1;
        public static final int STACKPOINTER = 2;
        public static final int METHOD = 3;
        public static final int CLOSURE_OR_NIL = 4;
        public static final int RECEIVER = 5;
        public static final int TEMP_FRAME_START = 6;
        public static final int SMALL_FRAMESIZE = 16;
        public static final int LARGE_FRAMESIZE = 56;
        public static final int MAX_STACK_SIZE = LARGE_FRAMESIZE - TEMP_FRAME_START;
    }

    public static final class ERROR_TABLE {
        public static final int GENERIC_ERROR = 0; // nil
        public static final int BAD_RECEIVER = 1;
        public static final int BAD_ARGUMENT = 2;
        public static final int BAD_INDEX = 3;
        public static final int BAD_NUMBER_OF_ARGUMENTS = 4;
        public static final int INAPPROPRIATE_OPERATION = 5;
        public static final int UNSUPPORTED_OPERATION = 6;
        public static final int NO_MODIFICATION = 7;
        public static final int INSUFFICIENT_OBJECT_MEMORY = 8;
        public static final int INSUFFICIENT_C_MEMORY = 9;
        public static final int NOT_FOUND = 10;
        public static final int BAD_METHOD = 11;
        public static final int INTERNAL_ERROR_IN_NAMED_PRIMITIVE_MACHINERY = 12;
        public static final int OBJECT_MAY_MOVE = 13;
        public static final int RESOURCE_LIMIT_EXCEEDED = 14;
        public static final int OBJECT_IS_PINNED = 15;
        public static final int PRIMITIVE_WRITE_BEYOND_END_OF_OBJECT = 16;
        public static final int OBJECT_MOVED = 17;
        public static final int OBJECT_NOT_PINNED = 18;
        public static final int CALLBACK_ERROR = 19;
        public static final int OPERATING_SYSTEM_ERROR = 20;
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

    public static final class MESSAGE {
        public static final int SELECTOR = 0;
        public static final int ARGUMENTS = 1;
        public static final int LOOKUP_CLASS = 2;
    }

    /**
     * Relative offsets to {@link CLASS_DESCRIPTION} for {@link ClassObject}.
     */
    public static final class METACLASS {
        public static final int THIS_CLASS = 0;
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

    public static final class SPECIAL_OBJECT_INDEX {
        public static final int NilObject = 0;
        public static final int FalseObject = 1;
        public static final int TrueObject = 2;
        public static final int SchedulerAssociation = 3;
        public static final int ClassBitmap = 4;
        public static final int ClassSmallInteger = 5;
        public static final int ClassString = 6;
        public static final int ClassArray = 7;
        public static final int SmalltalkDictionary = 8;
        public static final int ClassFloat = 9;
        public static final int ClassMethodContext = 10;
        public static final int ClassBlockContext = 11;
        public static final int ClassPoint = 12;
        public static final int ClassLargePositiveInteger = 13;
        public static final int TheDisplay = 14;
        public static final int ClassMessage = 15;
        public static final int ClassCompiledMethod = 16;
        public static final int TheLowSpaceSemaphore = 17;
        public static final int ClassSemaphore = 18;
        public static final int ClassCharacter = 19;
        public static final int SelectorDoesNotUnderstand = 20;
        public static final int SelectorCannotReturn = 21;
        public static final int TheInputSemaphore = 22;
        public static final int SpecialSelectors = 23;
        public static final int CharacterTable = 24;
        public static final int SelectorMustBeBoolean = 25;
        public static final int ClassByteArray = 26;
        public static final int ClassProcess = 27;
        public static final int CompactClasses = 28;
        public static final int TheTimerSemaphore = 29;
        public static final int TheInterruptSemaphore = 30;
        public static final int FloatProto = 31;
        public static final int ClassTruffleObject = 32; // reusing unused slot for TruffleObject
        public static final int SelectorCannotInterpret = 34;
        public static final int MethodContextProto = 35;
        public static final int ClassBlockClosure = 36;
        public static final int BlockContextProto = 37;
        public static final int ExternalObjectsArray = 38;
        public static final int ClassPseudoContext = 39;
        public static final int ClassTranslatedMethod = 40;
        public static final int TheFinalizationSemaphore = 41;
        public static final int ClassLargeNegativeInteger = 42;
        public static final int ClassExternalAddress = 43;
        public static final int ClassExternalStructure = 44;
        public static final int ClassExternalData = 45;
        public static final int ClassExternalFunction = 46;
        public static final int ClassExternalLibrary = 47;
        public static final int SelectorAboutToReturn = 48;
        public static final int SelectorRunWithIn = 49;
        public static final int SelectorAttemptToAssign = 50;
        public static final int PrimErrTableIndex = 51;
        public static final int ClassAlien = 52;
        public static final int InvokeCallbackSelector = 53;
        public static final int ClassUnsafeAlien = 54;
        public static final int ClassWeakFinalizer = 55;
    }

    public static final class TEST_RESULT {
        public static final int FAILURES = 1;
        public static final int ERRORS = 2;
        public static final int PASSES = 3;
    }

    public static final class WEAK_FINALIZATION_LIST {
        public static final int FIRST = 0;
    }

    public static final class WEAK_FINALIZER_ITEM {
        public static final int LIST = 0;
        public static final int NEXT = 1;
    }

    private ObjectLayouts() {
    }
}
