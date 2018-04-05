package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

@GenerateWrapper
@ReportPolymorphism
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNode extends Node implements InstrumentableNode {
    public abstract Object executeRead(VirtualFrame frame);

    /**
     * Set the source section for this node. Only nodes associated with a compiled code object have
     * that.
     *
     * @param createSection - the new section
     */
    public void setSourceSection(SourceSection createSection) {
        // no op
    }

    @Override
    public boolean hasTag(Class<? extends Tag> tag) {
        return tag == StandardTags.StatementTag.class;
    }

    public boolean isInstrumentable() {
        return true;
    }

    public WrapperNode createWrapper(ProbeNode probe) {
        return new SqueakNodeWrapper(this, probe);
    }
}
