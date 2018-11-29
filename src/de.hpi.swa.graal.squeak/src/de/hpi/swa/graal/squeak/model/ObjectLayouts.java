package de.hpi.swa.graal.squeak.model;

import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.GetObjectArrayNode;

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

    public static final class CHARACTER_SCANNER {
        public static final int DEST_X = 0;
        public static final int LAST_INDEX = 1;
        public static final int XTABLE = 2;
        public static final int MAP = 3;
    }

    /**
     * Relative offsets to {@link CLASS_DESCRIPTION} for {@link ClassObject}.
     */
    public static final class CLASS {
        public static final int SUBCLASSES = 0;
        public static final int NAME = 1;
    }

    public static final class CLASS_BINDING {
        public static final int KEY = 0;
        public static final int VALUE = 1;
    }

    public static final class CLASS_DESCRIPTION {
        public static final int SUPERCLASS = 0;
        public static final int METHOD_DICT = 1;
        public static final int FORMAT = 2;
        public static final int INSTANCE_VARIABLES = 3;
        public static final int ORGANIZATION = 4;
        public static final int SIZE = 5;

        public static String getClassComment(final ClassObject squeakClass) {
            CompilerAsserts.neverPartOfCompilation("For instrumentation access only.");
            final AbstractSqueakObject organization = squeakClass.getOrganization();
            if (organization.isNil()) {
                return null;
            }
            final AbstractSqueakObject classComment = (AbstractSqueakObject) ((PointersObject) organization).at0(CLASS_ORGANIZER.CLASS_COMMENT);
            final NativeObject string = (NativeObject) classComment.send("string");
            return string.asString();
        }
    }

    public static final class CLASS_ORGANIZER {
        public static final int CATEGORY_ARRAY = 0;
        public static final int CATEGORY_STOPS = 1;
        public static final int ELEMENT_ARRAY = 2;
        public static final int SUBJECT = 3;
        public static final int CLASS_COMMENT = 4;
        public static final int COMMENT_STAMP = 5;
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

    public static final class DICTIONARY {
        public static Map<Object, Object> toJavaMap(final PointersObject dictionary) {
            final ArrayObject classBindings = (ArrayObject) dictionary.at0(HASHED_COLLECTION.ARRAY);
            final Map<Object, Object> keyValues = new HashMap<>();
            // TODO: Avoid node allocation in next line.
            for (Object classBinding : GetObjectArrayNode.create().execute(classBindings)) {
                if (classBinding != dictionary.image.nil) {
                    final PointersObject classBindingPointer = (PointersObject) classBinding;
                    keyValues.put(classBindingPointer.at0(CLASS_BINDING.KEY), classBindingPointer.at0(CLASS_BINDING.VALUE));
                }
            }
            return keyValues;
        }
    }

    public static final class ENVIRONMENT {
        public static final int INFO = 0;
        public static final int DECLARATIONS = 1;
        public static final int BINDINGS = 2;
        public static final int UNDECLARED = 3;
        public static final int POLICIES = 4;
        public static final int OBSERVERS = 5;
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

    public static final class EXCEPTION {
        public static final int MESSAGE_TEXT = 0;
        public static final int TAG = 1;
        public static final int SIGNAL_CONTEXT = 2;
        public static final int HANDLER_CONTEXT = 3;
        public static final int OUTER_CONTEXT = 4;
    }

    public static final class FORM {
        public static final int BITS = 0;
        public static final int WIDTH = 1;
        public static final int HEIGHT = 2;
        public static final int DEPTH = 3;
        public static final int OFFSET = 4;
    }

    public static final class HASHED_COLLECTION {
        public static final int TALLY = 0;
        public static final int ARRAY = 1;
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

    public static final class SMALLTALK_IMAGE {
        public static final int GLOBALS = 0;
    }

    public static final class SPECIAL_OBJECT {
        public static final int NIL_OBJECT = 0;
        public static final int FALSE_OBJECT = 1;
        public static final int TRUE_OBJECT = 2;
        public static final int SCHEDULER_ASSOCIATION = 3;
        public static final int CLASS_BITMAP = 4;
        public static final int CLASS_SMALLINTEGER = 5;
        public static final int CLASS_STRING = 6;
        public static final int CLASS_ARRAY = 7;
        public static final int SMALLTALK_DICTIONARY = 8;
        public static final int CLASS_FLOAT = 9;
        public static final int CLASS_METHOD_CONTEXT = 10;
        public static final int CLASS_BLOCK_CONTEXT = 11;
        public static final int CLASS_POINT = 12;
        public static final int CLASS_LARGE_POSITIVE_INTEGER = 13;
        public static final int THE_DISPLAY = 14;
        public static final int CLASS_MESSAGE = 15;
        public static final int CLASS_COMPILED_METHOD = 16;
        public static final int THE_LOW_SPACE_SEMAPHORE = 17;
        public static final int CLASS_SEMAPHORE = 18;
        public static final int CLASS_CHARACTER = 19;
        public static final int SELECTOR_DOES_NOT_UNDERSTAND = 20;
        public static final int SELECTOR_CANNOT_RETURN = 21;
        public static final int THE_INPUT_SEMAPHORE = 22;
        public static final int SPECIAL_SELECTORS = 23;
        public static final int CHARACTER_TABLE = 24;
        public static final int SELECTOR_MUST_BE_BOOLEAN = 25;
        public static final int CLASS_BYTE_ARRAY = 26;
        public static final int CLASS_PROCESS = 27;
        public static final int COMPACT_CLASSES = 28;
        public static final int THE_TIMER_SEMAPHORE = 29;
        public static final int THE_INTERRUPT_SEMAPHORE = 30;
        public static final int FLOAT_PROTO = 31;
        public static final int CLASS_TRUFFLE_OBJECT = 32; // reusing unused slot for TruffleObject
        public static final int SELECTOR_CANNOT_INTERPRET = 34;
        public static final int METHOD_CONTEXT_PROTO = 35;
        public static final int CLASS_BLOCK_CLOSURE = 36;
        public static final int BLOCK_CONTEXT_PROTO = 37;
        public static final int EXTERNAL_OBJECTS_ARRAY = 38;
        public static final int CLASS_PSEUDO_CONTEXT = 39;
        public static final int CLASS_TRANSLATED_METHOD = 40;
        public static final int THE_FINALIZATION_SEMAPHORE = 41;
        public static final int CLASS_LARGE_NEGATIVE_INTEGER = 42;
        public static final int CLASS_EXTERNAL_ADDRESS = 43;
        public static final int CLASS_EXTERNAL_STRUCTURE = 44;
        public static final int CLASS_EXTERNAL_DATA = 45;
        public static final int CLASS_EXTERNAL_FUNCTION = 46;
        public static final int CLASS_EXTERNAL_LIBRARY = 47;
        public static final int SELECTOR_ABOUT_TO_RETURN = 48;
        public static final int SELECTOR_RUN_WITHIN = 49;
        public static final int SELECTOR_ATTEMPT_TO_ASSIGN = 50;
        public static final int PRIM_ERR_TABLE_INDEX = 51;
        public static final int CLASS_ALIEN = 52;
        public static final int INVOKE_CALLBACK_SELECTOR = 53;
        public static final int CLASS_UNSAFE_ALIEN = 54;
        public static final int CLASS_WEAK_FINALIZER = 55;
    }

    public static final class SYNTAX_ERROR_NOTIFICATION {
        public static final int IN_CLASS = 5;
        public static final int CODE = 6;
        public static final int DOIT_FLAG = 7;
        public static final int ERROR_MESSAGE = 8;
        public static final int LOCATION = 9;
        public static final int NEW_SOURCE = 10;
    }

    public static final class TEST_RESULT {
        public static final int FAILURES = 1;
        public static final int ERRORS = 2;
        public static final int PASSES = 3;
    }

    public static final class TEXT {
        public static final int STRING = 0;
        public static final int RUNS = 1;
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
