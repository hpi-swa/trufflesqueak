/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ExportLibrary(InteropLibrary.class)
public final class ContextObjectInfo implements TruffleObject {
    private static final String SENDER = "sender";
    private static final String PC = "pc";
    private static final String STACKP = "stackp";
    private static final String METHOD = "method";
    private static final String CLOSURE_OR_NIL = "closureOrNil";
    private static final String RECEIVER = "receiver";
    private static final String[] ALL_FIELDS = new String[]{SENDER, PC, STACKP, METHOD, CLOSURE_OR_NIL, RECEIVER};

    private final Frame frame;

    public ContextObjectInfo(final Frame frame) {
        this.frame = frame;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    public Object readMember(final String member) throws UnknownIdentifierException {
        if (frame == null) {
            return NilObject.SINGLETON;
        }
        if (SENDER.equals(member)) {
            return FrameAccess.getSender(frame);
        }
        if (PC.equals(member)) {
            return FrameAccess.getInstructionPointer(frame, FrameAccess.getBlockOrMethod(frame));
        }
        if (STACKP.equals(member)) {
            return FrameAccess.getStackPointer(frame, FrameAccess.getBlockOrMethod(frame));
        }
        if (METHOD.equals(member)) {
            return FrameAccess.getMethod(frame);
        }
        if (CLOSURE_OR_NIL.equals(member)) {
            return NilObject.nullToNil(FrameAccess.getClosure(frame));
        }
        if (RECEIVER.equals(member)) {
            return FrameAccess.getReceiver(frame);
        }
        try {
            final int stackIndex = Integer.parseInt(member);
            return FrameAccess.getStackSlow(frame)[stackIndex];
        } catch (final NumberFormatException e) {
            throw UnknownIdentifierException.create(member);
        }
    }

    @ExportMessage
    @TruffleBoundary
    public Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        final int stackPointer = FrameAccess.getStackPointerSlow(frame);
        final String[] members = new String[ALL_FIELDS.length + stackPointer];
        System.arraycopy(ALL_FIELDS, 0, members, 0, ALL_FIELDS.length);
        final int offset = ALL_FIELDS.length;
        for (int i = 0; i < stackPointer; i++) {
            members[offset + i] = Integer.toString(i);
        }
        return new InteropArray(members);
    }

    @ExportMessage
    @TruffleBoundary
    public boolean isMemberReadable(final String member) {
        for (final String field : ALL_FIELDS) {
            if (field.equals(member)) {
                return true;
            }
        }
        return isInStackRange(member);
    }

    @ExportMessage
    @TruffleBoundary
    public boolean isMemberModifiable(final String member) {
        if (frame == null) {
            return false;
        }
        return isInStackRange(member);
    }

    private boolean isInStackRange(final String member) {
        try {
            return Integer.parseInt(member) < FrameAccess.getStackPointerSlow(frame);
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    @ExportMessage
    @TruffleBoundary
    public void writeMember(final String member, final Object value) throws UnknownIdentifierException, UnsupportedMessageException {
        if (frame == null) {
            throw UnsupportedMessageException.create();
        }
        try {
            final int stackIndex = Integer.parseInt(member);
            FrameAccess.getStackSlow(frame)[stackIndex] = value;
        } catch (final NumberFormatException e) {
            throw UnknownIdentifierException.create(member);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isMemberInsertable(@SuppressWarnings("unused") final String member) {
        return false;
    }
}
