/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class InheritsFromNode extends AbstractNode {
    protected static final int CACHE_SIZE = 3;

    public abstract boolean execute(Node node, Object object, ClassObject classObject);

    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_SIZE", guards = {"object == cachedObject", "classObject == cachedClass"}, assumptions = {"cachedClass.getClassHierarchyAndMethodDictStable()"})
    protected static final boolean doCached(final Object object, final ClassObject classObject,
                    @Cached("object") final Object cachedObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("doUncached(object, cachedClass)") final boolean inInheritanceChain) {
        return inInheritanceChain;
    }

    protected static final boolean doUncached(final Object receiver, final ClassObject superClass) {
        return doUncached(null, receiver, superClass, SqueakObjectClassNode.getUncached());
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doCached")
    protected static final boolean doUncached(final Node node, final Object receiver, final ClassObject superClass,
                    @Cached final SqueakObjectClassNode classNode) {
        ClassObject classObject = classNode.executeLookup(node, receiver);
        while (classObject != superClass) {
            classObject = classObject.getSuperclassOrNull();
            if (classObject == null) {
                return false;
            }
        }
        return true;
    }
}
