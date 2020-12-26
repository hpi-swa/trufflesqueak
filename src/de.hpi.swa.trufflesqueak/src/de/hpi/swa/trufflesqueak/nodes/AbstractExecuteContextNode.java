/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;

@GenerateWrapper
public abstract class AbstractExecuteContextNode extends AbstractNode implements InstrumentableNode {

    public abstract Object executeFresh(VirtualFrame frame, int pc);

    public abstract Object executeResume(VirtualFrame frame, int pc);

    @Override
    public WrapperNode createWrapper(final ProbeNode probeNode) {
        return new AbstractExecuteContextNodeWrapper(this, probeNode);
    }
}
