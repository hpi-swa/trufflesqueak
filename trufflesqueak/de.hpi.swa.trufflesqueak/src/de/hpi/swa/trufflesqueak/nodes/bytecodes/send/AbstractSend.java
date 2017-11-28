package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchNodeGen;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNodeGen;

public abstract class AbstractSend extends SqueakBytecodeNode {
    public final Object selector;
    @Child protected SqueakLookupClassNode lookupClassNode;
    @Children public final SqueakNode[] argumentNodes;
    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode;

    public AbstractSend(CompiledCodeObject code, int idx, Object sel, int argcount) {
        super(code, idx);
        selector = sel;
        argumentNodes = new SqueakNode[argcount];
        lookupClassNode = SqueakLookupClassNodeGen.create(code);
        dispatchNode = DispatchNodeGen.create();
        lookupNode = LookupNodeGen.create();
    }

    protected AbstractSend(CompiledCodeObject code, int idx, Object sel, SqueakNode[] argNodes) {
        super(code, idx);
        selector = sel;
        argumentNodes = argNodes;
        lookupClassNode = SqueakLookupClassNodeGen.create(code);
        dispatchNode = DispatchNodeGen.create();
        lookupNode = LookupNodeGen.create();
    }

    @Override
    public Object executeGeneric(VirtualFrame frame) {
        return executeSend(frame, receiver(frame));
        // TODO: OaM
    }

    @ExplodeLoop
    public Object executeSend(VirtualFrame frame, Object receiver) {
        ClassObject rcvrClass;
        try {
            rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeLookup(receiver));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("receiver has no class");
        }
        CompilerAsserts.compilationConstant(argumentNodes.length);
        Object[] arguments = new Object[argumentNodes.length + 1];
        arguments[0] = receiver;
        for (int i = 0; i < argumentNodes.length; i++) {
            arguments[i + 1] = argumentNodes[i].executeGeneric(frame);
        }
        CompilerAsserts.compilationConstant(argumentNodes.length);
        Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
        return dispatchNode.executeDispatch(lookupResult, arguments);
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class)) && getSourceSection().isAvailable();
    }
}
