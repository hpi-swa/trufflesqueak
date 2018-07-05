package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.LookupNodeGen.ExecuteLookupNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;

public abstract class LookupNode extends Node {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    @Child protected ExecuteLookupNode executeLookupNode;

    public static LookupNode create(final SqueakImageContext image) {
        return LookupNodeGen.create(image);
    }

    protected LookupNode(final SqueakImageContext image) {
        executeLookupNode = ExecuteLookupNode.create(image);
    }

    public abstract Object executeLookup(Object sqClass, Object selector);

    protected abstract static class ExecuteLookupNode extends AbstractNodeWithImage {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        protected static ExecuteLookupNode create(final SqueakImageContext image) {
            return ExecuteLookupNodeGen.create(image);
        }

        protected ExecuteLookupNode(final SqueakImageContext image) {
            super(image);
        }

        protected abstract Object executeLookup(ClassObject classObject, NativeObject selector);

        @Specialization
        protected final Object doLookup(final ClassObject classObject, final NativeObject selector) {
            Object lookupClass = classObject;
            while (lookupClass instanceof ClassObject) {
                final Object methodDict = ((ClassObject) lookupClass).getMethodDict();
                if (methodDict instanceof PointersObject) {
                    final Object values = at0Node.execute(methodDict, METHOD_DICT.VALUES);
                    if (values instanceof PointersObject) {
                        for (int i = METHOD_DICT.NAMES; i < sizeNode.execute(methodDict); i++) {
                            final Object methodSelector = at0Node.execute(methodDict, i);
                            if (selector == methodSelector) {
                                return at0Node.execute(values, i - METHOD_DICT.NAMES);
                            }
                        }
                    }
                }
                lookupClass = ((ClassObject) lookupClass).getSuperclass();
            }
            if (selector == image.doesNotUnderstand) {
                throw new SqueakException("Could not find does not understand method for:", classObject);
            }
            return doLookup(classObject, image.doesNotUnderstand);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"sqClass == cachedSqClass",
                    "selector == cachedSelector"}, assumptions = {"methodLookupStable"})
    protected static final Object doDirect(final ClassObject sqClass, final NativeObject selector,
                    @Cached("sqClass") final ClassObject cachedSqClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("cachedSqClass.getMethodLookupStable()") final Assumption methodLookupStable,
                    @Cached("executeLookupNode.executeLookup(cachedSqClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doDirect")
    protected final Object doIndirect(final ClassObject sqClass, final NativeObject selector) {
        return executeLookupNode.executeLookup(sqClass, selector);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final Object fail(final Object sqClass, final Object selector) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }
}
