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

    public CallPrimitive(CompiledMethodObject compiledMethodObject, int idx, byte b, byte c) {
        super(compiledMethodObject, idx);
        int primitiveIdx = b + (c << 8);
        primitive = PrimitiveNode.forIdx(compiledMethodObject, primitiveIdx);
    }

    @Override
    public void executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch, LocalReturn {
        primitive.executeGeneric(frame);
    }

}
