/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

@NodeInfo(cost = NodeCost.NONE)
public final class TemporaryWriteMarkContextsNode extends AbstractNode {
    private final BranchProfile isContextObjectProfile = BranchProfile.create();
    @Child private FrameStackWriteNode writeNode;

    protected TemporaryWriteMarkContextsNode(final FrameStackWriteNode writeNode) {
        this.writeNode = writeNode;
    }

    public static TemporaryWriteMarkContextsNode create(final VirtualFrame frame, final int tempIndex) {
        return new TemporaryWriteMarkContextsNode(FrameStackWriteNode.create(frame, tempIndex));
    }

    public void executeWrite(final VirtualFrame frame, final Object value) {
        assert value != null : "Unexpected `null` value";
        if (value instanceof final ContextObject context) {
            isContextObjectProfile.enter();
            context.markEscaped();
        }
        writeNode.executeWrite(frame, value);
    }
}
