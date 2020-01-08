/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectClassNode;

public abstract class InheritsFromNode extends AbstractNode {
    protected static final int CACHE_SIZE = 3;

    public static InheritsFromNode create() {
        return InheritsFromNodeGen.create();
    }

    public abstract boolean execute(Object object, ClassObject classObject);

    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_SIZE", guards = {"object == cachedObject", "classObject == cachedClass"}, assumptions = {"cachedClass.getClassHierarchyStable()"})
    protected static final boolean doCached(final Object object, final ClassObject classObject,
                    @Cached("object") final Object cachedObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("doUncached(object, cachedClass)") final boolean inInheritanceChain) {
        return inInheritanceChain;
    }

    protected static final boolean doUncached(final Object receiver, final ClassObject superClass) {
        return doUncached(receiver, superClass, SqueakObjectClassNode.getUncached());
    }

    @Specialization(replaces = "doCached")
    protected static final boolean doUncached(final Object receiver, final ClassObject superClass,
                    @Cached final SqueakObjectClassNode classNode) {
        ClassObject classObject = classNode.executeLookup(receiver);
        while (classObject != superClass) {
            classObject = classObject.getSuperclassOrNull();
            if (classObject == null) {
                return false;
            }
        }
        return true;
    }
}
