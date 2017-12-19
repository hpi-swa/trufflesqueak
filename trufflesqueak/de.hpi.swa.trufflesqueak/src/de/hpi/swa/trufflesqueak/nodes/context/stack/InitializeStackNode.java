package de.hpi.swa.trufflesqueak.nodes.context.stack;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackWriteNode;

public class InitializeStackNode extends AbstractStackNode {
    @Child private FrameStackWriteNode writeNode;
    @CompilationFinal private final int numTemps;

    public InitializeStackNode(CompiledCodeObject code, int numTemps) {
        super(code);
        this.numTemps = numTemps;
        writeNode = FrameStackWriteNode.create();
    }

    @ExplodeLoop
    public void executeVoid(VirtualFrame frame) {
        Object[] arguments = frame.getArguments();
        for (int i = 0; i < arguments.length; i++) {
            writeNode.execute(frame, i, arguments[i]);
        }
        frame.setInt(code.stackPointerSlot, arguments.length - 1 + numTemps);
    }
}
