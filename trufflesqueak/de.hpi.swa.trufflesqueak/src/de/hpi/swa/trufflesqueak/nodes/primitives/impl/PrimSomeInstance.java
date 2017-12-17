package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimSomeInstance extends PrimitiveNodeUnary {
    private final ObjectGraph objectGraph;

    public PrimSomeInstance(CompiledMethodObject code) {
        super(code);
        objectGraph = new ObjectGraph(code);
    }

    protected boolean isClassObject(ClassObject classObject) {
        return classObject.isClass() && !classObject.equals(code.image.smallIntegerClass);
    }

    @SuppressWarnings("unused")
    protected boolean isIllegal(BaseSqueakObject obj) {
        return true;
    }

    @Specialization(guards = "isClassObject(classObj)")
    BaseSqueakObject someInstance(ClassObject classObj) {
        try {
            return objectGraph.someInstance(classObj).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new PrimitiveFailed();
        }
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isIllegal(obj)")
    ListObject allInstances(BaseSqueakObject obj) {
        throw new PrimitiveFailed();
    }
}
