package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public class CallPrimitive extends SqueakBytecodeNode {
    @Child private PrimitiveNode primitive;

    public CallPrimitive(CompiledMethodObject compiledMethodObject, int idx, int i, int j) {
        super(compiledMethodObject, idx);
        int primitiveIdx = i + (j << 8);
        primitive = PrimitiveNode.forIdx(compiledMethodObject, primitiveIdx, idx);
    }

    @Override
    public void executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch, LocalReturn {
        primitive.executeGeneric(frame);
    }

}
