/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectReadNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodesFactory.AbstractPointersObjectWriteNodeGen;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public class AbstractPointersObjectNodes {
    protected static final int CACHE_LIMIT = 6;
    protected static final int VARIABLE_PART_INDEX_CACHE_LIMIT = 3;
    protected static final int VARIABLE_PART_LAYOUT_CACHE_LIMIT = 1;

    @GenerateUncached
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class AbstractPointersObjectReadNode extends AbstractNode {

        public static AbstractPointersObjectReadNode create() {
            return AbstractPointersObjectReadNodeGen.create();
        }

        public static AbstractPointersObjectReadNode getUncached() {
            return AbstractPointersObjectReadNodeGen.getUncached();
        }

        public abstract Object execute(AbstractPointersObject obj, long index);

        public abstract long executeLong(AbstractPointersObject obj, long index);

        public abstract ArrayObject executeArray(AbstractPointersObject obj, long index);

        public abstract NativeObject executeNative(AbstractPointersObject obj, long index);

        public abstract PointersObject executePointers(AbstractPointersObject obj, long index);

        public final int executeInt(final AbstractPointersObject obj, final long index) {
            return MiscUtils.toIntExact(executeLong(obj, index));
        }

        @Specialization(limit = "CACHE_LIMIT")
        protected static final Object doRead(final AbstractPointersObject object, final long index,
                        @CachedLibrary("object") final DynamicObjectLibrary lib) {
            return lib.getOrDefault(object, index, NilObject.SINGLETON);
        }
    }

    @GenerateUncached
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class AbstractPointersObjectWriteNode extends AbstractNode {

        public static AbstractPointersObjectWriteNode create() {
            return AbstractPointersObjectWriteNodeGen.create();
        }

        public static AbstractPointersObjectWriteNode getUncached() {
            return AbstractPointersObjectWriteNodeGen.getUncached();
        }

        public abstract void execute(AbstractPointersObject obj, long index, Object value);

        public final void executeNil(final AbstractPointersObject obj, final long index) {
            execute(obj, index, NilObject.SINGLETON);
        }

        @Specialization(limit = "CACHE_LIMIT")
        protected static final void doWrite(final AbstractPointersObject object, final long index,
                        final Object value,
                        @CachedLibrary("object") final DynamicObjectLibrary lib) {
            lib.put(object, index, value);
        }
    }

    @GenerateUncached
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class AbstractPointersObjectInstSizeNode extends AbstractNode {
        public abstract int execute(AbstractPointersObject obj);

// @Specialization(guards = {"object.getLayout() == cachedLayout"}, assumptions =
// "cachedLayout.getValidAssumption()", limit = "1")
// protected static final int doSizeCached(@SuppressWarnings("unused") final AbstractPointersObject
// object,
// @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
// return cachedLayout.getInstSize();
// }

// @Specialization(limit = "CACHE_LIMIT")
// protected static final int doSize(final AbstractPointersObject object,
// @CachedLibrary("object") final DynamicObjectLibrary lib) {
// final int propertyCount = lib.getShape(object).getPropertyCount();
// assert object.getSqueakClass().getBasicInstanceSize() == propertyCount;
// return propertyCount;
// }

        @ReportPolymorphism.Megamorphic
        @Specialization // (replaces = "doSizeCached")
        protected static final int doSizeGeneric(final AbstractPointersObject object) {
            return object.getSqueakClass().getBasicInstanceSize();
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class VariablePointersObjectReadNode extends Node {

        public abstract Object execute(VariablePointersObject object, long index);

        public abstract ArrayObject executeArray(VariablePointersObject object, long index);

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()") // , replaces = "doReadCached")
        protected static final Object doReadGeneric(final VariablePointersObject object, final long index,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, index);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()") // , replaces =
                                                               // {"doReadFromVariablePartCachedIndex",
                                                               // "doReadFromVariablePartCachedLayout"})
        protected static final Object doReadFromVariablePartGeneric(final VariablePointersObject object, final long index) {
            return object.getFromVariablePart(index - object.instsize());
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class VariablePointersObjectWriteNode extends Node {

        public abstract void execute(VariablePointersObject object, long index, Object value);

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()") // , replaces = "doWriteCached")
        protected static final void doWriteGeneric(final VariablePointersObject object, final long index, final Object value,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, index, value);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()") // , replaces =
                                                               // {"doWriteIntoVariablePartCachedIndex",
                                                               // "doWriteIntoVariablePartCachedLayout"})
        protected static final void doWriteIntoVariablePartGeneric(final VariablePointersObject object, final long index, final Object value) {
            object.putIntoVariablePart(index - object.instsize(), value);
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class WeakVariablePointersObjectReadNode extends Node {

        public abstract Object execute(WeakVariablePointersObject object, long index);

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()") // , replaces = "doReadCached")
        protected static final Object doReadGeneric(final WeakVariablePointersObject object, final long index,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, index);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()") // , replaces =
                                                               // {"doReadFromVariablePartCachedIndex",
                                                               // "doReadFromVariablePartCachedLayout"})
        protected static final Object doReadFromVariablePartGeneric(final WeakVariablePointersObject object, final long index,
                        @Cached final ConditionProfile weakRefProfile) {
            return object.getFromVariablePart(index - object.instsize(), weakRefProfile);
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class WeakVariablePointersObjectWriteNode extends Node {

        public abstract void execute(WeakVariablePointersObject object, long index, Object value);

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()") // , replaces = "doWriteCached")
        protected static final void doWriteGeneric(final WeakVariablePointersObject object, final long index, final Object value,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, index, value);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()") // , replaces =
                                                               // {"doWriteIntoVariablePartCachedIndex",
                                                               // "doWriteIntoVariablePartCachedLayout"})
        protected static final void doWriteIntoVariablePartGeneric(final WeakVariablePointersObject object, final long index, final Object value,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(index - object.instsize(), value, primitiveProfile);
        }
    }
}
