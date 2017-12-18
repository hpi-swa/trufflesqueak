package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimAllInstances extends PrimitiveNodeUnary {
    private final ObjectGraph objectGraph;

    public PrimAllInstances(CompiledMethodObject code) {
        super(code);
        objectGraph = new ObjectGraph(code);
    }

    protected boolean hasNoInstances(ClassObject classObject) {
        return objectGraph.getClassesWithNoInstances().contains(classObject);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNoInstances(classObject)")
    ListObject noInstances(ClassObject classObject) {
        return code.image.wrap(new Object[0]);
    }

    @Specialization
    ListObject allInstances(ClassObject classObject) {
        return code.image.wrap(objectGraph.allInstances(classObject).toArray());
    }

    @SuppressWarnings("unused")
    @Fallback
    ListObject allInstances(Object object) {
        throw new PrimitiveFailed();
    }
}
