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

    public enum FFI_TYPES {

        /*
         * // void type, boolean type VOID("void", 0), // VOID BOOL("bool", 1), // OBJECT BOOL UINT8
         * // basic integer types UNSIGNED_BYTE("byte", 2), // UINT8 SIGNED_BYTE("sbyte", 3), //
         * SINT8 UNSIGNED_SHORT("ushort", 4), // UINT16 SIGNED_SHORT("short", 5), // SINT16
         * UNSIGNED_INT("ulong", 6), // UINT32 SIGNED_INT("long", 7), // SINT32 // 64bit types
         * UNSIGNED_LONG_LONG("ulonglong", 8), // UINT64 SIGNED_LONG_LONG("longlong", 9), // SINT64
         * // special integer types UNSIGNED_CHAR("string", 10), // STRING SIGNED_CHAR("schar", 11),
         * // POINTER // float types SINGLE_FLOAT("float", 12), // FLOAT DOUBLE_FLOAT("double", 13),
         * // DOUBLE
         */

        VOID("void", "POINTER", 0), // TODO: ändern
        BOOL("bool", "UINT8", 1), // OBJECT BOOL UINT8
        // basic integer types
        UNSIGNED_BYTE("byte", "UINT8", 2), // UINT8
        SIGNED_BYTE("sbyte", "SINT8", 3), // SINT8
        UNSIGNED_SHORT("ushort", "UINT16", 4), // UINT16
        SIGNED_SHORT("short", "SINT16", 5), // SINT16
        UNSIGNED_INT("ulong", "UINT32", 6), // UINT32
        SIGNED_INT("long", "SINT32", 7), // SINT32
        // 64bit types
        UNSIGNED_LONG_LONG("ulonglong", "UINT64", 8), // UINT64
        SIGNED_LONG_LONG("longlong", "SINT64", 9), // SINT64
        // special integer types
        UNSIGNED_CHAR("string", "UINT8", 10), // STRING
        SIGNED_CHAR("schar", "POINTER", 11), // POINTER
        // float types
        SINGLE_FLOAT("float", "FLOAT", 12), // FLOAT
        DOUBLE_FLOAT("double", "DOUBLE", 13), // DOUBLE

        // type flags
        FLAG_ATOMIC(0x40000), // type is atomic
        FLAG_POINTER(0x2000), // type is pointer to base type public
        FLAG_STRUCTURE(0x10000), // baseType is structure of 64k length
        // public
        STRUCT_SIZE_MASK(0xFFFF), // mask for max size of structure public
        ATOMIC_TYPE_MASK(0x0F000000), // mask for atomic type spec public
        ATOMIC_TYPE_SHIFT(24); // shift for atomic type

        private String squeakType;
        private String truffleType;
        private int integerValue;

        FFI_TYPES(final int integerValue) {
            this.integerValue = integerValue;
        }

        FFI_TYPES(final String squeakType, final int integerValue) {
            this.squeakType = squeakType;
            this.integerValue = integerValue;
        }

        FFI_TYPES(final String squeakType, final String truffleType, final int integerValue) {
            this.squeakType = squeakType;
            this.truffleType = truffleType;
            this.integerValue = integerValue;
        }

        public static FFI_TYPES getIntegerValueFromString(final String typeKey) {
            for (final FFI_TYPES type : FFI_TYPES.values()) {
                if (type.squeakType.equals(typeKey)) {
                    return type;
                }
            }
            return null;
        }

        public static String getSqueakTypeFromInt(final int typeValue) {
            for (final FFI_TYPES type : FFI_TYPES.values()) {
                if (type.integerValue == typeValue) {
                    return type.squeakType;
                }
            }
            return null;
        }

        public static String getTruffleTypeFromInt(final int typeValue) {
            for (final FFI_TYPES type : FFI_TYPES.values()) {
                if (type.integerValue == typeValue) {
                    return type.truffleType;
                }
            }
            return null;
        }

        @Override
        public String toString() {
            return squeakType;
        }

        public int getValue() {
            return integerValue;
        }
    }
}
