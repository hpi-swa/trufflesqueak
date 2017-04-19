package de.hpi.swa.trufflesqueak.nodes;

import java.lang.reflect.InvocationTargetException;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAdd;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAt;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimAtPut;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBitAnd;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBitOr;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimBitShift;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimClass;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimDiv;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimDivide;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimEquivalent;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimGreaterOrEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimGreaterThan;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimLessOrEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimLessThan;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimMakePoint;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimMod;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimMul;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNew;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNewArg;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNotEqual;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimNotSupported;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimSize;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimSub;

public abstract class PrimitiveNode extends SqueakBytecodeNode {
    @SuppressWarnings("unchecked") private static Class<? extends PrimitiveNode>[] primitiveClasses = new Class[573];

    public enum Primitives {
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
        NEXT(PrimNotSupported.class, 65),
        NEXT_PUT(PrimNotSupported.class, 66),
        AT_END(PrimNotSupported.class, 67),
        //
        NEW(PrimNew.class, 70),
        NEW_WITH_ARG(PrimNewArg.class, 71),
        //
        BLOCK_COPY(PrimNotSupported.class, 80),
        //
        EQUIVALENT(PrimEquivalent.class, 110),
        CLASS(PrimClass.class, 111),
        //
        CLOSURE_VALUE(PrimNotSupported.class, 201),
        CLOSURE_VALUE_WITH_ARG(PrimNotSupported.class, 202),
        ;

        public int index;

        Primitives(Class<? extends PrimitiveNode> cls, int idx) {
            index = idx;
            primitiveClasses[idx] = cls;
        }
    }

    public PrimitiveNode(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    public static PrimitiveNode forIdx(CompiledMethodObject method, int idx) {
        Class<? extends PrimitiveNode> primClass = primitiveClasses[idx];
        if (primClass == null) {
            return new PrimNotSupported();
        } else {
            try {
                return primClass.getConstructor(CompiledMethodObject.class).newInstance(method);
            } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                throw new RuntimeException("Internal error in creating primitive", e);
            }
        }
    }
}
