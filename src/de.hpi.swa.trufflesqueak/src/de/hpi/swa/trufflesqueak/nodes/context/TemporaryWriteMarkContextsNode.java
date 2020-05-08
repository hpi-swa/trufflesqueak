/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public final class TemporaryWriteMarkContextsNode extends AbstractNode {
    private final BranchProfile isContextObjectProfile = BranchProfile.create();
    @Child private FrameSlotWriteNode writeNode;

    protected TemporaryWriteMarkContextsNode(final FrameSlot stackSlot) {
        writeNode = FrameSlotWriteNode.create(stackSlot);
    }

    public static TemporaryWriteMarkContextsNode create(final VirtualFrame frame, final int tempIndex) {
        return new TemporaryWriteMarkContextsNode(FrameAccess.getStackSlot(frame, tempIndex));
    }

    public static TemporaryWriteMarkContextsNode create(final CompiledCodeObject code, final int tempIndex) {
        return new TemporaryWriteMarkContextsNode(code.getStackSlot(tempIndex));
    }

    public void executeWrite(final VirtualFrame frame, final Object value) {
        assert value != null : "Unexpected `null` value";
        if (value instanceof ContextObject) {
            isContextObjectProfile.enter();
            ((ContextObject) value).markEscaped();
        }
        writeNode.executeWrite(frame, value);
    }
}
