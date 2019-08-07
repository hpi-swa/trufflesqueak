package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.TypeSystemReference;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.BooleanObject;

@ImportStatic({SqueakGuards.class, BooleanObject.class})
@TypeSystemReference(SqueakTypes.class)
public abstract class AbstractNode extends Node {

}
