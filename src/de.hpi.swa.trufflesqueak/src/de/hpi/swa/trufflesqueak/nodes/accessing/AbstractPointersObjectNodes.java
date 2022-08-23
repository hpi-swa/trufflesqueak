/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation.AbstractSlotLocationAccessorNode;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation.IllegalWriteException;
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

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final Object doReadCached(final AbstractPointersObject object, final long index,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached("create(cachedLayout.getLocation(index), true)") final AbstractSlotLocationAccessorNode accessorNode) {
            return accessorNode.executeRead(object);
        }

        @TruffleBoundary
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doReadCached")
        protected static final Object doReadGeneric(final AbstractPointersObject object, final long index) {
            return object.getLayout().getLocation(index).read(object);
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

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final void doWriteCached(final AbstractPointersObject object, final long index,
                        final Object value,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached("create(cachedLayout.getLocation(index), false)") final AbstractSlotLocationAccessorNode accessorNode) {
            if (!accessorNode.canStore(value)) {
                /*
                 * Update layout in interpreter if it is not stable yet. This will also invalidate
                 * the assumption and therefore this particular instance of the specialization will
                 * be removed from the cache and replaced by an updated version.
                 */
                CompilerDirectives.transferToInterpreter();
                object.updateLayout(index, value);
                object.getLayout().getLocation(index).writeMustSucceed(object, value);
                return;
            }
            try {
                accessorNode.executeWrite(object, value);
            } catch (final IllegalWriteException e) {
                CompilerDirectives.transferToInterpreter();
                e.printStackTrace();
            }
        }

        @TruffleBoundary
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doWriteCached")
        protected static final void doWriteGeneric(final AbstractPointersObject object, final long index, final Object value) {
            try {
                object.getLayout().getLocation(index).write(object, value);
            } catch (final IllegalWriteException e) {
                /*
                 * Although the layout was valid, it is possible that the location cannot store the
                 * value. Generialize location in the interpreter.
                 */
                CompilerDirectives.transferToInterpreter();
                object.updateLayout(index, value);
                object.getLayout().getLocation(index).writeMustSucceed(object, value);
            }
        }
    }

    @GenerateUncached
    public abstract static class AbstractPointersObjectInstSizeNode extends AbstractNode {
        public abstract int execute(AbstractPointersObject obj);

        @Specialization(guards = {"object.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "1")
        protected static final int doSizeCached(@SuppressWarnings("unused") final AbstractPointersObject object,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return cachedLayout.getInstSize();
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = "doSizeCached")
        protected static final int doSizeGeneric(final AbstractPointersObject object) {
            return object.getLayout().getInstSize();
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class VariablePointersObjectReadNode extends Node {

        public abstract Object execute(VariablePointersObject object, long index);

        public abstract ArrayObject executeArray(VariablePointersObject object, long index);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final Object doReadCached(final VariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, cachedIndex);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()", replaces = "doReadCached")
        protected static final Object doReadGeneric(final VariablePointersObject object, final long index,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, index);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedIndex(final VariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return object.getFromVariablePart(cachedIndex - cachedLayout.getInstSize());
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doReadFromVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedLayout(final VariablePointersObject object, final long index,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return object.getFromVariablePart(index - cachedLayout.getInstSize());
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doReadFromVariablePartCachedIndex", "doReadFromVariablePartCachedLayout"})
        protected static final Object doReadFromVariablePartGeneric(final VariablePointersObject object, final long index) {
            return object.getFromVariablePart(index - object.instsize());
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class VariablePointersObjectWriteNode extends Node {

        public abstract void execute(VariablePointersObject object, long index, Object value);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final void doWriteCached(final VariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, cachedIndex, value);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()", replaces = "doWriteCached")
        protected static final void doWriteGeneric(final VariablePointersObject object, final long index, final Object value,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, index, value);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedIndex(final VariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            object.putIntoVariablePart(cachedIndex - cachedLayout.getInstSize(), value);
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doWriteIntoVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedLayout(final VariablePointersObject object, final long index, final Object value,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            object.putIntoVariablePart(index - cachedLayout.getInstSize(), value);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doWriteIntoVariablePartCachedIndex", "doWriteIntoVariablePartCachedLayout"})
        protected static final void doWriteIntoVariablePartGeneric(final VariablePointersObject object, final long index, final Object value) {
            object.putIntoVariablePart(index - object.instsize(), value);
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class WeakVariablePointersObjectReadNode extends Node {

        public abstract Object execute(WeakVariablePointersObject object, long index);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final Object doReadCached(final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, cachedIndex);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()", replaces = "doReadCached")
        protected static final Object doReadGeneric(final WeakVariablePointersObject object, final long index,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, index);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedIndex(final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final ConditionProfile weakRefProfile) {
            return object.getFromVariablePart(cachedIndex - cachedLayout.getInstSize(), weakRefProfile);
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doReadFromVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedLayout(final WeakVariablePointersObject object, final long index,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final ConditionProfile weakRefProfile) {
            return object.getFromVariablePart(index - cachedLayout.getInstSize(), weakRefProfile);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doReadFromVariablePartCachedIndex", "doReadFromVariablePartCachedLayout"})
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

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final void doWriteCached(final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, cachedIndex, value);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index < object.instsize()", replaces = "doWriteCached")
        protected static final void doWriteGeneric(final WeakVariablePointersObject object, final long index, final Object value,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, index, value);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedIndex(final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(cachedIndex - cachedLayout.getInstSize(), value, primitiveProfile);
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doWriteIntoVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedLayout(final WeakVariablePointersObject object, final long index, final Object value,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(index - cachedLayout.getInstSize(), value, primitiveProfile);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doWriteIntoVariablePartCachedIndex", "doWriteIntoVariablePartCachedLayout"})
        protected static final void doWriteIntoVariablePartGeneric(final WeakVariablePointersObject object, final long index, final Object value,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(index - object.instsize(), value, primitiveProfile);
        }
    }
}
