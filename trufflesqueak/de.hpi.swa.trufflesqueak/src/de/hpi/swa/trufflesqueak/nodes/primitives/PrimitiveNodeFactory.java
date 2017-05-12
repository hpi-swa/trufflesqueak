package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAddNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitAndNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitOrNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitShiftNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimCharacterValueNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClass;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClosureValueFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDivNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDivideNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimEquivalentNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimGreaterOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimGreaterThanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLessOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLessThanNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimMakePoint;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimModNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimMulNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNewArgNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNewNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNotEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPerform;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPrintArgs;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushFalse;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushMinusOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushNil;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushSelf;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushTrue;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushTwo;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushZero;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuickReturnReceiverVariableNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimQuoNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimReplaceFromToNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSubNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimUtcClockNodeGen;

public abstract class PrimitiveNodeFactory {
    private static final int PRIM_COUNT = 574;
    @SuppressWarnings("unchecked")
    private static Class<? extends PrimitiveNode>[] primitiveClasses = new Class[PRIM_COUNT + 1];

    //@formatter:off
    public static enum Primitives {
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
        //
        BIT_SHIFT(PrimBitShiftNodeGen.class, 17),
        MAKE_POINT(PrimMakePoint.class, 18),
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
        //
        LARGE_BIT_SHIFT(PrimBitShiftNodeGen.class, 37),
        //
        AT(PrimAtNodeGen.class, 60),
        AT_PUT(PrimAtPutNodeGen.class, 61),
        SIZE(PrimSizeNodeGen.class, 62),
        STRING_AT(PrimAtNodeGen.class, 63),
        STRING_AT_PUT(PrimAtPutNodeGen.class, 64),
        NEXT(PrimitiveNode.class, 65),
        NEXT_PUT(PrimitiveNode.class, 66),
        AT_END(PrimitiveNode.class, 67),
        //
        NEW(PrimNewNodeGen.class, 70),
        NEW_WITH_ARG(PrimNewArgNodeGen.class, 71),
        //
        BLOCK_COPY(PrimitiveNode.class, 80),
        //
        PERFORM(PrimPerform.class, 83),
        //
        REPLACE_FROM_TO(PrimReplaceFromToNodeGen.class, 105), 
        //
        EQUIVALENT(PrimEquivalentNodeGen.class, 110),
        CLASS(PrimClass.class, 111),
        //
        CHARACTER_VALUE(PrimCharacterValueNodeGen.class, 170),
        //
        CLOSURE_VALUE(PrimClosureValueFactory.PrimClosureValue0NodeGen.class, 201),
        CLOSURE_VALUE_(PrimClosureValueFactory.PrimClosureValue1NodeGen.class, 202),
        CLOSURE_VALUE__(PrimClosureValueFactory.PrimClosureValue2NodeGen.class, 203),
        CLOSURE_VALUE___(PrimClosureValueFactory.PrimClosureValue3NodeGen.class, 204),
        CLOSURE_VALUE____(PrimClosureValueFactory.PrimClosureValue4NodeGen.class, 205),
        CLOSURE_VALUE_ARGS(PrimClosureValueFactory.PrimClosureValueAryNodeGen.class, 206),
        //
        UTC_MICROSECOND_CLOCK(PrimUtcClockNodeGen.class, 240),
        //
        PUSH_SELF(PrimPushSelf.class, 256),
        PUSH_TRUE(PrimPushTrue.class, 257),
        PUSH_FALSE(PrimPushFalse.class, 258),
        PUSH_NIL(PrimPushNil.class, 259),
        PUSH_MINUS_ONE(PrimPushMinusOne.class, 260),
        PUSH_ZERO(PrimPushZero.class, 261),
        PUSH_ONE(PrimPushOne.class, 262),
        PUSH_TWO(PrimPushTwo.class, 262),
        //
        TEST(PrimPrintArgs.class, 255),
        //
        LAST(PrimitiveNode.class, PRIM_COUNT);

        public int index;

        Primitives(Class<? extends PrimitiveNode> cls, int idx) {
            index = idx;
            primitiveClasses[idx] = cls;
        }
    }

    static {
        // Forces instantiaton of the primitives enum
        Primitives.values();
    }

    public static SqueakNode arg(int index) {
        return new ArgumentNode(index);
    }

    private static PrimitiveNode createInstance(CompiledMethodObject method, Class<? extends PrimitiveNode> primClass) {
        try {
            int argCount = (int) Arrays.stream(primClass.getDeclaredFields())
                                       .filter(f -> f.getAnnotation(Child.class) != null)
                                       .count();

            Class<?>[] argTypes = new Class<?>[argCount + 1];
            argTypes[0] = CompiledMethodObject.class;
            for (int i = 1; i <= argCount; i++) {
                argTypes[i] = SqueakNode.class;
            }

            Object[] args = new Object[argCount + 1];
            args[0] = method;
            for (int i = 1; i <= argCount; i++) {
                args[i] = arg(i - 1);
            }

            try {
                Method factoryMethod = primClass.getMethod("create", argTypes);
                return (PrimitiveNode) factoryMethod.invoke(null, args);
            } catch (NoSuchMethodException e) {
                return primClass.getConstructor(CompiledMethodObject.class).newInstance(method);
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

    public static PrimitiveNode forIdx(CompiledCodeObject method, int primitiveIdx) {
        if (method instanceof CompiledMethodObject) {
            return forIdx((CompiledMethodObject) method, primitiveIdx);
        } else {
            throw new RuntimeException("Primitives only supported in CompiledMethodObject");
        }
    }

    public static PrimitiveNode forIdx(CompiledMethodObject method, int primitiveIdx) {
        if (primitiveIdx >= primitiveClasses.length) { return new PrimitiveNode(method); }
        if (primitiveIdx >= 264
            && primitiveIdx <= 520) { return new PrimQuickReturnReceiverVariableNode(method, primitiveIdx - 264); }
        Class<? extends PrimitiveNode> primClass = primitiveClasses[primitiveIdx];
        if (primClass == null) {
            return new PrimitiveNode(method);
        } else {
            return createInstance(method, primClass);
        }
    }
}
