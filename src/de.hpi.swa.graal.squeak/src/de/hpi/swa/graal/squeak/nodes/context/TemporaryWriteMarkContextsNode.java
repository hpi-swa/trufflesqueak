/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class TemporaryWriteMarkContextsNode extends AbstractNodeWithCode {
    @Child private FrameSlotWriteNode writeNode;

    protected TemporaryWriteMarkContextsNode(final CompiledCodeObject code, final int tempIndex) {
        super(code);
        writeNode = FrameSlotWriteNode.create(code.getStackSlot(tempIndex));
    }

    public static TemporaryWriteMarkContextsNode create(final CompiledCodeObject code, final int tempIndex) {
        return TemporaryWriteMarkContextsNodeGen.create(code, tempIndex);
    }

    public abstract void executeWrite(VirtualFrame frame, Object value);

    @Specialization
    protected final void doWriteContext(final VirtualFrame frame, final ContextObject value) {
        assert value != null : "Unexpected `null` value";
        value.markEscaped();
        writeNode.executeWrite(frame, value);
    }

    @Specialization(guards = {"!isContextObject(value)"})
    protected final void doWriteOther(final VirtualFrame frame, final Object value) {
        assert value != null : "Unexpected `null` value";
        writeNode.executeWrite(frame, value);
    }
}
