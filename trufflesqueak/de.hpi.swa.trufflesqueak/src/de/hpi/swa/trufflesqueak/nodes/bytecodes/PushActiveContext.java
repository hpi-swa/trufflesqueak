package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

public class PushActiveContext extends SqueakBytecodeNode {

    public PushActiveContext(CompiledMethodObject compiledMethodObject, int idx) {
        super(compiledMethodObject, idx);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        // TODO: ...
        MaterializedFrame materializedFrame = frame.materialize();
        ContextObject contextObject = new ContextObject(materializedFrame);
        push(frame, contextObject);
        return contextObject;
    }

}
