/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNodes.AbstractArgumentNode;

@NodeChild(value = "arguments", type = AbstractArgumentNode[].class)
public abstract class AbstractPrimitiveNode extends AbstractNode {

    public abstract Object executeWithArguments(VirtualFrame frame, Object... arguments);

    public abstract Object executePrimitive(VirtualFrame frame);

    public boolean acceptsMethod(@SuppressWarnings("unused") final CompiledCodeObject method) {
        CompilerAsserts.neverPartOfCompilation();
        return true;
    }
}
