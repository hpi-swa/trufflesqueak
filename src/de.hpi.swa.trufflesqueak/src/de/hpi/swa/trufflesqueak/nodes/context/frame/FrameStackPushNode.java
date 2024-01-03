/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@NodeInfo(cost = NodeCost.NONE)
public final class FrameStackPushNode extends AbstractNode {
    @CompilationFinal private int stackPointer = -1;

    @Child private FrameStackWriteNode writeNode;

    @NeverDefault
    public static FrameStackPushNode create() {
        return new FrameStackPushNode();
    }

    public void execute(final VirtualFrame frame, final Object value) {
        if (stackPointer == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame) + 1;
            assert stackPointer <= CONTEXT.MAX_STACK_SIZE : "Bad stack pointer";
            writeNode = insert(FrameStackWriteNode.create(frame, stackPointer - 1));
        }
        FrameAccess.setStackPointer(frame, stackPointer);
        writeNode.executeWrite(frame, value);
    }
}
