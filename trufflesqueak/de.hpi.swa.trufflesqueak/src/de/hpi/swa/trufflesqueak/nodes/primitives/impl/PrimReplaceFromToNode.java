package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuinaryOperation;

public class PrimReplaceFromToNode extends PrimitiveQuinaryOperation {
    public PrimReplaceFromToNode(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    Object replace(LargeInteger rcvr, int start, int stop, LargeInteger repl, int replStart) {
        return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
    }

    @Specialization
    Object replace(LargeInteger rcvr, int start, int stop, NativeObject repl, int replStart) {
        return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
    }

    @Specialization
    Object replace(LargeInteger rcvr, int start, int stop, BigInteger repl, int replStart) {
        return replaceInLarge(rcvr, start, stop, LargeInteger.getSqueakBytes(repl), replStart);
    }

    private static Object replaceInLarge(LargeInteger rcvr, int start, int stop, byte[] replBytes, int replStart) {
        byte[] rcvrBytes = rcvr.getBytes();
        int repOff = replStart - start;
        for (int i = start - 1; i < stop; i++) {
            rcvrBytes[i] = replBytes[repOff + i];
        }
        rcvr.setBytes(rcvrBytes);
        return rcvr;
    }

    @Specialization
    Object replace(NativeObject rcvr, int start, int stop, LargeInteger repl, int replStart) {
        int repOff = replStart - start;
        byte[] replBytes = repl.getBytes();
        for (int i = start - 1; i < stop; i++) {
            rcvr.setNativeAt0(i, replBytes[repOff + i]);
        }
        return rcvr;
    }

    @Specialization
    Object replace(NativeObject rcvr, int start, int stop, NativeObject repl, int replStart) {
        int repOff = replStart - start;
        for (int i = start - 1; i < stop; i++) {
            rcvr.setNativeAt0(i, repl.getNativeAt0(repOff + i));
        }
        return rcvr;
    }

    @Specialization
    Object replace(NativeObject rcvr, int start, int stop, BigInteger repl, int replStart) {
        int repOff = replStart - start;
        byte[] bytes = LargeInteger.getSqueakBytes(repl);
        for (int i = start - 1; i < stop; i++) {
            rcvr.setNativeAt0(i, bytes[repOff + i]);
        }
        return rcvr;
    }

    @Specialization
    Object replace(ListObject rcvr, int start, int stop, ListObject repl, int replStart) {
        int repOff = replStart - start;
        for (int i = start - 1; i < stop; i++) {
            rcvr.atput0(i, repl.at0(repOff + i));
        }
        return rcvr;
    }
}
