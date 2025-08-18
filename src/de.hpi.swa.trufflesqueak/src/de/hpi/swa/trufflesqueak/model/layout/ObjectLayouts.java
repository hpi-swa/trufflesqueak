/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model.layout;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;

public final class ObjectLayouts {

    public static final class ADDITIONAL_METHOD_STATE {
        public static final int METHOD = 0;
        public static final int SELECTOR = 1;
    }

    public static final class ASSOCIATION {
        public static final int KEY = 0;
        public static final int VALUE = 1;
    }

    public static final class BINDING {
        public static final int KEY = 0;
        public static final int VALUE = 1;
    }

    public static final class BLOCK_CLOSURE {
        public static final int OUTER_CONTEXT = 0;
        public static final int START_PC_OR_METHOD = 1;
        public static final int ARGUMENT_COUNT = 2;
        public static final int FIRST_COPIED_VALUE = 3;

        /* FullBlockClosure specifics */
        public static final int FULL_RECEIVER = 3;
        public static final int FULL_FIRST_COPIED_VALUE = 4;
    }

    public static final class CHARACTER_SCANNER {
        public static final int DEST_X = 0;
        public static final int LAST_INDEX = 1;
        public static final int XTABLE = 2;
        public static final int MAP = 3;
    }

    public static final class CLASS {
        /**
         * Relative offsets to {@link CLASS_DESCRIPTION} for {@link ClassObject}.
         */
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
            if (organization == NilObject.SINGLETON) {
                return null;
            }
            final AbstractSqueakObjectWithClassAndHash classComment = (AbstractSqueakObjectWithClassAndHash) ((VariablePointersObject) organization).instVarAt0Slow(CLASS_ORGANIZER.CLASS_COMMENT);
            final NativeObject string = (NativeObject) classComment.send(SqueakImageContext.getSlow(), "string");
            return string.asStringUnsafe();
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

    public static final class CLASS_TRAIT {
        /**
         * Relative offsets to {@link CLASS_DESCRIPTION} for {@link ClassObject}.
         */
        public static final int BASE_TRAIT = 6;
    }

    public static final class CONTEXT {
        public static final int SENDER_OR_NIL = 0;
        public static final int INSTRUCTION_POINTER = 1;
        public static final int STACKPOINTER = 2;
        public static final int METHOD = 3;
        public static final int CLOSURE_OR_NIL = 4;
        public static final int RECEIVER = 5;
        public static final int TEMP_FRAME_START = 6;
        public static final int INST_SIZE = 6;
        public static final int SMALL_FRAMESIZE = 16;
        public static final int LARGE_FRAMESIZE = 56;
        public static final int MAX_STACK_SIZE = LARGE_FRAMESIZE - TEMP_FRAME_START;
    }

    public static final class EPHEMERON {
        public static final int KEY = 0;
        public static final int VALUE = 1;
        public static final int MIN_SIZE = 2;
    }

    public enum ERROR_TABLE {
        GENERIC_ERROR, // nil
        BAD_RECEIVER,
        BAD_ARGUMENT,
        BAD_INDEX,
        BAD_NUMBER_OF_ARGUMENTS,
        INAPPROPRIATE_OPERATION,
        UNSUPPORTED_OPERATION,
        NO_MODIFICATION,
        INSUFFICIENT_OBJECT_MEMORY,
        INSUFFICIENT_C_MEMORY,
        NOT_FOUND,
        BAD_METHOD,
        INTERNAL_ERROR_IN_NAMED_PRIMITIVE_MACHINERY,
        OBJECT_MAY_MOVE,
        RESOURCE_LIMIT_EXCEEDED,
        OBJECT_IS_PINNED,
        PRIMITIVE_WRITE_BEYOND_END_OF_OBJECT,
        OBJECT_MOVED,
        OBJECT_NOT_PINNED,
        CALLBACK_ERROR,
        OPERATING_SYSTEM_ERROR,
        FFI_EXCEPTION,
        NEED_COMPACTION,
        OPERATION_FAILED
    }

    public static final class EXCEPTION {
        public static final int MESSAGE_TEXT = 0;
        public static final int TAG = 1;
        public static final int SIGNAL_CONTEXT = 2;
        public static final int HANDLER_CONTEXT = 3;
        public static final int OUTER_CONTEXT = 4;
    }

    public static final class EXTERNAL_LIBRARY_FUNCTION {
        public static final int HANDLE = 0;
        public static final int FLAGS = 1;
        public static final int ARG_TYPES = 2;
        public static final int NAME = 3;
        public static final int MODULE = 4;
        public static final int ERROR_CODE_NAME = 5;
    }

    public static final class EXTERNAL_TYPE {
        public static final int COMPILED_SPEC = 0;
        public static final int REFERENT_CLASS = 1;
        public static final int REFERENCED_TYPE = 2;
        public static final int POINTER_SIZE = 3;
    }

    public static final class FORM {
        public static final int BITS = 0;
        public static final int WIDTH = 1;
        public static final int HEIGHT = 2;
        public static final int DEPTH = 3;
        public static final int OFFSET = 4;
    }

    public static final class FRACTION {
        public static final int NUMERATOR = 0;
        public static final int DENOMINATOR = 1;
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

    public static final class METACLASS {
        /**
         * Relative offsets to {@link CLASS_DESCRIPTION} for {@link ClassObject}.
         */
        public static final int THIS_CLASS = 5;

        public static final int INST_SIZE = 6;
    }

    public static final class METHOD_DICT {
        public static final int NAMES = 2;
        public static final int VALUES = 1;
    }

    public static final class MUTEX {
        public static final int OWNER = 2;
    }

    public static final class POINT {
        public static final long X = 0;
        public static final long Y = 1;
        public static final int SIZE = 2;
    }

    public static final class PROCESS {
        public static final int NEXT_LINK = 0;
        public static final int SUSPENDED_CONTEXT = 1;
        public static final int PRIORITY = 2;
        public static final int LIST = 3;
        public static final int EFFECTIVE_PROCESS = 5;
    }

    public static final class PROCESS_SCHEDULER {
        public static final int PROCESS_LISTS = 0;
        public static final int ACTIVE_PROCESS = 1;
    }

    public static final class SEMAPHORE {
        public static final int EXCESS_SIGNALS = 2;
    }

    public static final class SPECIAL_OBJECT {
        public static final int NIL_OBJECT = 0;
        public static final int FALSE_OBJECT = 1;
        public static final int TRUE_OBJECT = 2;
        public static final int SCHEDULER_ASSOCIATION = 3;
        public static final int CLASS_BITMAP = 4;
        public static final int CLASS_SMALL_INTEGER = 5;
        public static final int CLASS_STRING = 6;
        public static final int CLASS_ARRAY = 7;
        public static final int SMALLTALK_DICTIONARY = 8;
        public static final int CLASS_FLOAT = 9;
        public static final int CLASS_METHOD_CONTEXT = 10;
        public static final int CLASS_WIDE_STRING = 11;
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
        public static final int PROCESS_SIGNALING_LOW_SPACE = 22;
        public static final int SPECIAL_SELECTORS = 23;
        public static final int CHARACTER_TABLE = 24;
        public static final int SELECTOR_MUST_BE_BOOLEAN = 25;
        public static final int CLASS_BYTE_ARRAY = 26;
        public static final int CLASS_PROCESS = 27;
        public static final int COMPACT_CLASSES = 28;
        public static final int THE_TIMER_SEMAPHORE = 29;
        public static final int THE_INTERRUPT_SEMAPHORE = 30;
        public static final int CLASS_DOUBLE_BYTE_ARRAY = 31;
        public static final int CLASS_WORD_ARRAY = 32;
        public static final int CLASS_DOUBLE_WORD_ARRAY = 33;
        public static final int SELECTOR_CANNOT_INTERPRET = 34;
        public static final int METHOD_CONTEXT_PROTO = 35;
        public static final int CLASS_BLOCK_CLOSURE = 36;
        public static final int CLASS_FULL_BLOCK_CLOSURE = 37;
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
        public static final int FOREIGN_CALLBACK_PROCESS = 56;
        public static final int SELECTOR_UNKNOWN_BYTECODE = 57;
        public static final int SELECTOR_COUNTER_TRIPPED = 58;
        public static final int SELECTOR_SISTA_TRAP = 59;
        public static final int LOWCODE_CONTEXT_MARK = 60;
        public static final int LOWCODE_NATIVE_CONTEXT_CLASS = 61;
    }

    public static final class SYNTAX_ERROR_NOTIFICATION {
        public static final int IN_CLASS = 5;
        public static final int CODE = 6;
        public static final int DOIT_FLAG = 7;
        public static final int ERROR_MESSAGE = 8;
        public static final int LOCATION = 9;
        public static final int NEW_SOURCE = 10;
    }

    private ObjectLayouts() {
    }
}
