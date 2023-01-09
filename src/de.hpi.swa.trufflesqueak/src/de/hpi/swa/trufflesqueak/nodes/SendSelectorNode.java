/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
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
public abstract class SendSelectorNode extends Node {
    private final NativeObject selector;

    protected SendSelectorNode(final NativeObject selector) {
        this.selector = selector;
    }

    public static SendSelectorNode create(final NativeObject selector) {
        return SendSelectorNodeGen.create(selector);
    }

    public abstract Object executeSend(VirtualFrame frame, Object... receiverAndArguments);

    @Specialization
    protected final Object send(final VirtualFrame frame, final Object[] receiverAndArguments,
                    @Bind("this") final Node node,
                    @Cached final SqueakObjectClassNode lookupClassNode,
                    @Cached final LookupMethodNode lookupMethodNode,
                    @Cached final DispatchEagerlyNode dispatchNode) {
        final ClassObject rcvrClass = lookupClassNode.executeLookup(node, receiverAndArguments[0]);
        final CompiledCodeObject method = (CompiledCodeObject) lookupMethodNode.executeLookup(node, rcvrClass, selector);
        final Object result = dispatchNode.executeDispatch(frame, method, receiverAndArguments);
        assert result != null : "Result of a message send should not be null";
        return result;
    }

    @Override
    public String getDescription() {
        return selector.asStringUnsafe();
    }
}
