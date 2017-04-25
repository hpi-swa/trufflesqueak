package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class PushRemoteTemp extends RemoteTempBytecode {
    public PushRemoteTemp(CompiledMethodObject compiledMethodObject, int idx, int indexInArray, int indexOfArray) {
        super(compiledMethodObject, idx, indexInArray, indexOfArray);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        BaseSqueakObject temp = getTempArray(frame);
        BaseSqueakObject at0 = temp.at0(indexInArray);
        push(frame, at0);
        return at0;
    }
}
