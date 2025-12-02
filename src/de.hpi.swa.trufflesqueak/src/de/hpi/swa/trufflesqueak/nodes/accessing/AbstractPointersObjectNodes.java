/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
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
    @GenerateInline
    @GenerateUncached
    @GenerateCached(true)
    public abstract static class AbstractPointersObjectReadNode extends AbstractNode {

        public static AbstractPointersObjectReadNode getUncached() {
            return AbstractPointersObjectReadNodeGen.getUncached();
        }

        public abstract Object execute(Node node, AbstractPointersObject obj, long index);

        public static final Object executeUncached(final AbstractPointersObject obj, final long index) {
            return getUncached().execute(null, obj, index);
        }

        public abstract long executeLong(Node node, AbstractPointersObject obj, long index);

        public abstract ArrayObject executeArray(Node node, AbstractPointersObject obj, long index);

        public abstract NativeObject executeNative(Node node, AbstractPointersObject obj, long index);

        public abstract PointersObject executePointers(Node node, AbstractPointersObject obj, long index);

        public final int executeInt(final Node node, final AbstractPointersObject obj, final long index) {
            return MiscUtils.toIntExact(executeLong(node, obj, index));
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadCached(final AbstractPointersObject object, final long index,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached("create(cachedLayout.getLocation(index), true)") final AbstractSlotLocationAccessorNode accessorNode) {
            return accessorNode.executeRead(object);
        }

        @TruffleBoundary
        @Specialization(guards = "!object.getLayout().isValid()", excludeForUncached = true)
        protected static final Object doReadInvalid(final AbstractPointersObject object, final long index) {
            object.updateLayout(); // ensure layout is updated
            return doReadGeneric(object, index);
        }

        @TruffleBoundary
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = {"doReadCached", "doReadInvalid"})
        protected static final Object doReadGeneric(final AbstractPointersObject object, final long index) {
            return object.getLayout().getLocation(index).read(object);
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(true)
    public abstract static class AbstractPointersObjectWriteNode extends AbstractNode {

        public static AbstractPointersObjectWriteNode getUncached() {
            return AbstractPointersObjectWriteNodeGen.getUncached();
        }

        public abstract void execute(Node node, AbstractPointersObject obj, long index, Object value);

        public static final void executeUncached(final AbstractPointersObject obj, final long index, final Object value) {
            getUncached().execute(null, obj, index, value);
        }

        public final void executeNil(final Node node, final AbstractPointersObject obj, final long index) {
            execute(node, obj, index, NilObject.SINGLETON);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = {"cachedIndex == index", "object.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteCached(final AbstractPointersObject object, final long index,
                        final Object value,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Cached("create(cachedLayout.getLocation(index), false)") final AbstractSlotLocationAccessorNode accessorNode) {
            if (accessorNode.canStore(value)) {
                try {
                    accessorNode.executeWrite(object, value);
                } catch (final IllegalWriteException e) {
                    throw CompilerDirectives.shouldNotReachHere("write must succeed", e);
                }
            } else {
                /*
                 * Update layout in interpreter if it is not stable yet. This will also invalidate
                 * the assumption and therefore this particular instance of the specialization will
                 * be removed from the cache and replaced by an updated version.
                 */
                transferToInterpreterUpdateLocationAndWrite(object, index, value);
            }
        }

        @TruffleBoundary
        @Specialization(guards = "!object.getLayout().isValid()")
        protected static final void doWriteInvalid(final AbstractPointersObject object, final long index, final Object value) {
            object.updateLayout();
            doWriteGeneric(object, index, value);
        }

        @TruffleBoundary
        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = {"doWriteCached", "doWriteInvalid"})
        protected static final void doWriteGeneric(final AbstractPointersObject object, final long index, final Object value) {
            try {
                object.getLayout().getLocation(index).write(object, value);
            } catch (final IllegalWriteException e) {
                /*
                 * Although the layout was valid, it is possible that the location cannot store the
                 * value. Generialize location in the interpreter.
                 */
                transferToInterpreterUpdateLocationAndWrite(object, index, value);
            }
        }

        private static void transferToInterpreterUpdateLocationAndWrite(final AbstractPointersObject object, final long index, final Object value) {
            CompilerDirectives.transferToInterpreter();
            object.updateLayout(index, value);
            object.getLayout().getLocation(index).writeMustSucceed(object, value);
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class AbstractPointersObjectInstSizeNode extends AbstractNode {
        public abstract int execute(Node node, AbstractPointersObject obj);

        @Specialization(guards = {"object.getLayout() == cachedLayout"}, assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final int doSizeCached(@SuppressWarnings("unused") final AbstractPointersObject object,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return cachedLayout.getInstSize();
        }

        @TruffleBoundary
        @Specialization(guards = "!object.getLayout().isValid()")
        protected static final int doSizeInvalid(final AbstractPointersObject object) {
            object.updateLayout(); // ensure layout is updated
            return doSizeGeneric(object);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(replaces = {"doSizeCached", "doSizeInvalid"})
        protected static final int doSizeGeneric(final AbstractPointersObject object) {
            return object.getLayout().getInstSize();
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class VariablePointersObjectReadNode extends AbstractNode {

        public abstract Object execute(Node node, VariablePointersObject object, long index);

        @Specialization(guards = {"cachedIndex < cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadCached(final Node node, final VariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Exclusive @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(node, object, cachedIndex);
        }

        @TruffleBoundary
        @Specialization(guards = {"index < object.instsize()", "!object.getLayout().isValid()"})
        protected static final Object doReadInvalid(final Node node, final VariablePointersObject object, final long index,
                        @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode) {
            object.updateLayout(); // ensure layout is updated
            return doReadGeneric(node, object, index, readNode);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"index < object.instsize()"}, replaces = {"doReadCached", "doReadInvalid"})
        protected static final Object doReadGeneric(final Node node, final VariablePointersObject object, final long index,
                        @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(node, object, index);
        }

        @Specialization(guards = {"cachedIndex >= cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_VARIABLE_PART_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCached(final VariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            return object.getFromVariablePart(cachedIndex - cachedLayout.getInstSize());
        }

        @Specialization(guards = {"index >= object.instsize()", "!object.getLayout().isValid()"})
        protected static final Object doReadFromVariablePartInvalid(final VariablePointersObject object, final long index) {
            object.updateLayout(); // ensure layout is updated
            return doReadFromVariablePartGeneric(object, index);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doReadFromVariablePartCached", "doReadFromVariablePartInvalid"})
        protected static final Object doReadFromVariablePartGeneric(final VariablePointersObject object, final long index) {
            return object.getFromVariablePart(index - object.instsize());
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class VariablePointersObjectWriteNode extends AbstractNode {

        public abstract void execute(Node node, VariablePointersObject object, long index, Object value);

        @Specialization(guards = {"cachedIndex < cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteCached(final Node node, final VariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(node, object, cachedIndex, value);
        }

        @TruffleBoundary
        @Specialization(guards = {"index < object.instsize()", "!object.getLayout().isValid()"})
        protected static final void doWriteInvalid(final Node node, final VariablePointersObject object, final long index, final Object value,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            object.updateLayout(); // ensure layout is updated
            doWriteGeneric(node, object, index, value, writeNode);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"index < object.instsize()"}, replaces = {"doWriteCached", "doWriteInvalid"})
        protected static final void doWriteGeneric(final Node node, final VariablePointersObject object, final long index, final Object value,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(node, object, index, value);
        }

        @Specialization(guards = {"cachedIndex >= cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_VARIABLE_PART_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCached(final VariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout) {
            object.putIntoVariablePart(cachedIndex - cachedLayout.getInstSize(), value);
        }

        @Specialization(guards = {"index >= object.instsize()", "!object.getLayout().isValid()"})
        protected static final void doReadFromVariablePartInvalid(final VariablePointersObject object, final long index, final Object value) {
            object.updateLayout(); // ensure layout is updated
            doWriteIntoVariablePartGeneric(object, index, value);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doWriteIntoVariablePartCached", "doReadFromVariablePartInvalid"})
        protected static final void doWriteIntoVariablePartGeneric(final VariablePointersObject object, final long index, final Object value) {
            object.putIntoVariablePart(index - object.instsize(), value);
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class WeakVariablePointersObjectReadNode extends AbstractNode {

        public abstract Object execute(Node node, WeakVariablePointersObject object, long index);

        @Specialization(guards = {"cachedIndex < cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final Object doReadCached(final Node node, final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Exclusive @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(node, object, cachedIndex);
        }

        @TruffleBoundary
        @Specialization(guards = {"index < object.instsize()", "!object.getLayout().isValid()"})
        protected static final Object doReadInvalid(final Node node, final WeakVariablePointersObject object, final long index,
                        @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode) {
            object.updateLayout(); // ensure layout is updated
            return doReadGeneric(node, object, index, readNode);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"index < object.instsize()"}, replaces = {"doReadCached", "doReadInvalid"})
        protected static final Object doReadGeneric(final Node node, final WeakVariablePointersObject object, final long index,
                        @Shared("readNode") @Cached final AbstractPointersObjectReadNode readNode) {
            return readNode.execute(node, object, index);
        }

        @Specialization(guards = {"cachedIndex >= cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_VARIABLE_PART_CACHE_LIMIT")
        protected static final Object doReadFromVariablePartCached(final Node node, final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Exclusive @Cached final InlinedConditionProfile weakRefProfile) {
            return object.getFromWeakVariablePart(node, cachedIndex - cachedLayout.getInstSize(), weakRefProfile);
        }

        @Specialization(guards = {"index >= object.instsize()", "!object.getLayout().isValid()"})
        protected static final Object doReadFromVariablePartInvalid(final Node node, final WeakVariablePointersObject object, final long index,
                        @Shared("weakRefProfile") @Cached final InlinedConditionProfile weakRefProfile) {
            object.updateLayout(); // ensure layout is updated
            return doReadFromVariablePartGeneric(node, object, index, weakRefProfile);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doReadFromVariablePartCached", "doReadFromVariablePartInvalid"})
        protected static final Object doReadFromVariablePartGeneric(final Node node, final WeakVariablePointersObject object, final long index,
                        @Shared("weakRefProfile") @Cached final InlinedConditionProfile weakRefProfile) {
            return object.getFromWeakVariablePart(node, index - object.instsize(), weakRefProfile);
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    public abstract static class WeakVariablePointersObjectWriteNode extends AbstractNode {

        public abstract void execute(Node node, WeakVariablePointersObject object, long index, Object value);

        @Specialization(guards = {"cachedIndex < cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_LAYOUT_CACHE_LIMIT")
        protected static final void doWriteCached(final Node node, final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Cached("index") final long cachedIndex,
                        @SuppressWarnings("unused") @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Exclusive @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(node, object, cachedIndex, value);
        }

        @TruffleBoundary
        @Specialization(guards = {"index < object.instsize()", "!object.getLayout().isValid()"})
        protected static final void doWriteInvalid(final Node node, final WeakVariablePointersObject object, final long index, final Object value,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            object.updateLayout(); // ensure layout is updated
            doWriteGeneric(node, object, index, value, writeNode);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = {"index < object.instsize()"}, replaces = {"doWriteCached", "doWriteInvalid"})
        protected static final void doWriteGeneric(final Node node, final WeakVariablePointersObject object, final long index, final Object value,
                        @Shared("writeNode") @Cached final AbstractPointersObjectWriteNode writeNode) {
            writeNode.execute(node, object, index, value);
        }

        @Specialization(guards = {"cachedIndex >= cachedLayout.getInstSize()", "cachedIndex == index", "object.getLayout() == cachedLayout"}, //
                        assumptions = "cachedLayout.getValidAssumption()", limit = "POINTERS_VARIABLE_PART_CACHE_LIMIT")
        protected static final void doWriteIntoVariablePartCached(final Node node, final WeakVariablePointersObject object, @SuppressWarnings("unused") final long index, final Object value,
                        @Bind final SqueakImageContext image,
                        @Cached("index") final long cachedIndex,
                        @Cached("object.getLayout()") final ObjectLayout cachedLayout,
                        @Exclusive @Cached final InlinedConditionProfile primitiveProfile) {
            object.putIntoWeakVariablePart(node, cachedIndex - cachedLayout.getInstSize(), value, image, primitiveProfile);
        }

        @Specialization(guards = {"index >= object.instsize()", "!object.getLayout().isValid()"})
        protected static final void doWriteIntoVariablePartInvalid(final Node node, final WeakVariablePointersObject object, final long index, final Object value,
                        @Bind final SqueakImageContext image,
                        @Shared("weakRefProfile") @Cached final InlinedConditionProfile weakRefProfile) {
            object.updateLayout(); // ensure layout is updated
            doWriteIntoVariablePartGeneric(node, object, index, value, image, weakRefProfile);
        }

        @ReportPolymorphism.Megamorphic
        @Specialization(guards = "index >= object.instsize()", replaces = {"doWriteIntoVariablePartCached", "doWriteIntoVariablePartInvalid"})
        protected static final void doWriteIntoVariablePartGeneric(final Node node, final WeakVariablePointersObject object, final long index, final Object value,
                        @Bind final SqueakImageContext image,
                        @Shared("weakRefProfile") @Cached final InlinedConditionProfile primitiveProfile) {
            object.putIntoWeakVariablePart(node, index - object.instsize(), value, image, primitiveProfile);
        }
    }
}
