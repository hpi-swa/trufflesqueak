/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class FrameStackPopNode extends AbstractNode {
    @CompilationFinal private int stackPointer = -1;

    @Child private FrameStackReadNode readNode;

    @NeverDefault
    public static FrameStackPopNode create() {
        return new FrameStackPopNode();
    }

    public Object execute(final VirtualFrame frame) {
        if (stackPointer == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            stackPointer = FrameAccess.getStackPointer(frame) - 1;
            readNode = insert(FrameStackReadNode.create(frame, stackPointer, true));
        }
        FrameAccess.setStackPointer(frame, stackPointer);
        return readNode.executeRead(frame);
    }
}
