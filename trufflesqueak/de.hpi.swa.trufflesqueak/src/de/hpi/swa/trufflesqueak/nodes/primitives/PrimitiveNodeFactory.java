package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameArgumentProfileNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAddNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAllInstancesNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimArcTanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAsFloatNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitAndNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitOrNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitShiftNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitXorNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimCharacterValueNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClosureValueFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimCompiledCodeAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimCompiledCodeAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDebugger;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDigitDivNegativeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDivNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDivideNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimEquivalentNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimExpNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimFloatExponentNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimFloatTimesTwoPowerNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimFloatTruncatedNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimGreaterOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimGreaterThanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimIdentityHashNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimIndexAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimIndexAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLessOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLessThanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLogNNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimModNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimMulNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNewArgNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNewNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNextHandlerContextNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNextInstanceNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNormalizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNotEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPerform;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPrintArgs;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuit;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuoNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimReplaceFromToNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimShallowCopyNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSinNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSomeInstanceNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSquareRootNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimStringAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimStringAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSubNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSystemAttributeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimUtcClockNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.file.PrimFileSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.file.PrimFileStdioHandles;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.file.PrimFileWriteNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnFalse;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnMinusOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnNil;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnSelf;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnTrue;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnTwo;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.quickreturn.PrimQuickReturnZero;

public abstract class PrimitiveNodeFactory {
    private static final int PRIM_COUNT = 574;
    @SuppressWarnings("unchecked") private static Class<? extends PrimitiveNode>[] indexPrims = new Class[PRIM_COUNT + 1];
    private static Map<String, Map<String, Class<? extends PrimitiveNode>>> namedPrims = new HashMap<>();

    private static enum IndexedPrimitives {
        /* SmallInteger Primitives */
        ADD(PrimAddNodeGen.class, 1),
        SUB(PrimSubNodeGen.class, 2),
        LESSTHAN(PrimLessThanNodeGen.class, 3),
        GREATERTHAN(PrimGreaterThanNodeGen.class, 4),
        LESSOREQUAL(PrimLessOrEqualNodeGen.class, 5),
        GREATEROREQUAL(PrimGreaterOrEqualNodeGen.class, 6),
        EQUAL(PrimEqualNodeGen.class, 7),
        NOTEQUAL(PrimNotEqualNodeGen.class, 8),
        MULTIPLY(PrimMulNodeGen.class, 9),
        DIVIDE(PrimDivideNodeGen.class, 10),
        MOD(PrimModNodeGen.class, 11),
        DIV(PrimDivNodeGen.class, 12),
        QUO(PrimQuoNodeGen.class, 13),
        BIT_AND(PrimBitAndNodeGen.class, 14),
        BIT_OR(PrimBitOrNodeGen.class, 15),
        BIT_XOR(PrimBitXorNodeGen.class, 16),
        BIT_SHIFT(PrimBitShiftNodeGen.class, 17),
        // TODO: MAKE_POINT(, 18),
        // TODO: FAIL(, 19),
        // TODO: LARGE_OFFSET(, 20),
        LARGE_ADD(PrimAddNodeGen.class, 21),
        LARGE_SUB(PrimSubNodeGen.class, 22),
        LARGE_LESSTHAN(PrimLessThanNodeGen.class, 23),
        LARGE_GREATERTHAN(PrimGreaterThanNodeGen.class, 24),
        LARGE_LESSOREQUAL(PrimLessOrEqualNodeGen.class, 25),
        LARGE_GREATEROREQUAL(PrimGreaterOrEqualNodeGen.class, 26),
        LARGE_EQUAL(PrimEqualNodeGen.class, 27),
        LARGE_NOTEQUAL(PrimNotEqualNodeGen.class, 28),
        LARGE_MULTIPLY(PrimMulNodeGen.class, 29),
        LARGE_DIVIDE(PrimDivideNodeGen.class, 30),
        LARGE_MOD(PrimModNodeGen.class, 31),
        LARGE_DIV(PrimDivNodeGen.class, 32),
        LARGE_QUO(PrimQuoNodeGen.class, 33),
        LARGE_BIT_AND(PrimBitAndNodeGen.class, 34),
        LARGE_BIT_OR(PrimBitOrNodeGen.class, 35),
        LARGE_BIT_XOR(PrimBitXorNodeGen.class, 36),
        LARGE_BIT_SHIFT(PrimBitShiftNodeGen.class, 37),

        /* Float Primitives */
        AS_FLOAT(PrimAsFloatNodeGen.class, 40),
        FLOAT_ADD(PrimAddNodeGen.class, 41),
        FLOAT_SUB(PrimSubNodeGen.class, 42),
        FLOAT_LESSTHAN(PrimLessThanNodeGen.class, 43),
        FLOAT_GREATERTHAN(PrimGreaterThanNodeGen.class, 44),
        FLOAT_LESSOREQUAL(PrimLessOrEqualNodeGen.class, 45),
        FLOAT_GREATEROREQUAL(PrimGreaterOrEqualNodeGen.class, 46),
        FLOAT_EQUAL(PrimEqualNodeGen.class, 47),
        FLOAT_NOTEQUAL(PrimNotEqualNodeGen.class, 48),
        FLOAT_MULTIPLY(PrimMulNodeGen.class, 49),
        FLOAT_DIVIDE(PrimDivideNodeGen.class, 50),
        FLOAT_TRUNCATED(PrimFloatTruncatedNodeGen.class, 51),
        /* optional: 52, 53 */
        FLOAT_EXPONENT(PrimFloatExponentNodeGen.class, 53),
        FLOAT_LDEXP(PrimFloatTimesTwoPowerNodeGen.class, 54),
        FLOAT_SQUARE_ROOT(PrimSquareRootNodeGen.class, 55),
        FLOAT_SIN(PrimSinNodeGen.class, 56),
        FLOAT_ARCTAN(PrimArcTanNodeGen.class, 57),
        FLOAT_LOG_N(PrimLogNNodeGen.class, 58),
        FLOAT_EXP(PrimExpNodeGen.class, 59),

        /* Subscript and Stream Primitives */
        AT(PrimIndexAtNodeGen.class, 60), // 1-indexed after named inst vars
        AT_PUT(PrimIndexAtPutNodeGen.class, 61),
        SIZE(PrimSizeNodeGen.class, 62),
        STRING_AT(PrimStringAtNodeGen.class, 63), // 1-indexed return a character
        STRING_AT_PUT(PrimStringAtPutNodeGen.class, 64),
        NEXT(PrimitiveNode.class, 65),
        NEXT_PUT(PrimitiveNode.class, 66),
        AT_END(PrimitiveNode.class, 67),

        /* Storage Management Primitives */
        OBJECT_AT(PrimCompiledCodeAtNodeGen.class, 68), // 1-indexed directly into literal array
        OBJECT_AT_PUT(PrimCompiledCodeAtPutNodeGen.class, 69),
        NEW(PrimNewNodeGen.class, 70),
        NEW_WITH_ARG(PrimNewArgNodeGen.class, 71),
        // TODO: ARRAY_BECOME_ONE_WAY(, 72),
        INST_VAR_AT(PrimAtNodeGen.class, 73), // 1-indexed
        INST_VAR_AT_PUT(PrimAtPutNodeGen.class, 74),
        IDENTITY_HASH(PrimIdentityHashNodeGen.class, 75),
        // TODO: STORE_STACKP(, 76),
        SOME_INSTANCE(PrimSomeInstanceNodeGen.class, 77),
        NEXT_INSTANCE(PrimNextInstanceNodeGen.class, 78),
        // TODO: NEW_METHOD(, 79),

        /* Control Primitives */
        BLOCK_COPY(PrimitiveNode.class, 80),
        // TODO: VALUE(, 81),
        // TODO: VALUE_WITH_ARGS(, 82),
        PERFORM(PrimPerform.class, 83),
        // TODO: PERFORM_WITH_ARGS(, 84),
        // TODO: SIGNAL(, 85),
        // TODO: WAIT(, 86),
        // TODO: RESUME(, 87),
        // TODO: SUSPEND(, 88),
        // TODO: FLUSH_CACHE(, 89),

        /* I/O Primitives */
        // TODO: MOUSE_POINT(, 90),
        // TODO: TEST_DISPLAY_DEPTH(, 91),
        // TODO: SET_DISPLAY_MODE(, 92),
        // TODO: INPUT_SEMAPHORE(, 93),
        // TODO: GET_NEXT_EVENT(, 94),
        // TODO: INPUT_WORD(, 95),
        // TODO: BITBLT_COPY_BITS(, 96),
        // TODO: SNAPSHOT(, 97),
        // TODO: STORE_IMAGE_SEGMENT(, 98),
        // TODO: LOAD_IMAGE_SEGMENT(, 99),
        // TODO: PERFORM_IN_SUPERCLASS(, 100),
        // TODO: BE_CURSOR(, 101),
        // TODO: BE_DISPLAY(, 102),
        // TODO: SCAN_CHARACTERS(, 103),
        // TODO: OBSOLETE_INDEXED(, 104),
        REPLACE_FROM_TO(PrimReplaceFromToNodeGen.class, 105),
        // TODO: SCREEN_SIZE(, 106),
        // TODO: MOUSE_BUTTONS(, 107),
        // TODO: KBD_NEXT(, 108),
        // TODO: KBD_PEEK(, 109),

        /* Control Primitives */
        EQUIVALENT(PrimEquivalentNodeGen.class, 110),
        CLASS(PrimClassNodeGen.class, 111),
        // TODO: BYTES_LEFT(, 112),
        QUIT(PrimQuit.class, 113),
        // TODO: EXIT_TO_DEBUGGER(, 114),
        // TODO: CHANGE_CLASS(, 115),
        // TODO: COMPILED_METHOD_FLUSH_CACHE(, 116),
        EXTERNAL_CALL(NamedPrimitiveCallNode.class, 117),
        /* optional 118 */
        // TODO: SYMBOL_FLUSH_CACHE(, 119),

        /* Miscellaneous Primitives */
        // TODO: CALLOUT_TO_FFI(, 120)
        // TODO: IMAGE_NAME(, 121)
        // TODO: NOOP(, 122)
        // TODO: VALUE_UNINTERRUPTABLY(, 123)
        // TODO: LOW_SPACE_SEMAPHORE(, 124)
        // TODO: SIGNAL_AT_BYTES_LEFT(, 125)
        // TODO: DEFER_UPDATES(, 126)
        // TODO: DRAW_RECTANGLE(, 127)

        /* Squeak Miscellaneous Primitives */
        // TODO: BECOME(, 128)
        // TODO: SPECIAL_OBJECTS_ARRAY(, 129)
        // TODO: FULL_GC(, 130)
        // TODO: INC_GC(, 131)
        // TODO: SET_INTERRUPT_KEY(, 133)
        // TODO: INTERRUPT_SEMAPHORE(, 134)

        /* Time Primitives */
        // TODO: MILLISECOND_CLOCK(, 135)
        // TODO: SIGNAL_AT_MILLISECONDS(, 136)
        // TODO: SECONDS_CLOCK(, 137)

        /* Misc Primitives */
        // TODO: SOME_OBJECT(, 138)
        // TODO: NEXT_OBJECT(, 139)
        // TODO: BEEP(, 140)
        // TODO: CLIPBOARD_TEXT(, 141)
        // TODO: VM_PATH(, 142)
        SHORT_AT(PrimAtNodeGen.class, 143), // 1-indexed from start
        SHORT_AT_PUT(PrimAtPutNodeGen.class, 144),
        // TODO: FILL(, 145)
        /* optional 146, 147 */
        SHALLOW_COPY(PrimShallowCopyNodeGen.class, 148),
        SYSTEM_ATTR(PrimSystemAttributeNodeGen.class, 149),

        /* File primitives */
        /* (they are obsolete in Squeak and done with a plugin) */
        // FILE_AT_END(, 150)
        // FILE_CLOSE(, 151)
        // FILE_GET_POSITION(, 152)
        // FILE_OPEN(, 153)
        // FILE_READ(, 154)
        // FILE_SET_POSITION(, 155)
        // FILE_DELETE(, 156)
        // FILE_SIZE(, 157)
        // FILE_WRITE(, 158)
        // FILE_RENAME(, 159)
        // DIRECTORY_CREATE(, 160)
        // DIRECTORY_DELIMITOR(, 161)
        // DIRECTORY_LOOKUP(, 162)
        // DIRECTORY_DELETE(, 163)

        /* Misc primitives */
        INTEGER_AT(PrimAtNodeGen.class, 165), // 1-indexed from start
        INTEGER_AT_PUT(PrimAtPutNodeGen.class, 166),
        // TODO: YIELD(, 167)
        /* optional 168, 169 */
        CHARACTER_VALUE(PrimCharacterValueNodeGen.class, 170),
        IMMEDIATE_HASH(PrimIdentityHashNodeGen.class, 171),
        /* optional 172 */
        SLOT_AT(PrimAtNodeGen.class, 173),
        SLOT_AT_PUT(PrimAtPutNodeGen.class, 174),
        BEHAVIOR_HASH(PrimIdentityHashNodeGen.class, 175),
        // TODO: MAX_IDENTITY_HASH(, 176)
        ALL_INSTANCES(PrimAllInstancesNodeGen.class, 177),
        // TODO: ALL_OBJECTS(, 178)
        // TODO: BYTE_SIZE_OF_INSTANCE(, 181)
        // TODO: EXIT_CRITICAL_SECTION(, 185)
        // TODO: ENTER_CRITICAL_SECTION(, 186)
        // TODO: TEST_AND_SET_OWNERSHIP_OF_CRITICAL_SECTION(, 187)
        // TODO: WITH_ARGS_EXECUTE_METHOD(, 188)

        /* Optional and not needed */
        NEXT_HANDLER_CONTEXT(PrimNextHandlerContextNodeGen.class, 197),

        /* BlockClosure Primitives */
        CLOSURE_VALUE(PrimClosureValueFactory.PrimClosureValue0NodeGen.class, 201),
        CLOSURE_VALUE_(PrimClosureValueFactory.PrimClosureValue1NodeGen.class, 202),
        CLOSURE_VALUE__(PrimClosureValueFactory.PrimClosureValue2NodeGen.class, 203),
        CLOSURE_VALUE___(PrimClosureValueFactory.PrimClosureValue3NodeGen.class, 204),
        CLOSURE_VALUE____(PrimClosureValueFactory.PrimClosureValue4NodeGen.class, 205),
        CLOSURE_VALUE_ARGS(PrimClosureValueFactory.PrimClosureValueAryNodeGen.class, 206),
        /* optional 207, 208, 209 */
        CONTEXT_AT(PrimIndexAtNodeGen.class, 210),
        CONTEXT_AT_PUT(PrimIndexAtPutNodeGen.class, 211),
        // TODO: CTXT_SIZE(, 212)
        // TODO: CLOSURE_VALUE_NO_CONTEXT_SWITCH(, 221)
        // TODO: CLOSURE_VALUE_NO_CONTEXT_SWITCH_(, 222)

        /* Drawing */
        // TODO: IDLE_FOR_MICROSECONDS(, 230)
        // TODO: FORCE_DISPLAY_UPDATE(, 231)
        // TODO: SET_FULL_SCREEN(, 233)

        /* Time Primitives */
        UTC_MICROSECOND_CLOCK(PrimUtcClockNodeGen.class, 240),
        // TODO: LOCAL_MICROSECOND_CLOCK(, 241)
        // TODO: SIGNAL_AT_UTC_MICROSECONDS(, 242)
        // TODO: UPDATE_TIMEZONE(, 243)

        /* VM implementor primitives */
        // TODO: VM_CLEAR_PROFILE(, 250)
        // TODO: VM_DUMP_PROFILE(, 251)
        // TODO: VM_START_PROFILING(, 252)
        // TODO: VM_STOP_PROFILING(, 253)
        // TODO: VM_PARAMETERS(, 254)

        /* Quick Push Const Primitives */
        PUSH_SELF(PrimQuickReturnSelf.class, 256),
        PUSH_TRUE(PrimQuickReturnTrue.class, 257),
        PUSH_FALSE(PrimQuickReturnFalse.class, 258),
        PUSH_NIL(PrimQuickReturnNil.class, 259),
        PUSH_MINUS_ONE(PrimQuickReturnMinusOne.class, 260),
        PUSH_ZERO(PrimQuickReturnZero.class, 261),
        PUSH_ONE(PrimQuickReturnOne.class, 262),
        PUSH_TWO(PrimQuickReturnTwo.class, 263),

        /* VM primitives */
        // TODO: VM_LOADED_MODULES( ,573)

        LAST(PrimitiveNode.class, PRIM_COUNT);

        IndexedPrimitives(Class<? extends PrimitiveNode> cls, int idx) {
            indexPrims[idx] = cls;
        }
    }

    private static enum NamedPrimitives {
        /* LargeIntegers Plugin */
        LARGE_ADD(PrimAddNodeGen.class, "LargeIntegers", "primDigitAdd"),
        LARGE_SUB(PrimSubNodeGen.class, "LargeIntegers", "primDigitSubtract"),
        LARGE_MUL(PrimMulNodeGen.class, "LargeIntegers", "primDigitMultiplyNegative"),
        LARGE_DIV(PrimDigitDivNegativeNodeGen.class, "LargeIntegers", "primDigitDivNegative"),
        // TODO: DIGIT_COMPARE(PrimDigitDivNegativeNodeGen.class, "LargeIntegers", "primDigitCompare"),
        LARGE_BIT_AND(PrimBitAndNodeGen.class, "LargeIntegers", "primDigitBitAnd"),
        LARGE_BIT_OR(PrimBitOrNodeGen.class, "LargeIntegers", "primDigitBitOr"),
        // TODO: LARGE_BIT_XOR(PrimBitOrNodeGen.class, "LargeIntegers", "primDigitBitXOr"),
        LARGE_BIT_SHIFT(PrimBitShiftNodeGen.class, "LargeIntegers", "primDigitBitShiftMagnitude"),
        LARGE_POS_NORMALIZE(PrimNormalizeNodeGen.class, "LargeIntegers", "primNormalizePositive"),
        LARGE_NEG_NORMALIZE(PrimNormalizeNodeGen.class, "LargeIntegers", "primNormalizeNegative"),

        /* File Plugin */
        // TODO: FILE_DELETE(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileDelete"),
        // TODO: FDIR_DELIMITOR(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveDirectoryDelimitor"),
        // TODO: FDIR_CREATE(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveDirectoryCreate"),
        // TODO: FDIR_DELETE(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveDirectoryDelete"),
        // TODO: FDIR_LOOKUP(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveDirectoryLookup"),
        // TODO: FILE_OPEN(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileOpen"),
        // TODO: FILE_CLOSE(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileClose"),
        // TODO: FILE_AT_END(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileAtEnd"),
        // TODO: FILE_READ(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileRead"),
        // TODO: FILE_GET_POSITION(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileGetPosition"),
        // TODO: FILE_SET_POSITION(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileSetPosition"),
        FILE_SIZE(PrimFileSizeNodeGen.class, "FilePlugin", "primitiveFileSize"),
        FILE_STDIO_HANDLES(PrimFileStdioHandles.class, "FilePlugin", "primitiveFileStdioHandles"),
        FILE_WRITE(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileWrite"),
        // TODO: FILE_TRUNCATE(PrimFileStdioHandles.class, "FilePlugin", "primitiveFileTruncate"),
        // TODO: FDIR_SET_MAC(PrimFileStdioHandles.class, "FilePlugin",
        // "primitiveDirectorySetMacTypeAndCreator"),
        // TODO: FILE_FLUSH(PrimFileStdioHandles.class, "FilePlugin", "primitiveFileFlush"),

        /* TruffleSqueak Plugin */
        TRUFFLE_PRINT(PrimPrintArgs.class, "TruffleSqueak", "debugPrint"),
        TRUFFLE_DEBUG(PrimDebugger.class, "TruffleSqueak", "debugger"),

        LAST(PrimitiveNode.class, "nil", "nil");

        NamedPrimitives(Class<? extends PrimitiveNode> cls, String modulename, String functionname) {
            namedPrims.putIfAbsent(modulename, new HashMap<>());
            namedPrims.get(modulename).put(functionname, cls);
        }
    }

    static {
        // Forces instantiaton of the primitives enum
        IndexedPrimitives.values();
        NamedPrimitives.values();
    }

    private static PrimitiveNode createInstance(CompiledMethodObject code, Class<? extends PrimitiveNode> primClass) {
        if (primClass == null) {
            return new PrimitiveNode(code);
        }
        try {
            int argCount = (int) Arrays.stream(primClass.getDeclaredFields()).filter(f -> f.getAnnotation(Child.class) != null).count();

            Class<?>[] argTypes = new Class<?>[argCount + 1];
            argTypes[0] = CompiledMethodObject.class;
            for (int i = 1; i <= argCount; i++) {
                argTypes[i] = SqueakNode.class;
            }

            Object[] args = new Object[argCount + 1];
            args[0] = code;
            for (int i = 1; i <= argCount; i++) {
                args[i] = new FrameArgumentProfileNode(new FrameArgumentNode(i - 1));
            }

            try {
                Method factoryMethod = primClass.getMethod("create", argTypes);
                return (PrimitiveNode) factoryMethod.invoke(null, args);
            } catch (NoSuchMethodException e) {
                return primClass.getConstructor(CompiledMethodObject.class).newInstance(code);
            }
        } catch (NoSuchMethodException
                        | InstantiationException
                        | SecurityException
                        | IllegalAccessException
                        | IllegalArgumentException
                        | InvocationTargetException e) {
            throw new RuntimeException("Internal error in creating primitive", e);
        }
    }

    public static PrimitiveNode forIdx(CompiledCodeObject code, int primitiveIdx) {
        if (code instanceof CompiledMethodObject) {
            return forIdx((CompiledMethodObject) code, primitiveIdx);
        } else {
            throw new RuntimeException("Primitives only supported in CompiledMethodObject");
        }
    }

    @TruffleBoundary
    private static PrimitiveNode forIdx(CompiledMethodObject code, int primitiveIdx) {
        if (primitiveIdx >= indexPrims.length) {
            return new PrimitiveNode(code);
        } else if (primitiveIdx >= 264 && primitiveIdx <= 520) {
            return new PrimQuickReturnReceiverVariableNode(code, primitiveIdx - 264);
        }
        Class<? extends PrimitiveNode> primClass = indexPrims[primitiveIdx];
        return createInstance(code, primClass);
    }

    @TruffleBoundary
    public static PrimitiveNode forName(CompiledMethodObject method, String modulename, String functionname) {
        Class<? extends PrimitiveNode> primClass = namedPrims.getOrDefault(modulename, new HashMap<>()).get(functionname);
        return createInstance(method, primClass);
    }
}
