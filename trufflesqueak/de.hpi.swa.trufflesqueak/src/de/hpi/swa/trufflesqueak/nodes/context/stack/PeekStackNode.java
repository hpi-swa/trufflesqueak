package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackReadNode;

public class PeekStackNode extends AbstractStackNode {
    @Child private FrameStackReadNode readNode;
    @CompilationFinal private final int offset;

    public PeekStackNode(CompiledCodeObject code, int offset) {
        super(code);
        this.offset = offset;
        readNode = FrameStackReadNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        int sp = stackPointer(frame);
        return readNode.execute(frame, sp - offset);
    }

}
