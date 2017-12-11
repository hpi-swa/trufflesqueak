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
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentProfileNode;
import de.hpi.swa.trufflesqueak.nodes.context.PushArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAddNodeGen;
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
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimFileSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimFileStdioHandles;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimFileWriteNodeGen;
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
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNormalizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNotEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPerform;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPrintArgs;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnFalse;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnMinusOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnNil;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnSelf;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnTrue;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnTwo;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnZero;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuit;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuoNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimReplaceFromToNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimShallowCopyNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSinNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSquareRootNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimStringAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimStringAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSubNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSystemAttributeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimUtcClockNodeGen;

public abstract class PrimitiveNodeFactory {
    private static final int PRIM_COUNT = 574;
    @SuppressWarnings("unchecked") private static Class<? extends PrimitiveNode>[] indexPrims = new Class[PRIM_COUNT + 1];
    private static Map<String, Map<String, Class<? extends PrimitiveNode>>> namedPrims = new HashMap<>();

    public static enum IndexedPrimitives {
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
        //
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
        //
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
        //
        FLOAT_EXPONENT(PrimFloatExponentNodeGen.class, 53),
        FLOAT_LDEXP(PrimFloatTimesTwoPowerNodeGen.class, 54),
        FLOAT_SQUARE_ROOT(PrimSquareRootNodeGen.class, 55),
        FLOAT_SIN(PrimSinNodeGen.class, 56),
        FLOAT_ARCTAN(PrimArcTanNodeGen.class, 57),
        FLOAT_LOG_N(PrimLogNNodeGen.class, 58),
        FLOAT_EXP(PrimExpNodeGen.class, 59),
        AT(PrimIndexAtNodeGen.class, 60), // 1-indexed after named inst vars
        AT_PUT(PrimIndexAtPutNodeGen.class, 61),
        SIZE(PrimSizeNodeGen.class, 62),
        STRING_AT(PrimStringAtNodeGen.class, 63), // 1-indexed return a character
        STRING_AT_PUT(PrimStringAtPutNodeGen.class, 64),
        NEXT(PrimitiveNode.class, 65),
        NEXT_PUT(PrimitiveNode.class, 66),
        AT_END(PrimitiveNode.class, 67),
        OBJECT_AT(PrimCompiledCodeAtNodeGen.class, 68), // 1-indexed directly into literal array
        OBJECT_AT_PUT(PrimCompiledCodeAtPutNodeGen.class, 69),
        //
        NEW(PrimNewNodeGen.class, 70),
        NEW_WITH_ARG(PrimNewArgNodeGen.class, 71),
        //
        INST_VAR_AT(PrimAtNodeGen.class, 73), // 1-indexed
        INST_VAR_AT_PUT(PrimAtPutNodeGen.class, 74),
        IDENTITY_HASH(PrimIdentityHashNodeGen.class, 75),
        //
        BLOCK_COPY(PrimitiveNode.class, 80),
        //
        PERFORM(PrimPerform.class, 83),
        //
        REPLACE_FROM_TO(PrimReplaceFromToNodeGen.class, 105),
        //
        EQUIVALENT(PrimEquivalentNodeGen.class, 110),
        CLASS(PrimClassNodeGen.class, 111),
        //
        QUIT(PrimQuit.class, 113),
        //
        EXTERNAL_CALL(AbstractPrimitiveCallNode.class, 117),
        //
        SHORT_AT(PrimAtNodeGen.class, 143), // 1-indexed from start
        SHORT_AT_PUT(PrimAtPutNodeGen.class, 144),
        //
        SHALLOW_COPY(PrimShallowCopyNodeGen.class, 148),
        SYSTEM_ATTR(PrimSystemAttributeNodeGen.class, 149),
        //
        INTEGER_AT(PrimAtNodeGen.class, 165), // 1-indexed from start
        INTEGER_AT_PUT(PrimAtPutNodeGen.class, 166),
        //
        CHARACTER_VALUE(PrimCharacterValueNodeGen.class, 170),
        IMMEDIATE_HASH(PrimIdentityHashNodeGen.class, 171),
        //
        SLOT_AT(PrimAtNodeGen.class, 173),
        SLOT_AT_PUT(PrimAtPutNodeGen.class, 174),
        //
        BEHAVIOR_HASH(PrimIdentityHashNodeGen.class, 175),
        //
        NEXT_HANDLER_CONTEXT(PrimNextHandlerContextNodeGen.class, 197),
        //
        CLOSURE_VALUE(PrimClosureValueFactory.PrimClosureValue0NodeGen.class, 201),
        CLOSURE_VALUE_(PrimClosureValueFactory.PrimClosureValue1NodeGen.class, 202),
        CLOSURE_VALUE__(PrimClosureValueFactory.PrimClosureValue2NodeGen.class, 203),
        CLOSURE_VALUE___(PrimClosureValueFactory.PrimClosureValue3NodeGen.class, 204),
        CLOSURE_VALUE____(PrimClosureValueFactory.PrimClosureValue4NodeGen.class, 205),
        CLOSURE_VALUE_ARGS(PrimClosureValueFactory.PrimClosureValueAryNodeGen.class, 206),
        //
        CONTEXT_AT(PrimIndexAtNodeGen.class, 210),
        CONTEXT_AT_PUT(PrimIndexAtPutNodeGen.class, 211),
        //
        UTC_MICROSECOND_CLOCK(PrimUtcClockNodeGen.class, 240),
        //
        PUSH_SELF(PrimQuickReturnSelf.class, 256),
        PUSH_TRUE(PrimQuickReturnTrue.class, 257),
        PUSH_FALSE(PrimQuickReturnFalse.class, 258),
        PUSH_NIL(PrimQuickReturnNil.class, 259),
        PUSH_MINUS_ONE(PrimQuickReturnMinusOne.class, 260),
        PUSH_ZERO(PrimQuickReturnZero.class, 261),
        PUSH_ONE(PrimQuickReturnOne.class, 262),
        PUSH_TWO(PrimQuickReturnTwo.class, 263),
        //
        LAST(PrimitiveNode.class, PRIM_COUNT);

        IndexedPrimitives(Class<? extends PrimitiveNode> cls, int idx) {
            indexPrims[idx] = cls;
        }
    }

    public static enum NamedPrimitives {
        LARGE_ADD(PrimAddNodeGen.class, "LargeIntegers", "primDigitAdd"),
        LARGE_SUB(PrimSubNodeGen.class, "LargeIntegers", "primDigitSubtract"),
        LARGE_MUL(PrimMulNodeGen.class, "LargeIntegers", "primDigitMultiplyNegative"),
        LARGE_DIV(PrimDigitDivNegativeNodeGen.class, "LargeIntegers", "primDigitDivNegative"),
        LARGE_BIT_AND(PrimBitAndNodeGen.class, "LargeIntegers", "primDigitBitAnd"),
        LARGE_BIT_OR(PrimBitOrNodeGen.class, "LargeIntegers", "primDigitBitOr"),
        LARGE_BIT_SHIFT(PrimBitShiftNodeGen.class, "LargeIntegers", "primDigitBitShiftMagnitude"),
        LARGE_POS_NORMALIZE(PrimNormalizeNodeGen.class, "LargeIntegers", "primNormalizePositive"),
        LARGE_NEG_NORMALIZE(PrimNormalizeNodeGen.class, "LargeIntegers", "primNormalizeNegative"),
        //
        FILE_WRITE(PrimFileWriteNodeGen.class, "FilePlugin", "primitiveFileWrite"),
        FILE_SIZE(PrimFileSizeNodeGen.class, "FilePlugin", "primitiveFileSize"),
        FILE_STDIO_HANDLES(PrimFileStdioHandles.class, "FilePlugin", "primitiveFileStdioHandles"),
        //
        TRUFFLE_PRINT(PrimPrintArgs.class, "TruffleSqueak", "debugPrint"),
        TRUFFLE_DEBUG(PrimDebugger.class, "TruffleSqueak", "debugger"),
        //
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

    public static SqueakNode arg(CompiledCodeObject code, int index) {
        return new ArgumentProfileNode(new PushArgumentNode(code, index));
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
                args[i] = arg(code, i - 1);
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
    public static PrimitiveNode forIdx(CompiledMethodObject code, int primitiveIdx) {
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
