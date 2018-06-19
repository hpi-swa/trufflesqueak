package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;

@ReportPolymorphism
@TypeSystemReference(SqueakTypes.class)
public abstract class SqueakNode extends Node {
    public abstract Object executeRead(VirtualFrame frame);
}
