/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.image.SqueakImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.SqueakImageWriter;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.BINDING;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.FRACTION;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.graal.squeak.nodes.accessing.UpdateSqueakObjectHashNode;
import de.hpi.swa.graal.squeak.util.ObjectGraphUtils.ObjectTracer;

public final class PointersObject extends AbstractPointersObject {

    public PointersObject(final SqueakImageContext image) {
        super(image); // for special PointersObjects only
    }

    public PointersObject(final SqueakImageContext image, final long hash, final ClassObject klass) {
        super(image, hash, klass);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    private PointersObject(final PointersObject original) {
        super(original);
    }

    public static PointersObject create(final AbstractPointersObjectWriteNode writeNode, final ClassObject squeakClass, final Object... pointers) {
        final PointersObject object = new PointersObject(squeakClass.image, squeakClass);
        for (int i = 0; i < pointers.length; i++) {
            writeNode.execute(object, i, pointers[i]);
        }
        return object;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final Object[] pointersObject = chunk.getPointers();
        initializeLayoutAndExtensionsUnsafe();
        for (int i = 0; i < pointersObject.length; i++) {
            writeNode.execute(this, i, pointersObject[i]);
        }
        assert size() == pointersObject.length;
        if (isProcess()) { /* Collect suspended contexts */
            final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
            final ContextObject suspendedContext = (ContextObject) readNode.execute(this, PROCESS.SUSPENDED_CONTEXT);
            chunk.getReader().getSuspendedContexts().put(this, suspendedContext);
        }
    }

    public void become(final PointersObject other) {
        becomeLayout(other);
    }

    public void pointersBecomeOneWay(final UpdateSqueakObjectHashNode updateHashNode, final Object[] from, final Object[] to, final boolean copyHash) {
        layoutValuesBecomeOneWay(updateHashNode, from, to, copyHash);
    }

    @Override
    public int size() {
        return instsize();
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final ConditionProfile isPrimitiveProfile, final Object thang) {
        return layoutValuesPointTo(identityNode, isPrimitiveProfile, thang);
    }

    public boolean isActiveProcess(final AbstractPointersObjectReadNode readNode) {
        return this == image.getActiveProcess(readNode);
    }

    public boolean isEmptyList(final AbstractPointersObjectReadNode readNode) {
        return readNode.execute(this, LINKED_LIST.FIRST_LINK) == NilObject.SINGLETON;
    }

    public boolean isDisplay() {
        return this == image.getSpecialObject(SPECIAL_OBJECT.THE_DISPLAY);
    }

    public boolean isPoint() {
        return getSqueakClass() == image.pointClass;
    }

    public boolean isProcess() {
        return getSqueakClass() == image.processClass;
    }

    public int[] getFormBits(final AbstractPointersObjectReadNode readNode) {
        return readNode.executeNative(this, FORM.BITS).getIntStorage();
    }

    public int getFormDepth(final AbstractPointersObjectReadNode readNode) {
        return (int) readNode.executeLong(this, FORM.DEPTH);
    }

    public int getFormHeight(final AbstractPointersObjectReadNode readNode) {
        return (int) readNode.executeLong(this, FORM.HEIGHT);
    }

    public int getFormWidth(final AbstractPointersObjectReadNode readNode) {
        return (int) readNode.executeLong(this, FORM.WIDTH);
    }

    public PointersObject removeFirstLinkOfList(final AbstractPointersObjectReadNode readNode, final AbstractPointersObjectWriteNode writeNode) {
        // Remove the first process from the given linked list.
        final PointersObject first = readNode.executePointers(this, LINKED_LIST.FIRST_LINK);
        final Object last = readNode.execute(this, LINKED_LIST.LAST_LINK);
        if (first == last) {
            writeNode.executeNil(this, LINKED_LIST.FIRST_LINK);
            writeNode.executeNil(this, LINKED_LIST.LAST_LINK);
        } else {
            writeNode.execute(this, LINKED_LIST.FIRST_LINK, readNode.execute(first, PROCESS.NEXT_LINK));
        }
        writeNode.executeNil(first, PROCESS.NEXT_LINK);
        return first;
    }

    public PointersObject shallowCopy() {
        return new PointersObject(this);
    }

    @Override
    public void tracePointers(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (isPoint()) {
            return readNode.execute(this, POINT.X) + "@" + readNode.execute(this, POINT.Y);
        }
        final String squeakClassName = getSqueakClass().getClassName();
        if ("Fraction".equals(squeakClassName)) {
            return readNode.execute(this, FRACTION.NUMERATOR) + " / " + readNode.execute(this, FRACTION.DENOMINATOR);
        }
        if ("Association".equals(squeakClassName)) {
            return readNode.execute(this, ASSOCIATION.KEY) + " -> " + readNode.execute(this, ASSOCIATION.VALUE);
        }
        final ClassObject superclass = getSqueakClass().getSuperclassOrNull();
        if (superclass != null && "Binding".equals(superclass.getClassName())) {
            return readNode.execute(this, BINDING.KEY) + " => " + readNode.execute(this, BINDING.VALUE);
        }
        return super.toString();
    }

    @Override
    public void write(final SqueakImageWriter writerNode) {
        super.writeHeaderAndLayoutObjects(writerNode);
    }
}
