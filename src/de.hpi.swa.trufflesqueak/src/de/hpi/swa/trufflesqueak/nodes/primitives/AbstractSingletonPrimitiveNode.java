/*
 * Copyright (c) 2021-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.lang.reflect.InvocationTargetException;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

public abstract class AbstractSingletonPrimitiveNode extends AbstractPrimitiveNode {
    private AbstractSingletonPrimitiveNode instance;

    public static AbstractSingletonPrimitiveNode getInstance(final Class<? extends AbstractSingletonPrimitiveNode> primitiveClass) {
        try {
            final AbstractSingletonPrimitiveNode node = primitiveClass.getDeclaredConstructor().newInstance();
            node.setInstance(node);
            return node;
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
            throw CompilerDirectives.shouldNotReachHere(e);
        }
    }

    @Override
    public final Object executeWithArguments(final VirtualFrame frame, final Object... receiverAndArguments) {
        return execute();
    }

    protected abstract Object execute();

    @Override
    public final boolean isAdoptable() {
        return false;
    }

    @Override
    public final Node copy() {
        return instance;
    }

    @Override
    public final Node deepCopy() {
        return instance;
    }

    private void setInstance(final AbstractSingletonPrimitiveNode node) {
        assert instance == null;
        instance = node;
    }
}
