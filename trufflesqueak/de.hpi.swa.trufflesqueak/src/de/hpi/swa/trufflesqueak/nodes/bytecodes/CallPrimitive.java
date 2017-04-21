package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

public class CallPrimitive extends SqueakBytecodeNode {
    @Child private PrimitiveNode primitive;

    public CallPrimitive(CompiledMethodObject compiledMethodObject, int idx, int i, int j) {
        super(compiledMethodObject, idx);
        int primitiveIdx = i + (j << 8);
        primitive = PrimitiveNodeFactory.forIdx(compiledMethodObject, primitiveIdx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch, LocalReturn {
        Object result = primitive.executeGeneric(frame);
        if (result == null) {
            return null;
        } else {
            assert getIndex() == 0; // TODO: don't assume that this is a primitive method
            throw new LocalReturn(result);
        }
    }

}
