package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveTernaryOperation;

public class PrimAtPut extends PrimitiveTernaryOperation {
    public PrimAtPut(CompiledMethodObject cm) {
        super(cm);
    }

    @Specialization
    protected char atput(LargeInteger receiver, int idx, char value) {
        receiver.atput0(idx - 1, value);
        return value;
    }

    @Specialization
    protected int atput(LargeInteger receiver, int idx, int value) {
        receiver.atput0(idx - 1, value);
        return value;
    }

    @Specialization
    protected char atput(NativeObject receiver, int idx, char value) {
        receiver.setNativeAt0(idx - 1, value);
        return value;
    }

    @Specialization
    protected int atput(NativeObject receiver, int idx, int value) {
        receiver.setNativeAt0(idx - 1, value);
        return value;
    }

    @Specialization
    protected long atput(NativeObject receiver, int idx, long value) {
        receiver.setNativeAt0(idx - 1, value);
        return value;
    }

    @Specialization
    protected Object atput(BlockClosure receiver, int idx, Object value) {
        receiver.atput0(idx - 1, value);
        return value;
    }

    @Specialization
    protected Object atput(ClassObject receiver, int idx, Object value) {
        receiver.atput0(idx - 1, value);
        return value;
    }

    @Specialization
    protected Object atput(CompiledCodeObject receiver, int idx, Object value) {
        receiver.atput0(idx - 1, value);
        return value;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected Object atput(EmptyObject receiver, int idx, Object value) {
        throw new PrimitiveFailed();
    }

    @Specialization
    protected Object atput(AbstractPointersObject receiver, int idx, Object value) {
        receiver.atput0(idx - 1, value);
        return value;
    }

    @Specialization
    protected Object atput(BaseSqueakObject receiver, int idx, Object value) {
        receiver.atput0(idx - 1, value);
        return value;
    }
}
