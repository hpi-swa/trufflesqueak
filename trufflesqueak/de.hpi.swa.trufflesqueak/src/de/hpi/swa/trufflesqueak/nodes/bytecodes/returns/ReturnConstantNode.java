package de.hpi.swa.trufflesqueak.nodes.bytecodes.returns;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;

public class ReturnConstantNode extends AbstractBytecodeNode {
    @CompilationFinal private final Object constant;

    public ReturnConstantNode(CompiledCodeObject code, int index, Object obj) {
        super(code, index);
        constant = obj;
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        throw new LocalReturn(constant);
    }

    @Override
    public String toString() {
        return "return: " + constant.toString();
    }
}
