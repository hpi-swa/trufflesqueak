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
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.ObjectGraphNode.ObjectTracer;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.graal.squeak.nodes.accessing.UpdateSqueakObjectHashNode;

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
            chunk.getReader().getSuspendedContexts().put(this, getSuspendedContext());
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

    public boolean isAssociation() {
        return getSqueakClass() == image.associationClass;
    }

    public boolean isActiveProcess() {
        return this == image.getActiveProcess();
    }

    public boolean isEmptyList() {
        return image.getFirstLink(this) == NilObject.SINGLETON;
    }

    public boolean isDisplay() {
        return this == image.getSpecialObject(SPECIAL_OBJECT.THE_DISPLAY);
    }

    public boolean isFraction() {
        return getSqueakClass() == image.fractionClass;
    }

    public boolean isPoint() {
        return getSqueakClass() == image.pointClass;
    }

    public AbstractSqueakObject getEffectiveProcess() {
        return image.getEffectiveProcess(this);
    }

    public long getExcessSignals() {
        return image.getExcessSignals(this);
    }

    public AbstractSqueakObject getFirstLink() {
        return image.getFirstLink(this);
    }

    public AbstractSqueakObject getLastLink() {
        return image.getLastLink(this);
    }

    public AbstractSqueakObject getMyList() {
        return image.getMyList(this);
    }

    public AbstractSqueakObject getNextLink() {
        return image.getNextLink(this);
    }

    public long getPriority() {
        return image.getPriority(this);
    }

    public AbstractSqueakObject getSuspendedContext() {
        return image.getSuspendedContext(this);
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

    public PointersObject removeFirstLinkOfList(final AbstractPointersObjectWriteNode writeNode) {
        // Remove the first process from the given linked list.
        final PointersObject first = (PointersObject) image.getFirstLink(this);
        final Object last = image.getLastLink(this);
        if (first == last) {
            writeNode.executeNil(this, LINKED_LIST.FIRST_LINK);
            writeNode.executeNil(this, LINKED_LIST.LAST_LINK);
        } else {
            writeNode.execute(this, LINKED_LIST.FIRST_LINK, image.getNextLink(first));
        }
        writeNode.executeNil(first, PROCESS.NEXT_LINK);
        return first;
    }

    public PointersObject shallowCopy() {
        return new PointersObject(this);
    }

    public void traceObjects(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        if (isPoint()) {
            return readNode.execute(this, 0) + "@" + readNode.execute(this, 1);
        }
        if (isFraction()) {
            return readNode.execute(this, 0) + " / " + readNode.execute(this, 1);
        }
        if (isAssociation()) {
            return readNode.execute(this, 0) + " -> " + readNode.execute(this, 1);
        }
        return super.toString();
    }

    @Override
    public void write(final SqueakImageWriter writerNode) {
        super.writeHeaderAndLayoutObjects(writerNode);
    }
}
