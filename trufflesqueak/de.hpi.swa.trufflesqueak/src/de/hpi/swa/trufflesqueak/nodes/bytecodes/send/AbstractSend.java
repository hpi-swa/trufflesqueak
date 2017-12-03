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

public abstract class AbstractSend extends SqueakBytecodeNode {
    public final Object selector;
    public final int argumentCount;
    @Child protected SqueakLookupClassNode lookupClassNode;
    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode;

    public AbstractSend(CompiledCodeObject code, int idx, Object sel, int argcount) {
        super(code, idx);
        selector = sel;
        argumentCount = argcount;
        lookupClassNode = SqueakLookupClassNode.create(code);
        dispatchNode = DispatchNode.create();
        lookupNode = LookupNode.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeSend(frame, receiver(frame));
        // TODO: Object as Method
    }

    @ExplodeLoop
    public Object executeSend(VirtualFrame frame, Object receiver) {
        ClassObject rcvrClass;
        try {
            rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(receiver));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("receiver has no class");
        }
        Object[] arguments = new Object[argumentCount + 1];
        arguments[0] = receiver;
        for (int i = 0; i < argumentCount; i++) {
            arguments[i + 1] = pop(frame);
        }
        Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
        return dispatchNode.executeDispatch(lookupResult, arguments);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class)) && getSourceSection().isAvailable();
    }
}
