/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class FrameStackPopNNode extends AbstractNodeWithCode {
    protected final int numPop;
    @CompilationFinal private int stackPointer = -1;

    @Children private FrameSlotReadNode[] readNodes;

    protected FrameStackPopNNode(final CompiledCodeObject code, final int numPop) {
        super(code);
        this.numPop = numPop;
        readNodes = numPop == 0 ? null : new FrameSlotReadNode[numPop];
    }

    public static FrameStackPopNNode create(final CompiledCodeObject code, final int numPop) {
        return FrameStackPopNNodeGen.create(code, numPop);
    }

    public final Object[] execute(final VirtualFrame frame) {
        if (numPop > 0) {
            if (stackPointer == -1) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                stackPointer = FrameAccess.getStackPointer(frame, code) - numPop;
                assert stackPointer >= 0 : "Bad stack pointer";
            }
            FrameAccess.setStackPointer(frame, code, stackPointer);
        }
        return executeSpecialized(frame);
    }

    protected abstract Object[] executeSpecialized(VirtualFrame frame);

    @Specialization(guards = {"numPop == 0"})
    protected static final Object[] doNone() {
        return ArrayUtils.EMPTY_ARRAY;
    }

    @Specialization(guards = {"numPop == 1"})
    protected final Object[] doSingle(final VirtualFrame frame) {
        return new Object[]{getReadNode(0).executeRead(frame)};
    }

    @ExplodeLoop
    @Specialization(guards = {"numPop > 1"})
    protected final Object[] doMultiple(final VirtualFrame frame) {
        final Object[] result = new Object[numPop];
        for (int i = 0; i < numPop; i++) {
            result[i] = getReadNode(i).executeRead(frame);
        }
        return result;
    }

    protected final FrameSlotReadNode getReadNode(final int offset) {
        if (readNodes[offset] == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            readNodes[offset] = insert(FrameSlotReadNode.create(code, stackPointer + offset));
        }
        return readNodes[offset];
    }
}
