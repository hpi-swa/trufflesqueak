package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;

public class UnconditionalJumpNode extends SqueakBytecodeNode {

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int bytecode) {
        super(code, index + AbstractJump.shortJumpOffset(bytecode));
    }

    public UnconditionalJumpNode(CompiledCodeObject code, int index, int bytecode, int parameter) {
        super(code, index + AbstractJump.longUnconditionalJumpOffset(bytecode, parameter));
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return code.image.nil; // Do nothing
    }
}
