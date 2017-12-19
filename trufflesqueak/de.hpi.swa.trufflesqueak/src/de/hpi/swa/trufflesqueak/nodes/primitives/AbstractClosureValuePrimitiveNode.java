package de.hpi.swa.trufflesqueak.nodes.primitives;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;

public abstract class AbstractClosureValuePrimitiveNode extends AbstractPrimitiveNode {
    @Child protected BlockActivationNode dispatch;

    public AbstractClosureValuePrimitiveNode(CompiledMethodObject method) {
        super(method);
        dispatch = BlockActivationNodeGen.create();
    }
}