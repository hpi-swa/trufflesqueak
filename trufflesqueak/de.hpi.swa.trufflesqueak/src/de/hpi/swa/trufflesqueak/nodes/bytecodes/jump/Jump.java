package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakBytecodeNode;

public abstract class Jump extends SqueakBytecodeNode {
    public Jump(CompiledMethodObject cm, int idx) {
        super(cm, idx);
    }

    @Override
    public int execute(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, LocalReturn, ProcessSwitch {
        super.execute(frame);
        return getTargetPC();
    }

    public abstract int getTargetPC();
}