package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;

public abstract class RemoteTempBytecode extends SqueakBytecodeNode {
    protected final int indexInArray;
    protected final int indexOfArray;

    public RemoteTempBytecode(CompiledMethodObject compiledMethodObject, int idx, int indexInArray, int indexOfArray) {
        super(compiledMethodObject, idx);
        this.indexInArray = indexInArray;
        this.indexOfArray = indexOfArray;
    }

    protected BaseSqueakObject getTempArray(VirtualFrame frame) {
        try {
            return SqueakTypesGen.expectPointersObject(getTemp(frame, indexOfArray));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("cannot store into non-pointers temp array");
        }
    }
}