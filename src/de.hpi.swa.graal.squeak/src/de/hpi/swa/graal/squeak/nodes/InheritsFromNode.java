package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodes.LookupClassNode;

public abstract class InheritsFromNode extends AbstractNode {
    protected static final int CACHE_SIZE = 3;

    @Child private LookupClassNode lookupClassNode;

    protected InheritsFromNode(final SqueakImageContext image) {
        lookupClassNode = LookupClassNode.create(image);
    }

    public static InheritsFromNode create(final SqueakImageContext image) {
        return InheritsFromNodeGen.create(image);
    }

    public abstract boolean execute(Object object, ClassObject classObject);

    @SuppressWarnings("unused")
    @Specialization(limit = "CACHE_SIZE", guards = {"object == cachedObject", "classObject == cachedClass"}, assumptions = {"classHierarchyStable"})
    protected static final boolean doCached(final Object object, final ClassObject classObject,
                    @Cached("object") final Object cachedObject,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("cachedClass.getClassHierarchyStable()") final Assumption classHierarchyStable,
                    @Cached("doUncached(object, cachedClass)") final boolean inInheritanceChain) {
        return inInheritanceChain;
    }

    @Specialization(replaces = "doCached")
    protected final boolean doUncached(final Object receiver, final ClassObject superClass) {
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
