package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class BytecodeSequenceNode extends Node {
    private final byte[] bytes;
    @Children final SqueakBytecodeNode[] children;

    public BytecodeSequenceNode(byte[] bc) {
        super();
        bytes = bc;
        children = new SqueakBytecodeNode[bc.length];
    }

    public void initialize(CompiledCodeObject code) {
        new SqueakBytecodeDecoder(bytes, code).decode(children);
    }

// @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.FULL_EXPLODE)
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

    public void setByte(int index, byte value) {
        bytes[index] = value;
    }

}
