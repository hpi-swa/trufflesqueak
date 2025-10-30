/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

@GenerateWrapper
public abstract class AbstractExecuteContextNode extends AbstractNode implements InstrumentableNode {

    public abstract Object execute(VirtualFrame frame, int startPC);

    public abstract CompiledCodeObject getCodeObject();

    @Override
    public WrapperNode createWrapper(final ProbeNode probeNode) {
        return new AbstractExecuteContextNodeWrapper(this, probeNode);
    }
}
