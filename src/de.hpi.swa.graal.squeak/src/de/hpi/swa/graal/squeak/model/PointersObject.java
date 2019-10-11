/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.graal.squeak.nodes.ObjectGraphNode.ObjectTracer;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

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
    }

    public Object instVarAt0Unsafe(final int index) {
        CompilerAsserts.neverPartOfCompilation();
        assert index < instsize() && getLayout().isValid() : "Invalid unsafe instVar access";
        return getLayout().getLocation(index).read(this);
    }

    public void instVarAtPut0Unsafe(final int index, final Object value) {
        CompilerAsserts.neverPartOfCompilation();
        getLayout().getLocation(index).write(this, value);
    }

    @Override
    public int size() {
        return instsize();
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

    public void traceObjects(final ObjectTracer tracer) {
        super.traceLayoutObjects(tracer);
    }
}
