package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.LookupMethodNodeGen.ExecuteLookupNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ReadArrayObjectNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class LookupMethodNode extends Node {
    protected static final int LOOKUP_CACHE_SIZE = 3;

    @Child protected ExecuteLookupNode executeLookupNode;

    public static LookupMethodNode create(final SqueakImageContext image) {
        return LookupMethodNodeGen.create(image);
    }

    public abstract Object executeLookup(Object sqClass, Object selector);

    protected LookupMethodNode(final SqueakImageContext image) {
        executeLookupNode = ExecuteLookupNodeGen.create(image);
    }

    protected abstract static class ExecuteLookupNode extends AbstractNodeWithImage {
        protected ExecuteLookupNode(final SqueakImageContext image) {
            super(image);
        }

        protected abstract Object executeLookup(ClassObject classObject, NativeObject selector);

        @Specialization
        protected final Object doLookup(final ClassObject classObject, final NativeObject selector,
                        @Cached("create()") final SqueakObjectSizeNode sizeNode,
                        @Cached("create()") final ReadArrayObjectNode readNode) {
            ClassObject lookupClass = classObject;
            while (lookupClass != null) {
                final PointersObject methodDict = lookupClass.getMethodDict();
                for (int i = METHOD_DICT.NAMES; i < sizeNode.execute(methodDict); i++) {
                    final Object methodSelector = methodDict.at0(i);
                    if (selector == methodSelector) {
                        final ArrayObject values = (ArrayObject) methodDict.at0(METHOD_DICT.VALUES);
                        return readNode.execute(values, i - METHOD_DICT.NAMES);
                    }
                }
                lookupClass = lookupClass.getSuperclassOrNull();
            }
            if (selector == image.doesNotUnderstand) {
                throw new SqueakException("Could not find does not understand method for:", classObject);
            }
            return doLookup(classObject, image.doesNotUnderstand, sizeNode, readNode);
        }
    }

    @SuppressWarnings("unused")
    @Specialization(limit = "LOOKUP_CACHE_SIZE", guards = {"squeakClass == cachedSqClass",
                    "selector == cachedSelector"}, assumptions = {"methodLookupStable"})
    protected static final Object doDirect(final ClassObject squeakClass, final NativeObject selector,
                    @Cached("squeakClass") final ClassObject cachedSqClass,
                    @Cached("selector") final NativeObject cachedSelector,
                    @Cached("cachedSqClass.getMethodDictStable()") final Assumption methodLookupStable,
                    @Cached("executeLookupNode.executeLookup(cachedSqClass, cachedSelector)") final Object cachedMethod) {
        return cachedMethod;
    }

    @Specialization(replaces = "doDirect")
    protected final Object doIndirect(final ClassObject sqClass, final NativeObject selector) {
        return executeLookupNode.executeLookup(sqClass, selector);
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final Object doFail(final Object sqClass, final Object selector) {
        throw new SqueakException("failed to lookup generic selector object on generic class");
    }
}
