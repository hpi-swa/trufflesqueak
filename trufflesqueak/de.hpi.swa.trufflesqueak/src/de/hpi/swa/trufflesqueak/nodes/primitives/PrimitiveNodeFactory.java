package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAddNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtPutNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitAndNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitOrNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitShiftNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClass;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClosureValueNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClosureValueWithArgsNodeGen;
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
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushFalse;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushMinusOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushNil;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushSelf;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushTrue;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushTwo;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushZero;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSubNodeGen;

public abstract class PrimitiveNodeFactory {
    private static final int PRIM_COUNT = 574;
    @SuppressWarnings("unchecked") private static Class<? extends PrimitiveNode>[] primitiveClasses = new Class[PRIM_COUNT + 1];

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
        //
        BIT_AND(PrimBitAndNodeGen.class, 14),
        BIT_OR(PrimBitOrNodeGen.class, 15),
        //
        BIT_SHIFT(PrimBitShiftNodeGen.class, 17),
        MAKE_POINT(PrimMakePoint.class, 18),
        //
        AT(PrimAtNodeGen.class, 60),
        AT_PUT(PrimAtPutNodeGen.class, 61),
        SIZE(PrimSizeNodeGen.class, 62),
        //
        NEXT(PrimitiveNode.class, 65),
        NEXT_PUT(PrimitiveNode.class, 66),
        AT_END(PrimitiveNode.class, 67),
        //
        NEW(PrimNewNodeGen.class, 70),
        NEW_WITH_ARG(PrimNewArgNodeGen.class, 71),
        //
        BLOCK_COPY(PrimitiveNode.class, 80),
        //
        EQUIVALENT(PrimEquivalentNodeGen.class, 110),
        CLASS(PrimClass.class, 111),
        //
        CLOSURE_VALUE(PrimClosureValueNodeGen.class, 201),
        CLOSURE_VALUE_WITH_ARG(PrimClosureValueWithArgsNodeGen.class, 202),
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

    private static SqueakNode arg(int index) {
        return new ArgumentNode(index);
    }

    private static PrimitiveNode createInstance(CompiledMethodObject method, Class<? extends PrimitiveNode> primClass) {
        try {
            int argCount = (int) Arrays.stream(primClass.getDeclaredFields()).filter(f -> f.getAnnotation(Child.class) != null).count();

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
        } catch (NoSuchMethodException | InstantiationException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw new RuntimeException("Internal error in creating primitive", e);
        }
    }

    public static PrimitiveNode forIdx(CompiledMethodObject method, int primitiveIdx) {
        if (primitiveIdx >= primitiveClasses.length) {
            return new PrimitiveNode(method);
        }
        Class<? extends PrimitiveNode> primClass = primitiveClasses[primitiveIdx];
        if (primClass == null) {
            return new PrimitiveNode(method);
        } else {
            return createInstance(method, primClass);
        }
    }
}
