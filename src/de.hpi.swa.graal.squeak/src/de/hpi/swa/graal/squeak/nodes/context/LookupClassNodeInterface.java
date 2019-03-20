package de.hpi.swa.graal.squeak.nodes.context;

import de.hpi.swa.graal.squeak.model.ClassObject;

public interface LookupClassNodeInterface {
    ClassObject executeLookup(Object receiver);
}
