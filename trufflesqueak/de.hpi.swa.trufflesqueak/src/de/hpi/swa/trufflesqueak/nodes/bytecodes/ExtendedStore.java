package de.hpi.swa.trufflesqueak.nodes.bytecodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakExecutionNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.context.Top;
import de.hpi.swa.trufflesqueak.nodes.context.TopSqueakObject;

public class ExtendedStore extends ExtendedAccess {
    @Child private SqueakExecutionNode topNode;

    public ExtendedStore(CompiledMethodObject compiledMethodObject, int idx, int i) {
        super(compiledMethodObject, idx, i);
        if (type == 1) {
            topNode = new Top(compiledMethodObject, 0);
        } else {
            topNode = TopSqueakObject.create(compiledMethodObject, 0);
        }
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) throws NonLocalReturn, NonVirtualReturn, ProcessSwitch, LocalReturn {
        Object top = topNode.executeGeneric(frame);
        switch (type) {
            case 0:
                try {
                    SqueakTypesGen.expectBaseSqueakObject(getReceiver(frame)).atput0(
                                    storeIdx, SqueakTypesGen.expectBaseSqueakObject(top));
                } catch (UnwrappingError | UnexpectedResultException e) {
                    throw new RuntimeException("illegal ExtendedStore bytecode: unwrapping error", e);
                }
                break;
            case 1:
                setTemp(frame, storeIdx, top);
                break;
            case 2:
                throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2");
            case 3:
                BaseSqueakObject assoc = getMethod().getLiteral(storeIdx);
                try {
                    assoc.atput0(1, SqueakTypesGen.expectBaseSqueakObject(top));
                } catch (UnwrappingError | UnexpectedResultException e) {
                    throw new RuntimeException("illegal ExtendedStore bytecode: variable type 2", e);
                }
                break;
        }
        return top;
    }
}
