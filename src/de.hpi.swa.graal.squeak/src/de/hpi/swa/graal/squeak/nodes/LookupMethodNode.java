package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;

public abstract class LookupMethodNode extends AbstractNode {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    public static LookupMethodNode create() {
        return LookupMethodNodeGen.create();
    }

    public abstract Object executeLookup(ClassObject sqClass, NativeObject selector);

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass",
                    "selector == cachedSelector"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
    protected static final Object doCached(final ClassObject classObject, final NativeObject selector,
                    @Cached("classObject") final ClassObject cachedClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("doUncached(cachedClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doCached")
    protected static final Object doUncached(final ClassObject classObject, final NativeObject selector) {
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final Object[] methodDictPointers = lookupClass.getMethodDict().getPointers();
            for (int i = METHOD_DICT.NAMES; i < methodDictPointers.length; i++) {
                if (selector == methodDictPointers[i]) {
                    return ((ArrayObject) methodDictPointers[METHOD_DICT.VALUES]).getObjectStorage()[i - METHOD_DICT.NAMES];
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
        return null; // Signals a doesNotUnderstand.
    }
}
