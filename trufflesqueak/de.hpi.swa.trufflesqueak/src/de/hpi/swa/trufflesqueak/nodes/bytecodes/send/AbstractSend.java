package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.DispatchNodeGen;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNodeGen;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.Pop;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SqueakBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakClass;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakClassNodeGen;

public abstract class AbstractSend extends SqueakBytecodeNode {
    private final BaseSqueakObject selector;
    @Child protected SqueakNode receiverNode;
    @Child protected SqueakClass lookupClassNode;
    @Children protected final Pop[] argumentNodes;
    @Child protected LookupNode lookupNode;
    @Child protected DispatchNode dispatchNode;

    public AbstractSend(CompiledMethodObject cm, int idx, BaseSqueakObject sel, int argcount) {
        super(cm, idx);
        selector = sel;
        receiverNode = new Pop(cm, idx);
        lookupClassNode = SqueakClassNodeGen.create(cm, FrameSlotReadNode.peek(cm, argcount));
        argumentNodes = new Pop[argcount];
        for (int i = 0; i < argcount; i++) {
            argumentNodes[i] = new Pop(cm, idx);
        }
        dispatchNode = DispatchNodeGen.create();
        lookupNode = LookupNodeGen.create();
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        ClassObject rcvrClass;
        try {
            rcvrClass = SqueakTypesGen.expectClassObject(lookupClassNode.executeGeneric(frame));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("receiver has no class");
        }
        // first take the arguments off the stack in reverse order
        CompilerAsserts.compilationConstant(argumentNodes.length);
        Object[] arguments = new Object[argumentNodes.length + 1];
        for (int i = argumentNodes.length; i > 0; i--) {
            arguments[i] = argumentNodes[i - 1].executeGeneric(frame);
        }
        // now we can take the receiver
        arguments[0] = receiverNode.executeGeneric(frame);
        Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
        return dispatchNode.executeDispatch(lookupResult, arguments);
        // TODO: OaM
    }
}
