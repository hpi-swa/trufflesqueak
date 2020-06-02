package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;

@GenerateWrapper
public abstract class AbstractExecuteContextNode extends AbstractNode implements InstrumentableNode {

    public abstract Object executeFresh(VirtualFrame frame);

    public abstract Object executeResumeAtStart(VirtualFrame frame);

    public abstract Object executeResumeInMiddle(VirtualFrame frame, long initialPC);

    @Override
    public WrapperNode createWrapper(final ProbeNode probeNode) {
        return new AbstractExecuteContextNodeWrapper(this, probeNode);
    }
}
