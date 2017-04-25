package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;

public class PushNewArray extends SqueakBytecodeNode {
    private final boolean popIntoArray;
    private final int arraySize;

    public PushNewArray(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx);
        arraySize = (i >> 1) & 0xFF;
        popIntoArray = (i & 1) == 1;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch {
        BaseSqueakObject[] ptrs = new BaseSqueakObject[arraySize];
        if (popIntoArray) {
            for (int i = 0; i < arraySize; i++) {
                ptrs[i] = (BaseSqueakObject) pop(frame); // FIXME
            }
        }
        PointersObject ary = new PointersObject(ptrs);
        ary.setSqClass(getMethod().getImage().arrayClass);
        push(frame, ary);
        return ary;
    }
}
