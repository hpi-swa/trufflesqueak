/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;

/**
 * Performs a send to receiver with arguments. For use in other node. Selector must resolve to a
 * {@link CompiledCodeObject}, object-as-method is not allowed.
 */
public final class SendSelectorNode extends Node {
    private final NativeObject selector;

    @Child private SqueakObjectClassNode lookupClassNode = SqueakObjectClassNode.create();
    @Child private LookupMethodForSelectorNode lookupMethodNode;
    @Child private DispatchEagerlyNode dispatchNode = DispatchEagerlyNode.create();

    private SendSelectorNode(final NativeObject selector) {
        this.selector = selector;
        lookupMethodNode = LookupMethodForSelectorNode.create(selector);
    }

    public static SendSelectorNode create(final NativeObject selector) {
        return new SendSelectorNode(selector);
    }

    public Object executeSend(final VirtualFrame frame, final Object... receiverAndArguments) {
        final ClassObject rcvrClass = lookupClassNode.executeLookup(receiverAndArguments[0]);
        final CompiledCodeObject method = (CompiledCodeObject) lookupMethodNode.executeLookup(rcvrClass);
        final Object result = dispatchNode.executeDispatch(frame, method, receiverAndArguments);
        assert result != null : "Result of a message send should not be null";
        return result;
    }

    @Override
    public String getDescription() {
        return selector.asStringUnsafe();
    }
}
