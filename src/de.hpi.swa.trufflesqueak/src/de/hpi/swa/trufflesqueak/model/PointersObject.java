/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakExceptionWrapper;
import de.hpi.swa.trufflesqueak.image.SqueakImageChunk;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageWriter;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.BINDING;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.EXCEPTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FRACTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.LINKED_LIST;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SPECIAL_OBJECT;
import de.hpi.swa.trufflesqueak.nodes.InheritsFromNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectIdentityNode;
import de.hpi.swa.trufflesqueak.util.ObjectGraphUtils.ObjectTracer;

@ExportLibrary(InteropLibrary.class)
public final class PointersObject extends AbstractPointersObject {

    public PointersObject(final SqueakImageContext image) {
        super(image); // for special PointersObjects only
    }

    public PointersObject(final SqueakImageContext image, final long hash, final ClassObject klass) {
        super(image, hash, klass);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject, final ObjectLayout layout) {
        super(image, classObject, layout);
    }

    public PointersObject(final SqueakImageContext image, final ClassObject classObject) {
        super(image, classObject);
    }

    private PointersObject(final PointersObject original) {
        super(original);
    }

    public static PointersObject newHandleWithHiddenObject(final SqueakImageContext image, final Object hiddenObject) {
        final PointersObject handle = new PointersObject(image, image.pointClass);
        handle.object2 = hiddenObject;
        return handle;
    }

    public Object getHiddenObject() {
        assert getSqueakClass() == image.pointClass && object2 != NilObject.SINGLETON : "Object not a handle with hidden object";
        return object2;
    }

    public void setHiddenObject(final Object value) {
        object2 = value;
    }

    @Override
    public void fillin(final SqueakImageChunk chunk) {
        final AbstractPointersObjectWriteNode writeNode = AbstractPointersObjectWriteNode.getUncached();
        final Object[] pointersObject = chunk.getPointers();
        fillInLayoutAndExtensions();
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

    public PointersObject shallowCopy() {
        return new PointersObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        layoutValuesBecomeOneWay(from, to, copyHash);
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
    public void write(final SqueakImageWriter writer) {
        super.writeHeaderAndLayoutObjects(writer);
    }

    /*
     * INTEROPERABILITY
     */

    @ExportMessage
    protected static boolean isException(final PointersObject receiver,
                    @Shared("inheritsFromNode") @Cached final InheritsFromNode inheritsFromNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return inheritsFromNode.execute(receiver, image.getExceptionClass());
    }

    @ExportMessage
    protected static RuntimeException throwException(final PointersObject receiver,
                    @Shared("inheritsFromNode") @Cached final InheritsFromNode inheritsFromNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws UnsupportedMessageException {
        if (isException(receiver, inheritsFromNode, image)) {
            throw new SqueakExceptionWrapper(receiver);
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    protected static ExceptionType getExceptionType(final PointersObject receiver,
                    @Shared("inheritsFromNode") @Cached final InheritsFromNode inheritsFromNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws UnsupportedMessageException {
        if (isException(receiver, inheritsFromNode, image)) {
            return ExceptionType.RUNTIME_ERROR;
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    protected static boolean isExceptionIncompleteSource(@SuppressWarnings("unused") final PointersObject receiver) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    protected static int getExceptionExitStatus(@SuppressWarnings("unused") final PointersObject receiver) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    @TruffleBoundary
    protected static boolean hasExceptionMessage(final PointersObject receiver,
                    @Shared("inheritsFromNode") @Cached final InheritsFromNode inheritsFromNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        if (isException(receiver, inheritsFromNode, image)) {
            final Object messageText = receiver.instVarAt0Slow(EXCEPTION.MESSAGE_TEXT);
            return messageText instanceof NativeObject && ((NativeObject) messageText).isString();
        } else {
            return false;
        }
    }

    @ExportMessage
    @TruffleBoundary
    protected static Object getExceptionMessage(final PointersObject receiver,
                    @Shared("inheritsFromNode") @Cached final InheritsFromNode inheritsFromNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) throws UnsupportedMessageException {
        if (isException(receiver, inheritsFromNode, image)) {
            final Object messageText = receiver.instVarAt0Slow(EXCEPTION.MESSAGE_TEXT);
            if (messageText instanceof NativeObject && ((NativeObject) messageText).isString()) {
                return messageText;
            }
        }
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    protected static boolean hasExceptionStackTrace(@SuppressWarnings("unused") final PointersObject receiver) {
        return false;
    }

    @ExportMessage
    protected static Object getExceptionStackTrace(@SuppressWarnings("unused") final PointersObject receiver) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create(); // TODO: expose Squeak stacktrace
    }
}
