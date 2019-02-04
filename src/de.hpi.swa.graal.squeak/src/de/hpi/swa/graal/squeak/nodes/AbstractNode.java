package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

@ReportPolymorphism
@ImportStatic(SqueakGuards.class)
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNode extends Node {

}
