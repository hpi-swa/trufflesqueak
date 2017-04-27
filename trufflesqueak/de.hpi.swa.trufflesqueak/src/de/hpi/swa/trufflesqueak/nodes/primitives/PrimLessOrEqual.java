package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PrimLessOrEqual extends PrimitiveBinaryOperation {
    protected PrimLessOrEqual(CompiledMethodObject cm) {
        super(cm);
    }

    public static PrimitiveNode create(CompiledMethodObject cm) {
        return PrimLessOrEqualNodeGen.create(cm, arg(cm, 0), arg(cm, 1));
    }

    @Specialization
    boolean add(int a, int b) {
        return a <= b;
    }

    @Specialization
    boolean add(long a, long b) {
        return a <= b;
    }

    @Specialization
    boolean add(BigInteger a, BigInteger b) {
        return a.compareTo(b) <= 0;
    }
}
