package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;

public abstract class AbstractSendNode extends SqueakBytecodeNode {
    public final Object selector;
    public final int argumentCount;
    @Child protected SqueakLookupClassNode lookupClassNode;
    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode;

    public AbstractSendNode(CompiledCodeObject code, int idx, Object sel, int argcount) {
        super(code, idx);
        selector = sel;
        argumentCount = argcount;
        lookupClassNode = SqueakLookupClassNode.create(code);
        dispatchNode = DispatchNode.create();
        lookupNode = LookupNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, executeSend(frame));
        // TODO: Object as Method
    }

    @ExplodeLoop
    public Object executeSend(VirtualFrame frame) {
        Object[] rcvrAndArgs = popN(frame, argumentCount + 1);
        ClassObject rcvrClass;
        try {
            rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(rcvrAndArgs[0]));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("receiver has no class");
        }
        Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
        return dispatchNode.executeDispatch(lookupResult, rcvrAndArgs);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class)) && getSourceSection().isAvailable();
    }
}
