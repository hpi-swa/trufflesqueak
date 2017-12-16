package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.FrameStackWriteNode;

public class AdjustStackNode extends AbstractStackNode {
    @Child private FrameStackWriteNode writeNode;
    @CompilationFinal private int offset;

    public AdjustStackNode(CompiledCodeObject code, int offset) {
        super(code);
        this.offset = offset;
        writeNode = FrameStackWriteNode.create();
    }

    public void executeVoid(VirtualFrame frame) {
        frame.setInt(code.stackPointerSlot, stackPointer(frame) + offset);
    }
}
