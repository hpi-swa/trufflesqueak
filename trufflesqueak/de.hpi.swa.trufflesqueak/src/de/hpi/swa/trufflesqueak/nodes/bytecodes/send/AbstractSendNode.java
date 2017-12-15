package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;

public abstract class AbstractSendNode extends SqueakBytecodeNode {
    public final Object selector;
    public final int argumentCount;
    @Child protected SqueakLookupClassNode lookupClassNode;
    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode;
    @Child private PopNReversedStackNode popNReversedNode;

    public AbstractSendNode(CompiledCodeObject code, int index, int numBytecodes, Object sel, int argcount) {
        super(code, index, numBytecodes);
        selector = sel;
        argumentCount = argcount;
        lookupClassNode = SqueakLookupClassNode.create(code);
        dispatchNode = DispatchNode.create();
        lookupNode = LookupNode.create();
        popNReversedNode = new PopNReversedStackNode(code, 1 + argumentCount);
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return push(frame, executeSend(frame));
        // TODO: Object as Method
    }

    public Object executeSend(VirtualFrame frame) {
        Object[] rcvrAndArgs = popNReversedNode.execute(frame);
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
        return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class));
    }
}
