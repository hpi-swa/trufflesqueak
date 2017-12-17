package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimAllInstances extends PrimitiveNodeUnary {
    private final Set<BaseSqueakObject> classesWithNoInstances;
    private final ObjectGraph objectGraph;

    public PrimAllInstances(CompiledMethodObject code) {
        super(code);
        // TODO: BlockContext missing.
        BaseSqueakObject[] classes = new BaseSqueakObject[]{code.image.smallIntegerClass, code.image.characterClass, code.image.floatClass};
        classesWithNoInstances = new HashSet<>(Arrays.asList(classes));
        objectGraph = new ObjectGraph(code);
    }

    protected boolean hasNoInstances(ClassObject classObject) {
        return classesWithNoInstances.contains(classObject);
    }

    protected boolean isClassObject(ClassObject classObject) {
        return classObject.isClass();
    }

    @SuppressWarnings("unused")
    protected boolean isIllegal(BaseSqueakObject obj) {
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "hasNoInstances(classObj)")
    ListObject noInstances(ClassObject classObj) {
        return code.image.wrap(new Object[0]);
    }

    @Specialization(guards = "isClassObject(classObj)")
    ListObject allInstances(ClassObject classObj) {
        return code.image.wrap(objectGraph.allInstances(classObj).toArray());
    }

    @SuppressWarnings("unused")
    @Specialization(guards = "isIllegal(obj)")
    ListObject allInstances(BaseSqueakObject obj) {
        throw new PrimitiveFailed();
    }
}
