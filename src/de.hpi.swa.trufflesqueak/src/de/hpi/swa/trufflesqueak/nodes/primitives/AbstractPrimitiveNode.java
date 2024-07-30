/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public abstract class AbstractPrimitiveNode extends AbstractNode {

    public abstract Object executeWithArguments(VirtualFrame frame, Object... receiverAndArguments);

    public boolean acceptsMethod(@SuppressWarnings("unused") final CompiledCodeObject method) {
        CompilerAsserts.neverPartOfCompilation();
        return true;
    }
}
