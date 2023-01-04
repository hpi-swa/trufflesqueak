/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class AbstractDispatchNode extends AbstractNode {
    protected final NativeObject selector;
    protected final int argumentCount;

    public AbstractDispatchNode(final NativeObject selector, final int argumentCount) {
        this.selector = selector;
        this.argumentCount = argumentCount;
    }

    public final NativeObject getSelector() {
        return selector;
    }

    protected final Object lookupInSuperClassSlow(final ClassObject receiver) {
        CompilerAsserts.neverPartOfCompilation();
        assert receiver != null;
        final Object result = receiver.getSuperclassOrNull().lookupInMethodDictSlow(selector);
        if (result == null) {
            return getContext().doesNotUnderstand;
        } else {
            return result;
        }
    }
}
