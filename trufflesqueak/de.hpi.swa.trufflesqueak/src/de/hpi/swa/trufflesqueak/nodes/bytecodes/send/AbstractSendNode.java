package de.hpi.swa.trufflesqueak.nodes.bytecodes.send;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.nodes.UnexpectedResultException;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.DispatchNode;
import de.hpi.swa.trufflesqueak.nodes.LookupNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakTypesGen;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.AbstractBytecodeNode;
import de.hpi.swa.trufflesqueak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PopNReversedStackNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public abstract class AbstractSendNode extends AbstractBytecodeNode {
    @CompilationFinal protected final Object selector;
    @CompilationFinal private final int argumentCount;
    @Child protected SqueakLookupClassNode lookupClassNode;
    @Child private LookupNode lookupNode;
    @Child private DispatchNode dispatchNode;
    @Child private PopNReversedStackNode popNReversedNode;
    @Child private PushStackNode pushNode;

    public AbstractSendNode(CompiledCodeObject code, int index, int numBytecodes, Object sel, int argcount) {
        super(code, index, numBytecodes);
        selector = sel;
        argumentCount = argcount;
        lookupClassNode = SqueakLookupClassNode.create(code);
        dispatchNode = DispatchNode.create();
        lookupNode = LookupNode.create();
        pushNode = new PushStackNode(code);
        popNReversedNode = new PopNReversedStackNode(code, 1 + argumentCount);
    }

    @Override
    public void executeVoid(VirtualFrame frame) {
        pushNode.executeWrite(frame, executeSend(frame));
        // TODO: Object as Method
    }

    private Object executeSend(VirtualFrame frame) {
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
    public String toString() {
        return "send: " + selector.toString();
    }

    public Object getSelector() {
        return selector;
    }

    @Override
    protected boolean isTaggedWith(Class<?> tag) {
        if (selector.toString().equals("halt") && tag == DebuggerTags.AlwaysHalt.class) {
            return true;
        }
        return ((tag == StandardTags.StatementTag.class) || (tag == StandardTags.CallTag.class));
    }
}
