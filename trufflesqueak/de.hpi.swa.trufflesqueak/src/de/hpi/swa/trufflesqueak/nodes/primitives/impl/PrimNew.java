package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimNew extends PrimitiveUnaryOperation {
    final static int NEW_CACHE_SIZE = 3;

    public PrimNew(CompiledMethodObject code) {
        super(code);
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "NEW_CACHE_SIZE", guards = {"receiver == cachedReceiver"}, assumptions = {"classFormatStable"})
    BaseSqueakObject newDirect(ClassObject receiver,
                    @Cached("receiver") ClassObject cachedReceiver,
                    @Cached("cachedReceiver.getClassFormatStable()") Assumption classFormatStable) {
        return cachedReceiver.newInstance();
    }

    @Specialization(replaces = "newDirect")
    BaseSqueakObject newIndirect(ClassObject receiver) {
        return receiver.newInstance();
    }
}
