package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.util.Decoder;

public class BytecodeSequenceNode extends Node {
    private final CompiledCodeObject code;
    private final byte[] bytes;
    @Children final SqueakBytecodeNode[] children;

    public BytecodeSequenceNode(byte[] bc, CompiledCodeObject co) {
        super();
        code = co;
        bytes = bc;
        children = new SqueakBytecodeNode[bc.length];
    }

    public void initialize() {
        new Decoder(bytes, code).decode(children);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE_UNTIL_RETURN)
    public Object executeGeneric(VirtualFrame frame) {
        int pc = 0;
        while (pc >= 0 && pc < children.length) {
            pc = children[pc].executeInt(frame);
        }
        throw new RuntimeException("Method did not return");
    }

    public byte[] getBytes() {
        return bytes;
    }

}
