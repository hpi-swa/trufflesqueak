package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimMod extends PrimitiveBinaryOperation {
    public PrimMod(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    long mod(long a, long b) {
        return a % b;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long mod(BigInteger a, BigInteger b) {
        return doBigModulo(a, b).longValueExact();
    }

    @Specialization
    BigInteger modBig(BigInteger a, BigInteger b) {
        return doBigModulo(a, b);
    }

    private static BigInteger doBigModulo(BigInteger a, BigInteger b) {
        BigInteger mod = a.mod(b.abs());
        if (a.signum() + b.signum() <= 0) {
            return mod.negate();
        } else {
            return mod;
        }
    }
}
