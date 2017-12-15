package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.util.SqueakBytecodeDecoder;

public class BytecodeSequenceNode extends Node {
    @Children private final SqueakBytecodeNode[] bytecodeNodes;

    public BytecodeSequenceNode(CompiledCodeObject code) {
        super();
        bytecodeNodes = new SqueakBytecodeDecoder(code).decode();
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
}