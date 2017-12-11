package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Arrays;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchNodeGen;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNodeGen;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNode;

public class PrimPerform extends PrimitiveNode {
    @Child public SqueakNode receiverNode;
    @Child public SqueakNode selectorNode;
    @Child protected SqueakLookupClassNode lookupClassNode;
    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode;

    public PrimPerform(CompiledMethodObject code) {
        super(code);
        lookupClassNode = SqueakLookupClassNodeGen.create(code);
        dispatchNode = DispatchNodeGen.create();
        lookupNode = LookupNodeGen.create();
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        Object[] rcvrAndArgs = topN(frame, code.getNumArgs() + 1);
        ClassObject rcvrClass;
        try {
            rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(rcvrAndArgs[0]));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("receiver has no class");
        }
        Object selector = rcvrAndArgs[2];
        Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
        return dispatchNode.executeDispatch(lookupResult, rcvrAndArgs);
    }
}
