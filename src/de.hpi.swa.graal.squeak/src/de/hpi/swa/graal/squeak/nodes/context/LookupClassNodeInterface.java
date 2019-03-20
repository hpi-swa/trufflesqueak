package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.nodes.NodeInterface;

import de.hpi.swa.graal.squeak.model.ClassObject;

public interface LookupClassNodeInterface extends NodeInterface {
    ClassObject executeLookup(Object receiver);
}
