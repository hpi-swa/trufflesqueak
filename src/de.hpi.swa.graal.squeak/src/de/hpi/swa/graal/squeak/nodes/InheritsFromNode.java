package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupClassNode;

public abstract class InheritsFromNode extends AbstractNode {
    protected static final int CACHE_SIZE = 3;

    public static InheritsFromNode create() {
        return InheritsFromNodeGen.create();
    }

    public abstract boolean execute(Object object, ClassObject classObject);

    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_SIZE", guards = {"object == cachedObject", "classObject == cachedClass"}, assumptions = {"classHierarchyStable"})
    protected static final boolean doCached(final Object object, final ClassObject classObject,
                    @Cached("object") final Object cachedObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("cachedClass.getClassHierarchyStable()") final Assumption classHierarchyStable,
                    @Cached final LookupClassNode lookupClassNode,
                    @Cached("doUncached(object, cachedClass, lookupClassNode)") final boolean inInheritanceChain) {
        return inInheritanceChain;
    }

    @Specialization(replaces = "doCached")
    protected static final boolean doUncached(final Object receiver, final ClassObject superClass,
                    @Cached final LookupClassNode lookupClassNode) {
        ClassObject classObject = lookupClassNode.executeLookup(receiver);
        while (classObject != superClass) {
            classObject = classObject.getSuperclassOrNull();
            if (classObject == null) {
                return false;
            }
        }
        return true;
    }
}
