package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNode extends Node {
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
}
