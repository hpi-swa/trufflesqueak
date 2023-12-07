/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.HandlePrimitiveFailedNodeFactory.HandlePrimitiveFailedImplNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class HandlePrimitiveFailedNode extends AbstractNode {
    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        if (code.hasStoreIntoTemp1AfterCallPrimitive()) {
            return HandlePrimitiveFailedImplNodeGen.create();
        } else {
            return HandlePrimitiveFailedNoopNode.SINGLETON;
        }
    }

    public abstract void executeHandle(VirtualFrame frame, int reasonCode);

    protected abstract static class HandlePrimitiveFailedImplNode extends HandlePrimitiveFailedNode {
        @Specialization(guards = {"reasonCode < sizeNode.execute(node, getContext().primitiveErrorTable)"})
        protected static final void doHandleWithLookup(final VirtualFrame frame, final int reasonCode,
                        @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final ArrayObjectReadNode readNode,
                        @Cached("createStackTopNode(frame)") final FrameStackWriteNode tempWriteNode) {
            tempWriteNode.executeWrite(frame, readNode.execute(node, getContext(node).primitiveErrorTable, reasonCode));
        }

        @Specialization(guards = {"reasonCode >= sizeNode.execute(node, getContext().primitiveErrorTable)"})
        protected static final void doHandleRawValue(final VirtualFrame frame, final int reasonCode,
                        @SuppressWarnings("unused") @Bind("this") final Node node,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached("createStackTopNode(frame)") final FrameStackWriteNode tempWriteNode) {
            tempWriteNode.executeWrite(frame, (long) reasonCode);
        }

        @NeverDefault
        protected static final FrameStackWriteNode createStackTopNode(final VirtualFrame frame) {
            final int stackPointer = FrameAccess.getStackPointer(frame) - 1;
            return FrameStackWriteNode.create(frame, stackPointer);
        }
    }

    @DenyReplace
    @NodeInfo(cost = NodeCost.NONE)
    private static final class HandlePrimitiveFailedNoopNode extends HandlePrimitiveFailedNode {
        private static final HandlePrimitiveFailedNoopNode SINGLETON = new HandlePrimitiveFailedNoopNode();

        @Override
        public void executeHandle(final VirtualFrame frame, final int reasonCode) {
            // nothing to do
        }

        @Override
        public boolean isAdoptable() {
            return false;
        }

        @Override
        public Node copy() {
            return SINGLETON;
        }

        @Override
        public Node deepCopy() {
            return SINGLETON;
        }
    }
}
