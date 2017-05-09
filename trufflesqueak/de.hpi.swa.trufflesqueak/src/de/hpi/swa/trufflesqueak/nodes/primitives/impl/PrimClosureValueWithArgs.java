package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveBinaryOperation;

public class PrimClosureValueWithArgs extends PrimitiveBinaryOperation {
    @Child private BlockActivationNode dispatch;

    public PrimClosureValueWithArgs(CompiledMethodObject cm) {
        super(cm);
        dispatch = BlockActivationNodeGen.create();
    }

    @Specialization
    protected Object value(BlockClosure block, PointersObject ary) {
        return dispatch.executeBlock(block, block.getFrameArguments(ary.getPointers()));
    }
}
