package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

/**
 * Performs a send to receiver with arguments. For use in other node. Selector must resolve to a
 * {@link CompiledMethodObject}, object-as-method is not allowed.
 */
public final class SendSelectorNode extends Node {
    private final NativeObject selector;

    @Child private SqueakObjectClassNode lookupClassNode = SqueakObjectClassNode.create();
    @Child private LookupMethodNode lookupMethodNode = LookupMethodNode.create();
    @Child private DispatchEagerlyNode dispatchNode = DispatchEagerlyNode.create();

    private SendSelectorNode(final NativeObject selector) {
        this.selector = selector;
    }

    public static SendSelectorNode create(final NativeObject selector) {
        return new SendSelectorNode(selector);
    }

    public Object executeSend(final VirtualFrame frame, final Object... receiverAndArguments) {
        final ClassObject rcvrClass = lookupClassNode.executeLookup(receiverAndArguments[0]);
        final CompiledMethodObject method = (CompiledMethodObject) lookupMethodNode.executeLookup(rcvrClass, selector);
        final ContextObject context = FrameAccess.getContext(frame);
        final Object contextOrMarker = context != null ? context : FrameAccess.getMarker(frame);
        final Object result = dispatchNode.executeDispatch(frame, method, receiverAndArguments, contextOrMarker);
        assert result != null : "Result of a message send should not be null";
        return result;
    }

    @Override
    public String getDescription() {
        return selector.asStringUnsafe();
    }
}
