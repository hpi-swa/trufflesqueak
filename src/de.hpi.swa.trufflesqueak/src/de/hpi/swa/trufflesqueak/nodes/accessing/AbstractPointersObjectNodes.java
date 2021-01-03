/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;
import com.oracle.truffle.api.profiles.ConditionProfile;
import com.oracle.truffle.api.profiles.ValueProfile;

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

        public abstract Object execute(AbstractPointersObject obj, int index);

        public abstract long executeLong(AbstractPointersObject obj, int index);

        public abstract ArrayObject executeArray(AbstractPointersObject obj, int index);

        public abstract NativeObject executeNative(AbstractPointersObject obj, int index);

        public abstract PointersObject executePointers(AbstractPointersObject obj, int index);

        public final int executeInt(final AbstractPointersObject obj, final int index) {
            return MiscUtils.toIntExact(executeLong(obj, index));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final Object doReadCached(final AbstractPointersObject object, final int index,
                        @Cached("index") final int cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached("create(cachedLayout.getLocation(index), true)") final AbstractSlotLocationAccessorNode accessorNode) {
            return accessorNode.executeRead(object);
        }

        @TruffleBoundary
        @Specialization(guards = "object.getLayout().isValid(assumptionProfile)", replaces = "doReadCached", limit = "1")
        protected static final Object doReadUncached(final AbstractPointersObject object, final int index,
                        @SuppressWarnings("unused") @Shared("assmuptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile) {
            return object.getLayout().getLocation(index).read(object);
        }

        @Specialization(guards = "!object.getLayout().isValid(assumptionProfile)", limit = "1")
        protected static final Object doUpdateLayoutAndRead(final AbstractPointersObject object, final int index,
                        @SuppressWarnings("unused") @Shared("assmuptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile) {
            /* Note that this specialization does not replace the cached specialization. */
            CompilerDirectives.transferToInterpreter();
            object.updateLayout();
            return doReadUncached(object, index, assumptionProfile);
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

        public abstract void execute(AbstractPointersObject obj, int index, Object value);

        public final void executeNil(final AbstractPointersObject obj, final int index) {
            execute(obj, index, NilObject.SINGLETON);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final void doWriteCached(final AbstractPointersObject object, final int index,
                        final Object value,
                        @Cached("index") final int cachedIndex,
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
        @Specialization(guards = "object.getLayout().isValid(assumptionProfile)", replaces = "doWriteCached", limit = "1")
        protected static final void doWriteUncached(final AbstractPointersObject object, final int index, final Object value,
                        @SuppressWarnings("unused") @Shared("assmuptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile) {
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

        @Specialization(guards = "!object.getLayout().isValid(assumptionProfile)", limit = "1")
        protected static final void doUpdateLayoutAndWrite(final AbstractPointersObject object, final int index, final Object value,
                        @SuppressWarnings("unused") @Shared("assmuptionProfile") @Cached("createClassProfile()") final ValueProfile assumptionProfile) {
            /* Note that this specialization does not replace the cached specialization. */
            CompilerDirectives.transferToInterpreter();
            object.updateLayout();
            doWriteUncached(object, index, value, assumptionProfile);
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

        @Specialization(replaces = "doSizeCached")
        protected static final int doSizeUncached(final AbstractPointersObject object) {
            return object.getLayout().getInstSize();
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class VariablePointersObjectReadNode extends Node {

        public abstract Object execute(VariablePointersObject object, int index);

        public abstract ArrayObject executeArray(VariablePointersObject object, int index);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final Object doReadCached(final VariablePointersObject object, @SuppressWarnings("unused") final int index,
                        @Cached("index") final int cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, cachedIndex);
        }

        @Specialization(guards = "index < object.instsize()", replaces = "doReadCached")
        protected static final Object doRead(final VariablePointersObject object, final int index,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, index);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedIndex(final VariablePointersObject object, @SuppressWarnings("unused") final int index,
                        @Cached("index") final int cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return object.getFromVariablePart(cachedIndex - cachedLayout.getInstSize());
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doReadFromVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedLayout(final VariablePointersObject object, final int index,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return object.getFromVariablePart(index - cachedLayout.getInstSize());
        }

        @Specialization(guards = "index >= object.instsize()", replaces = {"doReadFromVariablePartCachedIndex", "doReadFromVariablePartCachedLayout"})
        protected static final Object doReadFromVariablePart(final VariablePointersObject object, final int index) {
            return object.getFromVariablePart(index - object.instsize());
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class VariablePointersObjectWriteNode extends Node {

        public abstract void execute(VariablePointersObject object, int index, Object value);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final void doWriteCached(final VariablePointersObject object, @SuppressWarnings("unused") final int index, final Object value,
                        @Cached("index") final int cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, cachedIndex, value);
        }

        @Specialization(guards = "index < object.instsize()", replaces = "doWriteCached")
        protected static final void doWrite(final VariablePointersObject object, final int index, final Object value,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, index, value);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedIndex(final VariablePointersObject object, @SuppressWarnings("unused") final int index, final Object value,
                        @Cached("index") final int cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            object.putIntoVariablePart(cachedIndex - cachedLayout.getInstSize(), value);
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doWriteIntoVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedLayout(final VariablePointersObject object, final int index, final Object value,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            object.putIntoVariablePart(index - cachedLayout.getInstSize(), value);
        }

        @Specialization(guards = "index >= object.instsize()", replaces = {"doWriteIntoVariablePartCachedIndex", "doWriteIntoVariablePartCachedLayout"})
        protected static final void doWriteIntoVariablePart(final VariablePointersObject object, final int index, final Object value) {
            object.putIntoVariablePart(index - object.instsize(), value);
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class WeakVariablePointersObjectReadNode extends Node {

        public abstract Object execute(WeakVariablePointersObject object, int index);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final Object doReadCached(final WeakVariablePointersObject object, @SuppressWarnings("unused") final int index,
                        @Cached("index") final int cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, cachedIndex);
        }

        @Specialization(guards = "index < object.instsize()", replaces = "doReadCached")
        protected static final Object doRead(final WeakVariablePointersObject object, final int index,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(object, index);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedIndex(final WeakVariablePointersObject object, @SuppressWarnings("unused") final int index,
                        @Cached("index") final int cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final ConditionProfile nilProfile) {
            return object.getFromVariablePart(cachedIndex - cachedLayout.getInstSize(), nilProfile);
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doReadFromVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCachedLayout(final WeakVariablePointersObject object, final int index,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final ConditionProfile nilProfile) {
            return object.getFromVariablePart(index - cachedLayout.getInstSize(), nilProfile);
        }

        @Specialization(guards = "index >= object.instsize()", replaces = {"doReadFromVariablePartCachedIndex", "doReadFromVariablePartCachedLayout"})
        protected static final Object doReadFromVariablePart(final WeakVariablePointersObject object, final int index,
                        @Cached final ConditionProfile nilProfile) {
            return object.getFromVariablePart(index - object.instsize(), nilProfile);
        }
    }

    @GenerateUncached
    @NodeInfo(cost = NodeCost.NONE)
    @ImportStatic(AbstractPointersObjectNodes.class)
    public abstract static class WeakVariablePointersObjectWriteNode extends Node {

        public abstract void execute(WeakVariablePointersObject object, int index, Object value);

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex < cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "CACHE_LIMIT")
        protected static final void doWriteCached(final WeakVariablePointersObject object, @SuppressWarnings("unused") final int index, final Object value,
                        @Cached("index") final int cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, cachedIndex, value);
        }

        @Specialization(guards = "index < object.instsize()", replaces = "doWriteCached")
        protected static final void doWrite(final WeakVariablePointersObject object, final int index, final Object value,
                        @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(object, index, value);
        }

        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout", "cachedIndex >= cachedLayout.getInstSize()"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "VARIABLE_PART_INDEX_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedIndex(final WeakVariablePointersObject object, @SuppressWarnings("unused") final int index, final Object value,
                        @Cached("index") final int cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final BranchProfile nilProfile,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(cachedIndex - cachedLayout.getInstSize(), value, nilProfile, primitiveProfile);
        }

        @Specialization(guards = {"object.getLayout() == cachedLayout", "index >= cachedLayout.getInstSize()"}, assumptions = "cachedLayout.getValidAssumption()", //
                        replaces = "doWriteIntoVariablePartCachedIndex", limit = "VARIABLE_PART_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCachedLayout(final WeakVariablePointersObject object, final int index, final Object value,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached final BranchProfile nilProfile,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(index - cachedLayout.getInstSize(), value, nilProfile, primitiveProfile);
        }

        @Specialization(guards = "index >= object.instsize()", replaces = {"doWriteIntoVariablePartCachedIndex", "doWriteIntoVariablePartCachedLayout"})
        protected static final void doWriteIntoVariablePart(final WeakVariablePointersObject object, final int index, final Object value,
                        @Cached final BranchProfile nilProfile,
                        @Cached final ConditionProfile primitiveProfile) {
            object.putIntoVariablePart(index - object.instsize(), value, nilProfile, primitiveProfile);
        }
    }
}
