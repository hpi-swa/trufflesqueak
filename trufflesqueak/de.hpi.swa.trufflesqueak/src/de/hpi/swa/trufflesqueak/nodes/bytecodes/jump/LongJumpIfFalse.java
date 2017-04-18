package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class LongJumpIfFalse extends ConditionalJump {

    public LongJumpIfFalse(CompiledMethodObject compiledMethodObject, int b, byte c) {
        super(compiledMethodObject);
        // TODO Auto-generated constructor stub
    }

    @Override
    public BaseSqueakObject executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int getTargetPC() {
        // TODO Auto-generated method stub
        return 0;
    }

}
