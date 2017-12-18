package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.List;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimNextInstance extends PrimitiveNodeUnary {
    private final ObjectGraph objectGraph;

    public PrimNextInstance(CompiledMethodObject code) {
        super(code);
        objectGraph = new ObjectGraph(code);
    }

    protected boolean hasNoInstances(BaseSqueakObject sqObject) {
        return objectGraph.getClassesWithNoInstances().contains(sqObject.getSqClass());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNoInstances(sqObject)")
    BaseSqueakObject noInstances(BaseSqueakObject sqObject) {
        return code.image.nil;
    }

    @Specialization
    BaseSqueakObject someInstance(BaseSqueakObject sqObject) {
        List<BaseSqueakObject> instances = objectGraph.allInstances(sqObject.getSqClass());
        int index;
        try {
            index = instances.indexOf(sqObject);
        } catch (NullPointerException e) {
            index = -1;
        }
        try {
            return instances.get(index + 1);
        } catch (IndexOutOfBoundsException e) {
            return code.image.nil;
        }
    }
}
