/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodesFactory.LookupMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodesFactory.LookupMethodWithSelectorAndBreakpointNodeGen;
import de.hpi.swa.graal.squeak.nodes.AbstractLookupMethodWithSelectorNodesFactory.LookupMethodWithSelectorNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.util.SqueakMessageInterceptor;

public abstract class AbstractLookupMethodWithSelectorNodes extends AbstractNode {

    public abstract static class AbstractLookupMethodNode extends AbstractNode {
        protected static final int LOOKUP_CACHE_SIZE = 6;
        /**
         * An AbstractPointersObjectReadNode is sufficient for accessing `values` instance variable
         * here.
         */
        @Child protected AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

        protected static Object first(final Object[] array) {
            return array[0];
        }

        protected final Object[] methodAndClass(final ClassObject classObject, final NativeObject selector) {
            ClassObject lookupClass = classObject;
            while (lookupClass != null) {
                final VariablePointersObject methodDict = lookupClass.getMethodDict();
                final Object[] methodDictVariablePart = methodDict.getVariablePart();
                for (int i = 0; i < methodDictVariablePart.length; i++) {
                    if (selector == methodDictVariablePart[i]) {
                        return new Object[]{readNode.executeArray(methodDict, METHOD_DICT.VALUES).getObjectStorage()[i], lookupClass};
                    }
                }
                lookupClass = lookupClass.getSuperclassOrNull();
            }
            assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
            return null; // Signals a doesNotUnderstand.
        }

    }

    public abstract static class AbstractLookupMethodWithSelectorNode extends AbstractLookupMethodNode {

        protected final NativeObject selector;

        protected AbstractLookupMethodWithSelectorNode(final NativeObject selector) {
            super();
            this.selector = selector;
        }

        private static AbstractLookupMethodWithSelectorNode create(final NativeObject selector, final ClassObject breakpointClass) {
            if (breakpointClass == null) {
                return LookupMethodWithSelectorNodeGen.create(selector);
            } else {
                return LookupMethodWithSelectorAndBreakpointNodeGen.create(selector, breakpointClass);
            }
        }

        public static AbstractLookupMethodWithSelectorNode create(final NativeObject selector) {
            return create(selector, SqueakMessageInterceptor.breakpointClassFor(selector));
        }

        public abstract Object executeLookup(ClassObject sqClass);

    }

    public abstract static class LookupMethodWithSelectorNode extends AbstractLookupMethodWithSelectorNode {

        protected LookupMethodWithSelectorNode(final NativeObject selector) {
            super(selector);
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
        protected static final Object doCached(final ClassObject classObject,
                        @Cached("classObject") final ClassObject cachedClass,
                        @Cached("doUncached(cachedClass)") final Object cachedMethod) {
            return cachedMethod;
        }

        @Specialization(replaces = "doCached")
        protected final Object doUncached(final ClassObject classObject) {
            ClassObject lookupClass = classObject;
            while (lookupClass != null) {
                final VariablePointersObject methodDict = lookupClass.getMethodDict();
                final Object[] methodDictVariablePart = methodDict.getVariablePart();
                for (int i = 0; i < methodDictVariablePart.length; i++) {
                    if (selector == methodDictVariablePart[i]) {
                        return readNode.executeArray(methodDict, METHOD_DICT.VALUES).getObjectStorage()[i];
                    }
                }
                lookupClass = lookupClass.getSuperclassOrNull();
            }
            assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
            return null; // Signals a doesNotUnderstand.
        }
    }

    public abstract static class LookupMethodNode extends AbstractLookupMethodNode {

        public static LookupMethodNode create() {
            return LookupMethodNodeGen.create();
        }

        public abstract Object executeLookup(ClassObject sqClass, NativeObject selector);

        @SuppressWarnings("unused")
        @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "!breakpointWasHit(cachedMethodAndClass, selector)",
                        "selector == cachedSelector"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
        protected static final Object doCached(final ClassObject classObject, final NativeObject selector,
                        @Cached("classObject") final ClassObject cachedClass,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached(value = "methodAndClass(cachedClass, selector)", dimensions = 0) final Object[] cachedMethodAndClass,
                        @Cached("first(cachedMethodAndClass)") final Object cachedMethod) {
            return cachedMethod;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "breakpointWasHit(cachedMethodAndClass, selector)",
                        "selector == cachedSelector"}, assumptions = {"cachedClass.getClassHierarchyStable()", "cachedClass.getMethodDictStable()"})
        protected static final Object doCachedWithBreak(final ClassObject classObject, final NativeObject selector,
                        @Cached("classObject") final ClassObject cachedClass,
                        @Cached("selector") final NativeObject cachedSelector,
                        @Cached(value = "methodAndClass(cachedClass, selector)", dimensions = 0) final Object[] cachedMethodAndClass,
                        @Cached("first(cachedMethodAndClass)") final Object cachedMethod) {
            SqueakMessageInterceptor.breakpointReached(selector);
            return cachedMethod;
        }

        @Specialization(replaces = "doCached")
        protected final Object doUncached(final ClassObject classObject, final NativeObject selector) {
            final Object[] methodAndClass = methodAndClass(classObject, selector);
            if (breakpointWasHit(methodAndClass, selector)) {
                SqueakMessageInterceptor.breakpointReached(selector);
            }
            return methodAndClass[0];
        }

        protected static final boolean breakpointWasHit(final Object[] array, final NativeObject selector) {
            return array[1] == SqueakMessageInterceptor.breakpointClassFor(selector);
        }
    }

    public abstract static class LookupMethodWithSelectorAndBreakpointNode extends AbstractLookupMethodWithSelectorNode {

        private final ClassObject breakpointClass;

        protected LookupMethodWithSelectorAndBreakpointNode(final NativeObject selector, final ClassObject breakpointClass) {
            super(selector);
            this.breakpointClass = breakpointClass;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "!breakpointWasHit(cachedMethodAndClass)"}, assumptions = {"cachedClass.getClassHierarchyStable()",
                        "cachedClass.getMethodDictStable()"})
        protected static final Object doCached(final ClassObject classObject,
                        @Cached("classObject") final ClassObject cachedClass,
                        @Cached(value = "methodAndClass(cachedClass, selector)", dimensions = 0) final Object[] cachedMethodAndClass,
                        @Cached("first(cachedMethodAndClass)") final Object cachedMethod) {
            return cachedMethod;
        }

        @SuppressWarnings("unused")
        @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"classObject == cachedClass", "breakpointWasHit(cachedMethodAndClass)"}, assumptions = {"cachedClass.getClassHierarchyStable()",
                        "cachedClass.getMethodDictStable()"})
        protected final Object doCachedWithBreak(final ClassObject classObject,
                        @Cached("classObject") final ClassObject cachedClass,
                        @Cached(value = "methodAndClass(cachedClass, selector)", dimensions = 0) final Object[] cachedMethodAndClass,
                        @Cached("first(cachedMethodAndClass)") final Object cachedMethod) {
            SqueakMessageInterceptor.breakpointReached(selector);
            return cachedMethod;
        }

        @Specialization(replaces = "doCached")
        protected final Object doUncached(final ClassObject classObject) {
            final Object[] methodAndClass = methodAndClass(classObject, selector);
            if (breakpointWasHit(methodAndClass)) {
                SqueakMessageInterceptor.breakpointReached(selector);
            }
            return methodAndClass[0];
        }

        protected final boolean breakpointWasHit(final Object[] array) {
            return array[1] == breakpointClass;
        }
    }
}
