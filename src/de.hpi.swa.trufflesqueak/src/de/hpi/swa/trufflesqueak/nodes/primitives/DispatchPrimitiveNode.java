/*
 * Copyright (c) 2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNodes.AbstractArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory.ArgumentsLocation;

public final class DispatchPrimitiveNode extends AbstractNode {
    @Child protected AbstractPrimitiveNode primitiveNode;
    @Children protected AbstractArgumentNode[] argumentNodes;

    private DispatchPrimitiveNode(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
        this.primitiveNode = primitiveNode;
        this.argumentNodes = argumentNodes;
    }

    public static DispatchPrimitiveNode create(final AbstractPrimitiveNode primitiveNode, final ArgumentsLocation location, final int numReceiverAndArguments) {
        return new DispatchPrimitiveNode(primitiveNode, createArgumentNodes(location, numReceiverAndArguments));
    }

    private static AbstractArgumentNode[] createArgumentNodes(final ArgumentsLocation location, final int numReceiverAndArguments) {
        final AbstractArgumentNode[] argumentNodes = new AbstractArgumentNode[numReceiverAndArguments];
        final boolean useStack = location == ArgumentsLocation.ON_STACK || location == ArgumentsLocation.ON_STACK_REVERSED;
        final int offset = location == ArgumentsLocation.ON_STACK_REVERSED ? numReceiverAndArguments : 0;
        for (int i = 0; i < numReceiverAndArguments; i++) {
            argumentNodes[i] = AbstractArgumentNode.create(i - offset, useStack);
        }
        return argumentNodes;
    }

    @ExplodeLoop
    public Object execute(final VirtualFrame frame) {
        final int numArguments = argumentNodes.length;
        final Object[] arguments = new Object[numArguments];
        for (int i = 0; i < numArguments; i++) {
            arguments[i] = argumentNodes[i].execute(frame);
        }
        return primitiveNode.executeWithArguments(frame, arguments);
    }
}
