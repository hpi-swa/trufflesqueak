package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.dsl.Fallback;
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

    protected boolean isSmallIntegerClass(ClassObject classObject) {
        return classObject.equals(code.image.smallIntegerClass);
    }

    protected boolean isClassObject(ClassObject classObject) {
        return classObject.isClass();
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isSmallIntegerClass(classObject)")
    ListObject allInstances(ClassObject classObject) {
        throw new PrimitiveFailed();
    }

    @Specialization(guards = "isClassObject(classObject)")
    BaseSqueakObject someInstance(ClassObject classObject) {
        try {
            return objectGraph.someInstance(classObject).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new PrimitiveFailed();
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    ListObject allInstances(Object object) {
        throw new PrimitiveFailed();
    }
}
