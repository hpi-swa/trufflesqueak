/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodes.AbstractLookupMethodWithSelectorNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;

/**
 * Performs a send to receiver with arguments. For use in other node. Selector must resolve to a
 * {@link CompiledMethodObject}, object-as-method is not allowed.
 */
public final class SendSelectorNode extends Node {
    private final NativeObject selector;

    @Child private SqueakObjectClassNode lookupClassNode = SqueakObjectClassNode.create();
    @Child private AbstractLookupMethodWithSelectorNode lookupMethodNode;
    @Child private DispatchEagerlyNode dispatchNode;

    private SendSelectorNode(final CompiledCodeObject code, final NativeObject selector) {
        dispatchNode = DispatchEagerlyNode.create(code);
        lookupMethodNode = AbstractLookupMethodWithSelectorNode.create(selector);
        this.selector = selector;
    }

    public static SendSelectorNode create(final CompiledCodeObject code, final NativeObject selector) {
        return new SendSelectorNode(code, selector);
    }

    public Object executeSend(final VirtualFrame frame, final Object... receiverAndArguments) {
        final ClassObject rcvrClass = lookupClassNode.executeLookup(receiverAndArguments[0]);
        final CompiledMethodObject method = (CompiledMethodObject) lookupMethodNode.executeLookup(rcvrClass);
        final Object result = dispatchNode.executeDispatch(frame, method, receiverAndArguments);
        assert result != null : "Result of a message send should not be null";
        return result;
    }

    @Override
    public String getDescription() {
        return selector.asStringUnsafe();
    }
}
