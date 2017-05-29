package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.instrumentation.PrettyPrintVisitor;

@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNode extends Node {
    public abstract Object executeGeneric(VirtualFrame frame);

    public void accept(PrettyPrintVisitor visitor) {
        visitor.visit(this);
    }

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
