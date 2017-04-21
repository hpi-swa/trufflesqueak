package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;

public class PrimAdd extends PrimitiveBinaryOperation {
    public PrimAdd(CompiledMethodObject cm) {
        super(cm);
    }

    public static PrimitiveNode create(CompiledMethodObject cm) {
        return PrimAddNodeGen.create(cm, arg(cm, 0), arg(cm, 1));
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int add(int a, int b) {
        return Math.addExact(a, b);
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long add(long a, long b) {
        return Math.addExact(a, b);
    }

    @Specialization
    BigInteger add(BigInteger a, BigInteger b) {
        return a.add(b);
    }
}
