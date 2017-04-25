package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.PushReceiver;

public abstract class PrimitiveNodeFactory {
    private static final int PRIM_COUNT = 574;
    @SuppressWarnings("unchecked") private static Class<? extends PrimitiveNode>[] primitiveClasses = new Class[PRIM_COUNT + 1];

    public static enum Primitives {
        ADD(PrimAdd.class, 1),
        SUB(PrimSub.class, 2),
        LESSTHAN(PrimLessThan.class, 3),
        GREATERTHAN(PrimGreaterThan.class, 4),
        LESSOREQUAL(PrimLessOrEqual.class, 5),
        GREATEROREQUAL(PrimGreaterOrEqual.class, 6),
        EQUAL(PrimEqual.class, 7),
        NOTEQUAL(PrimNotEqual.class, 8),
        MULTIPLY(PrimMul.class, 9),
        DIVIDE(PrimDivide.class, 10),
        MOD(PrimMod.class, 11),
        DIV(PrimDiv.class, 12),
        //
        BIT_AND(PrimBitAnd.class, 14),
        BIT_OR(PrimBitOr.class, 15),
        //
        BIT_SHIFT(PrimBitShift.class, 17),
        MAKE_POINT(PrimMakePoint.class, 18),
        //
        AT(PrimAt.class, 60),
        AT_PUT(PrimAtPut.class, 61),
        SIZE(PrimSize.class, 62),
        //
        NEXT(PrimitiveNode.class, 65),
        NEXT_PUT(PrimitiveNode.class, 66),
        AT_END(PrimitiveNode.class, 67),
        //
        NEW(PrimNew.class, 70),
        NEW_WITH_ARG(PrimNewArg.class, 71),
        //
        BLOCK_COPY(PrimitiveNode.class, 80),
        //
        EQUIVALENT(PrimEquivalent.class, 110),
        CLASS(PrimClass.class, 111),
        //
        CLOSURE_VALUE(PrimitiveNode.class, 201),
        CLOSURE_VALUE_WITH_ARG(PrimitiveNode.class, 202),
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

    public static PrimitiveNode forIdx(CompiledMethodObject method, int primitiveIdx) {
        if (primitiveIdx >= primitiveClasses.length) {
            return new PrimitiveNode(method);
        }
        Class<? extends PrimitiveNode> primClass = primitiveClasses[primitiveIdx];
        if (primClass == null) {
            return new PrimitiveNode(method);
        } else {
            // FIXME: clean this up
            try {
                try {
                    return (PrimitiveNode) primClass.getMethod("create", CompiledMethodObject.class).invoke(primClass, method);
                } catch (NoSuchMethodException e) {
                    return primClass.getConstructor(CompiledMethodObject.class).newInstance(method);
                }
            } catch (NoSuchMethodException | InstantiationException | SecurityException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException("Internal error in creating primitive", e);
            }
        }
    }
}
