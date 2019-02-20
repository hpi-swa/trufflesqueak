package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.nodes.Node;

@ReportPolymorphism
@ImportStatic(SqueakGuards.class)
public abstract class AbstractNode extends Node {

}
