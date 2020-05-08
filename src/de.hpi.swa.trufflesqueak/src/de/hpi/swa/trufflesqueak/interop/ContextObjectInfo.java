/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@ExportLibrary(InteropLibrary.class)
public final class ContextObjectInfo implements TruffleObject {
    private static final String SENDER = "sender";
    private static final String PC = "pc";
    private static final String STACKP = "stackp";
    private static final String METHOD = "method";
    private static final String CLOSURE_OR_NIL = "closureOrNil";
    private static final String RECEIVER = "receiver";
    private static final String[] ALL_FIELDS = new String[]{SENDER, PC, STACKP, METHOD, CLOSURE_OR_NIL, RECEIVER};

    private final Map<String, FrameSlot> slots = new HashMap<>();
    private final Frame frame;

    public ContextObjectInfo(final Frame frame) {
        this.frame = frame;
        final CompiledCodeObject code = FrameAccess.getBlockOrMethod(frame);
        for (final FrameSlot slot : code.getStackSlotsUnsafe()) {
            if (slot == null) {
                break;
            }
            slots.put(Objects.toString(slot.getIdentifier()), slot);
        }
    }

    public static boolean isInstance(final TruffleObject obj) {
        return obj instanceof ContextObjectInfo;
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
        final FrameSlot slot = slots.get(member);
        if (slot == null) {
            throw UnknownIdentifierException.create(member);
        } else {
            return Objects.requireNonNull(frame.getValue(slot));
        }
    }

    @ExportMessage
    @TruffleBoundary
    public Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        final String[] members = new String[ALL_FIELDS.length + slots.size()];
        System.arraycopy(ALL_FIELDS, 0, members, 0, ALL_FIELDS.length);
        int index = ALL_FIELDS.length;
        for (final String key : slots.keySet()) {
            members[index++] = key;
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
        final FrameSlot slot = slots.get(member);
        if (slot == null) {
            return false;
        }
        final CompiledCodeObject blockOrMethod = FrameAccess.getBlockOrMethod(frame);
        int i = 0;
        for (final FrameSlot currentSlot : blockOrMethod.getStackSlotsUnsafe()) {
            if (currentSlot == slot) {
                return i < FrameAccess.getStackPointer(frame, blockOrMethod);
            }
            i++;
        }
        throw SqueakException.create("Unable to find slot");
    }

    @ExportMessage
    @TruffleBoundary
    public boolean isMemberModifiable(final String member) {
        return slots.containsKey(member) && frame != null;
    }

    @ExportMessage
    @TruffleBoundary
    public void writeMember(final String member, final Object value) throws UnknownIdentifierException, UnsupportedMessageException {
        if (frame == null) {
            throw UnsupportedMessageException.create();
        }
        final FrameSlot slot = slots.get(member);
        if (slot != null) {
            FrameAccess.setStackSlot(frame, slot, value);
        } else {
            throw UnknownIdentifierException.create(member);
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isMemberInsertable(@SuppressWarnings("unused") final String member) {
        return false;
    }
}
