package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveQuinaryOperation;

public class PrimReplaceFromToNode extends PrimitiveQuinaryOperation {
    public PrimReplaceFromToNode(CompiledMethodObject cm) {
        super(cm);
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
        // Squeak LargeIntegers are unsigned
        byte[] bytes = repl.abs().toByteArray();
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
