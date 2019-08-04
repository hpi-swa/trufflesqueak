package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithCode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class FrameStackInitializationNode extends AbstractNodeWithCode {
    @Children private FrameSlotWriteNode[] writeNodes;

    protected FrameStackInitializationNode(final CompiledCodeObject code) {
        super(code);
        final int initialSP = code.getNumArgsAndCopied() + code.getNumTemps() - code.getNumArgs();
        writeNodes = new FrameSlotWriteNode[initialSP];
        for (int i = 0; i < writeNodes.length; i++) {
            writeNodes[i] = insert(FrameSlotWriteNode.create(code.getStackSlot(i)));
        }
    }

    public static FrameStackInitializationNode create(final CompiledCodeObject code) {
        return new FrameStackInitializationNode(code);
    }

    @ExplodeLoop
    public void executeInitialize(final VirtualFrame frame) {
        CompilerDirectives.isCompilationConstant(writeNodes.length);
        final Object[] arguments = frame.getArguments();
        final int numArgs = code.getNumArgsAndCopied();
        CompilerDirectives.isCompilationConstant(numArgs);
        assert arguments.length == FrameAccess.expectedArgumentSize(numArgs);
        for (int i = 0; i < numArgs; i++) {
            writeNodes[i].executeWrite(frame, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        for (int i = 0; i < writeNodes.length - numArgs; i++) {
            writeNodes[numArgs + i].executeWrite(frame, NilObject.SINGLETON);
        }
        FrameAccess.setStackPointer(frame, code, writeNodes.length);
    }
}
