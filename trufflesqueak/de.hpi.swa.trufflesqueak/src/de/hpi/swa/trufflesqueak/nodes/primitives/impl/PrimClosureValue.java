package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveUnaryOperation;

public class PrimClosureValue extends PrimitiveUnaryOperation {
    @Child private BlockActivationNode dispatch;

    public PrimClosureValue(CompiledMethodObject cm) {
        super(cm);
        dispatch = BlockActivationNodeGen.create();
    }

    @Specialization
    protected Object value(BlockClosure block) {
        return dispatch.executeBlock(block, new Object[0]);
    }
}
