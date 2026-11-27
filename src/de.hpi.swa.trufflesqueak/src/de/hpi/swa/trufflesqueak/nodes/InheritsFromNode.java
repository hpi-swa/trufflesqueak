/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUtils;
import de.hpi.swa.trufflesqueak.nodes.dispatch.LookupClassGuard;

@GenerateInline
@GenerateUncached
@GenerateCached(false)
public abstract class InheritsFromNode extends AbstractNode {
    protected static final int CACHE_SIZE = 3;

    public abstract boolean execute(Node node, Object object, ClassObject classObject);

    /*
     * Cache per receiver class, not per receiver: the answer depends on the class hierarchy from the
     * receiver's class up to classObject (or up to the root if classObject is not found), so the
     * hierarchy assumptions of all these classes guard it. The receiver's class itself can change
     * as well (e.g., via primitiveChangeClassTo:), which the lookup class guard checks.
     */
    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_SIZE", guards = {"classObject == cachedClass", "guard.check(object)"}, assumptions = "assumptions")
    protected static final boolean doCached(final Object object, final ClassObject classObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("create(object)") final LookupClassGuard guard,
                    @Cached("inheritsFromUncached(object, cachedClass)") final boolean inInheritanceChain,
                    @Cached(value = "createAssumptions(object, cachedClass, inInheritanceChain)", dimensions = 1) final Assumption[] assumptions) {
        return inInheritanceChain;
    }

    protected static final boolean inheritsFromUncached(final Object receiver, final ClassObject superClass) {
        return inheritsFrom(SqueakObjectClassNode.executeUncached(receiver), superClass);
    }

    protected static final Assumption[] createAssumptions(final Object receiver, final ClassObject superClass, final boolean inInheritanceChain) {
        return DispatchUtils.createAssumptions(SqueakObjectClassNode.executeUncached(receiver), inInheritanceChain ? superClass : null, null);
    }

    @ReportPolymorphism.Megamorphic
    @Specialization(replaces = "doCached")
    protected static final boolean doUncached(final Node node, final Object receiver, final ClassObject superClass,
                    @Cached final SqueakObjectClassNode classNode) {
        return inheritsFrom(classNode.executeLookup(node, receiver), superClass);
    }

    private static boolean inheritsFrom(final ClassObject receiverClass, final ClassObject superClass) {
        ClassObject classObject = receiverClass;
        while (classObject != superClass) {
            classObject = classObject.getSuperclassOrNull();
            if (classObject == null) {
                return false;
            }
        }
        return true;
    }
}
