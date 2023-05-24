/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.object.DynamicObjectLibrary;
import com.oracle.truffle.api.object.HiddenKey;
import com.oracle.truffle.api.object.Shape;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.BINDING;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FRACTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

public final class PointersObject extends AbstractPointersObject {
    private static final HiddenKey HIDDEN_KEY = new HiddenKey("Hidden Object");

    public PointersObject() {
        super(); // for special PointersObjects only
    }

    public PointersObject(final long header, final ClassObject klass) {
        super(header, klass);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject, final Shape shape) {
        super(image, classObject, shape);
    }

    private PointersObject(final PointersObject original, final DynamicObjectLibrary lib) {
        super(original, lib);
    }

    public static PointersObject newHandleWithHiddenObject(final SqueakImageContext image, final Object hiddenObject) {
        final PointersObject handle = new PointersObject(image, image.pointClass, image.pointClass.getRootShape());
        handle.setHiddenObject(hiddenObject);
        return handle;
    }

    public Object getHiddenObject() {
        assert SqueakImageContext.getSlow().isPointClass(getSqueakClass()) : "Object cannot be a handle with hidden object";
        return DynamicObjectLibrary.getUncached().getOrDefault(this, HIDDEN_KEY, NilObject.SINGLETON);
    }

    public void setHiddenObject(final Object value) {
        DynamicObjectLibrary.getUncached().put(this, HIDDEN_KEY, value);
    }

    @Override
    protected void fillInVariablePart(final Object[] pointers, final int instSize) {
        // No variable part to fill in
        assert pointers.length == instSize : "Unexpected number of pointers found for " + this;
    }

    public void become(final PointersObject other) {
        becomeLayout(other);
    }

    @Override
    public int size() {
        return instsize();
    }

    public boolean pointsTo(final SqueakObjectIdentityNode identityNode, final Object thang) {
        return layoutValuesPointTo(identityNode, thang);
    }

    public boolean isEmptyList(final AbstractPointersObjectReadNode readNode) {
        return readNode.execute(this, LINKED_LIST.FIRST_LINK) == NilObject.SINGLETON;
    }

    public boolean isDisplay(final SqueakImageContext image) {
        return this == image.getSpecialObject(SPECIAL_OBJECT.THE_DISPLAY);
    }

    public int[] getFormBits(final AbstractPointersObjectReadNode readNode) {
        return readNode.executeNative(this, FORM.BITS).getIntStorage();
    }

    public int getFormDepth(final AbstractPointersObjectReadNode readNode) {
        return readNode.executeInt(this, FORM.DEPTH);
    }

    public int getFormHeight(final AbstractPointersObjectReadNode readNode) {
        return readNode.executeInt(this, FORM.HEIGHT);
    }

    public PointersObject getFormOffset(final AbstractPointersObjectReadNode readNode) {
        return readNode.executePointers(this, FORM.OFFSET);
    }

    public int getFormWidth(final AbstractPointersObjectReadNode readNode) {
        return readNode.executeInt(this, FORM.WIDTH);
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

    public PointersObject shallowCopy(final DynamicObjectLibrary lib) {
        return new PointersObject(this, lib);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to) {
        layoutValuesBecomeOneWay(from, to);
    }

    @Override
    protected void traceVariablePart(final ObjectTracer tracer) {
        // nothing to do
    }

    @Override
    protected void traceVariablePart(final SqueakImageWriter writer) {
        // nothing to do
    }

    @Override
    protected void writeVariablePart(final SqueakImageWriter writer) {
        // nothing to do
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        final AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.getUncached();
        final ClassObject classObject = getSqueakClass();
        if (classObject.getImage().isPointClass(classObject)) {
            return readNode.execute(this, POINT.X) + "@" + readNode.execute(this, POINT.Y);
        }
        final String squeakClassName = classObject.getClassName();
        if ("Fraction".equals(squeakClassName)) {
            return readNode.execute(this, FRACTION.NUMERATOR) + " / " + readNode.execute(this, FRACTION.DENOMINATOR);
        }
        if ("Association".equals(squeakClassName)) {
            return readNode.execute(this, ASSOCIATION.KEY) + " -> " + readNode.execute(this, ASSOCIATION.VALUE);
        }
        final ClassObject superclass = classObject.getSuperclassOrNull();
        if (superclass != null && "Binding".equals(superclass.getClassName())) {
            return readNode.execute(this, BINDING.KEY) + " => " + readNode.execute(this, BINDING.VALUE);
        }
        return super.toString();
    }
}
