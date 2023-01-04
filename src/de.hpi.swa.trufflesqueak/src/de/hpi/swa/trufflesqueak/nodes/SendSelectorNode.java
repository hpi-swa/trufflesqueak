/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchEagerlyNode;

/**
 * Performs a send to receiver with arguments. For use in other node. Selector must resolve to a
 * {@link CompiledCodeObject}, object-as-method is not allowed.
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
        final CompiledCodeObject method = (CompiledCodeObject) lookupMethodNode.executeLookup(rcvrClass, selector);
        final Object result = dispatchNode.executeDispatch(frame, method, receiverAndArguments);
        assert result != null : "Result of a message send should not be null";
        return result;
    }

    @Override
    public String getDescription() {
        return selector.asStringUnsafe();
    }
}
