package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public abstract class PrimNewArg extends PrimitiveBinaryOperation {
    final static int NEW_CACHE_SIZE = 3;

    public PrimNewArg(CompiledMethodObject code) {
        super(code);
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
    BaseSqueakObject newWithArgDirect(ClassObject receiver, int size,
                    @Cached("receiver") ClassObject cachedReceiver,
                    @Cached("cachedReceiver.getClassFormatStable()") Assumption classFormatStable) {
        if (size == 0 || !cachedReceiver.isVariable())
            throw new PrimitiveFailed();
        return cachedReceiver.newInstance(size);
    }

    @Specialization(replaces = "newWithArgDirect")
    BaseSqueakObject newWithArg(ClassObject receiver, int size) {
        if (size == 0)
            return null;
        if (!receiver.isVariable())
            return null;
        return receiver.newInstance(size);
    }
}
