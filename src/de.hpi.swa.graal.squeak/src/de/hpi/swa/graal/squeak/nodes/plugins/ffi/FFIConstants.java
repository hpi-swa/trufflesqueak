package de.hpi.swa.graal.squeak.nodes.plugins.ffi;

public class FFIConstants {
    /** See FFIConstants>>initializeErrorConstants. */
    public static final class FFI_ERROR {
        /** "No callout mechanism available". */
        public static final int FFI_NO_CALLOUT_AVAILABLE = -1;
        /** "generic error". */
        public static final int GENERIC_ERROR = 0;
        /** "primitive invoked without ExternalFunction". */
        public static final int NOT_FUNCTION = 1;
        /** "bad arguments to primitive call". */
        public static final int BAD_ARGS = 2;
        /** "generic bad argument". */
        public static final int BAD_ARG = 3;
        /** "int passed as pointer". */
        public static final int INT_AS_POINTER = 4;
        /** "bad atomic type (e.g., unknown)". */
        public static final int BAD_ATOMIC_TYPE = 5;
        /** "argument coercion failed". */
        public static final int COERCION_FAILED = 6;
        /** "Type check for non-atomic types failed". */
        public static final int WRONG_TYPE = 7;
        /** "struct size wrong or too large". */
        public static final int STRUCT_SIZE = 8;
        /** "unsupported calling convention". */
        public static final int CALL_TYPE = 9;
        /** "cannot return the given type". */
        public static final int BAD_RETURN = 10;
        /** "bad function address". */
        public static final int BAD_ADDRESS = 11;
        /** "no module given but required for finding address". */
        public static final int NO_MODULE = 12;
        /** "function address not found". */
        public static final int ADDRESS_NOT_FOUND = 13;
        /** "attempt to pass 'void' parameter". */
        public static final int ATTEMPT_TO_PASS_VOID = 14;
        /** "module not found". */
        public static final int MODULE_NOT_FOUND = 15;
        /** "external library invalid". */
        public static final int BAD_EXTERNAL_LIBRARY = 16;
        /** "external function invalid". */
        public static final int BAD_EXTERNAL_FUNCTION = 17;
        /** "ExternalAddress points to ST memory (don't you dare to do this!)". */
        public static final int INVALID_POINTER = 18;
        /** "Stack frame required more than 16k bytes to pass arguments.". */
        public static final int CALL_FRAME_TOO_BIG = 19;
    }

    public static final class FFI_TYPE {

        // type void
        public static final int VOID = 0;
        // type bool
        public static final int BOOL = 1;
        // basic integer types.
        // note: (integerType anyMask: 1) = integerType isSigned
        public static final int UNSIGNED_BYTE = 2;
        public static final int SIGNED_BYTE = 3;

        public static final int UNSIGNED_SHORT = 4;
        public static final int SIGNED_SHORT = 5;
        public static final int UNSIGNED_INT = 6;
        public static final int SIGNED_INT = 7;

        // 64bit types
        public static final int UNSIGNED_LONG_LONG = 8;
        public static final int SIGNED_LONG_LONG = 9;

        // special integer types
        public static final int UNSIGNED_CHAR = 10;
        public static final int SIGNED_CHAR = 11;

        // float types
        public static final int SINGLE_FLOAT = 12;
        public static final int DOUBLE_FLOAT = 13;

        // type flags
        public static final int FLAG_ATOMIC = 0x40000; // type is atomic
        public static final int FLAG_POINTER = 0x20000; // type is pointer to base type public
        static final int FLAG_STRUCTURE = 0x10000; // baseType is structure of 64k length public
        static final int STRUCT_SIZE_MASK = 0xFFFF; // mask for max size of structure public
        static final int ATOMIC_TYPE_MASK = 0x0F000000; // mask for atomic type spec public
        static final int ATOMIC_TYPE_SHIFT = 24; // shift for atomic type

    }
}
