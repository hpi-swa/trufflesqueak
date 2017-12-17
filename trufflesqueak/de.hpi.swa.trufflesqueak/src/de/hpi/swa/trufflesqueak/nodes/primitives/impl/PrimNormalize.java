package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.returns.ReturnReceiverNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimNormalize extends PrimitiveNodeUnary {
    public PrimNormalize(CompiledMethodObject code) {
        super(code);
    }

    public static PrimNormalize create(CompiledMethodObject code) {
        return PrimNormalizeNodeGen.create(code, new ReturnReceiverNode(code, -1));
    }

    @Specialization
    int normalizeInt(int o) {
        return o;
    }

    @Specialization
    long normalizeLong(long o) {
        return o;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int normalizeInt(BigInteger o) {
        return o.intValueExact();
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long normalizeLong(BigInteger o) {
        return o.longValueExact();
    }

    @Specialization
    BigInteger normalizeBig(BigInteger o) {
        return o;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    int normalizeInt(NativeObject o) {
        return bigIntFromNative(o).intValueExact();
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    long normalizeLong(NativeObject o) {
        return bigIntFromNative(o).longValueExact();
    }

    @Specialization
    BigInteger normalizeBig(NativeObject o) {
        return bigIntFromNative(o);
    }

    private BigInteger bigIntFromNative(NativeObject o) {
        return new LargeInteger(code.image, o.getSqClass(), o.getBytes()).getValue();
    }
}
