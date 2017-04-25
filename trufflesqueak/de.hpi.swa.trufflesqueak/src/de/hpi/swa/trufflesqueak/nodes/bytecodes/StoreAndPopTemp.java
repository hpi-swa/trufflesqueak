package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class StoreAndPopTemp extends SqueakBytecodeNode {
    /* TODO: use the frame specializations
     * @NodeField(name = "tempSlot", type = FrameSlot.class)
     * protected abstract FrameSlot getTempSlot();
     * @Specialization(guards = "isLongOrIllegal(frame)")
     * ...
     *    getTempSlot().setKind(FrameSlotKind.Long)
     * ...
     * 
     * 
     * @Specialization(containts = {"writeLong", "writeBoolean"})
     * ...
     *    setKind Object
     * ...
    */ 
    public StoreAndPopTemp(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx);
        // TODO Auto-generated constructor stub
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        // TODO Auto-generated method stub
        return null;
    }

}
