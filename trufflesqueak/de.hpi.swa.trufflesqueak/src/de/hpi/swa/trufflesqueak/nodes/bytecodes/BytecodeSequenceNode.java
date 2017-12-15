package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class BytecodeSequenceNode extends Node {
    @CompilationFinal(dimensions = 1) private final byte[] bytes;
    @Children private final SqueakBytecodeNode[] bytecodeNodes;

    public BytecodeSequenceNode(byte[] bc) {
        super();
        bytes = bc;
        bytecodeNodes = new SqueakBytecodeNode[bc.length];
    }

    public void initialize(CompiledCodeObject code) {
        new SqueakBytecodeDecoder(bytes, code).decode(bytecodeNodes);
    }

    @ExplodeLoop(kind = ExplodeLoop.LoopExplosionKind.MERGE_EXPLODE)
    public Object executeGeneric(VirtualFrame frame) {
        int pc = 0;
        CompilerAsserts.compilationConstant(bytecodeNodes.length);
        while (pc >= 0 && pc < bytecodeNodes.length) {
            CompilerAsserts.partialEvaluationConstant(bytecodeNodes[pc]);
            pc = bytecodeNodes[pc].executeInt(frame);
        }
        CompilerDirectives.transferToInterpreter();
        throw new RuntimeException("Method did not return");
    }

    public byte[] getBytes() {
        return bytes;
    }

    public void setByte(int index, byte value) {
        bytes[index] = value;
    }

}
