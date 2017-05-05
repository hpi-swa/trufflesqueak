package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAddNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAt;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimAtPut;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitAnd;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitOr;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimBitShift;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimClass;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDiv;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimDivide;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimEquivalentNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimGreaterOrEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimGreaterThan;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLessOrEqualNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimLessThan;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimMakePoint;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimMod;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimMul;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNew;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNewArgNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimNotEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushFalse;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushMinusOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushNil;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushOne;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushSelf;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushTrue;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushTwo;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimPushZero;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSizeNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.PrimSub;

public abstract class PrimitiveNodeFactory {
    private static final int PRIM_COUNT = 574;
    @SuppressWarnings("unchecked") private static Class<? extends PrimitiveNode>[] primitiveClasses = new Class[PRIM_COUNT + 1];

    public static enum Primitives {
        ADD(PrimAddNodeGen.class, 1),
        SUB(PrimSub.class, 2),
        LESSTHAN(PrimLessThan.class, 3),
        GREATERTHAN(PrimGreaterThan.class, 4),
        LESSOREQUAL(PrimLessOrEqualNodeGen.class, 5),
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
        SIZE(PrimSizeNodeGen.class, 62),
        //
        NEXT(PrimitiveNode.class, 65),
        NEXT_PUT(PrimitiveNode.class, 66),
        AT_END(PrimitiveNode.class, 67),
        //
        NEW(PrimNew.class, 70),
        NEW_WITH_ARG(PrimNewArgNodeGen.class, 71),
        //
        BLOCK_COPY(PrimitiveNode.class, 80),
        //
        EQUIVALENT(PrimEquivalentNodeGen.class, 110),
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

    private static PrimitiveNode createInstance(CompiledMethodObject method, Class<? extends PrimitiveNode> primClass) {
        try {
            try {
                return (PrimitiveNode) primClass.getMethod("createInstance", Class.class, CompiledMethodObject.class).invoke(primClass, primClass, method);
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
