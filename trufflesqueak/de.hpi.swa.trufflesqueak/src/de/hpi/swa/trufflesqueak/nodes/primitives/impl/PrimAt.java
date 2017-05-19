package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimAt extends PrimitiveBinaryOperation {
    public PrimAt(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected long intAt(BigInteger receiver, int idx) {
        return LargeInteger.byteAt0(receiver, idx - 1);
    }

    @Specialization
    protected long intAt(NativeObject receiver, int idx) {
        return receiver.getNativeAt0(idx - 1);
    }

    @Specialization(rewriteOn = {UnwrappingError.class, ArithmeticException.class})
    protected int intAt(BaseSqueakObject receiver, int idx) throws UnwrappingError {
        return safeObject(receiver, idx - 1).unwrapInt();
    }

    @Specialization(rewriteOn = {UnwrappingError.class, ArithmeticException.class})
    protected long longAt(BaseSqueakObject receiver, int idx) throws UnwrappingError {
        return safeObject(receiver, idx - 1).unwrapLong();
    }

    @Specialization(rewriteOn = {UnwrappingError.class})
    protected BigInteger bigIntAt(BaseSqueakObject receiver, int idx) throws UnwrappingError {
        return safeObject(receiver, idx - 1).unwrapBigInt();
    }

    private static BaseSqueakObject safeObject(BaseSqueakObject o, int idx) throws UnwrappingError {
        BaseSqueakObject at0 = o.at0(idx);
        if (at0 == null) {
            throw new UnwrappingError();
        }
        return at0;
    }

    @Specialization
    protected BaseSqueakObject at(BaseSqueakObject receiver, int idx) {
        return receiver.at0(idx - 1);
    }
}
