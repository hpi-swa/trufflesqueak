package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimAt extends PrimitiveBinaryOperation {
    public PrimAt(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    protected int at(char receiver, int idx) {
        if (idx == 1) {
            return receiver;
        } else {
            throw new PrimitiveFailed();
        }
    }

    @Specialization
    protected Object at(LargeInteger receiver, int idx) {
        return receiver.at0(idx - 1);
    }

    @Specialization
    protected long intAt(BigInteger receiver, int idx) {
        return LargeInteger.byteAt0(receiver, idx - 1);
    }

    @Specialization
    protected long at(double receiver, int idx) {
        long doubleBits = Double.doubleToLongBits(receiver);
        if (idx == 1) {
            return 0xFFFFFFFF & (doubleBits >> 32);
        } else if (idx == 2) {
            return 0xFFFFFFFF & doubleBits;
        } else {
            throw new PrimitiveFailed();
        }
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    protected int intAt(NativeObject receiver, int idx) throws ArithmeticException {
        return Math.toIntExact(receiver.getNativeAt0(idx - 1));
    }

    @Specialization
    protected long longAt(NativeObject receiver, int idx) {
        return receiver.getNativeAt0(idx - 1);
    }

    @Specialization
    protected Object at(BlockClosure receiver, int idx) {
        return receiver.at0(idx - 1);
    }

    @Specialization
    protected Object at(CompiledCodeObject receiver, int idx) {
        return receiver.at0(idx - 1);
    }

    @Specialization
    protected Object at(EmptyObject receiver, int idx) {
        return receiver.at0(idx - 1);
    }

    @Specialization
    protected Object at(AbstractPointersObject receiver, int idx) {
        return receiver.at0(idx - 1);
    }

    @Specialization
    protected Object at(BaseSqueakObject receiver, int idx) {
        return receiver.at0(idx - 1);
    }
}
