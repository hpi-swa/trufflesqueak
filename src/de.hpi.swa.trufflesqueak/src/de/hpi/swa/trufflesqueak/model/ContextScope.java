/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.InteropArray;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

@ExportLibrary(InteropLibrary.class)
@SuppressWarnings("static-method")
public final class ContextScope implements TruffleObject {
    private static final String SENDER = "sender";
    private static final String PC = "pc";
    private static final String STACKP = "stackp";
    private static final String METHOD = "method";
    private static final String CLOSURE_OR_NIL = "closureOrNil";
    private static final String RECEIVER = "receiver";
    private static final String[] ALL_FIELDS = new String[]{SENDER, PC, STACKP, METHOD, CLOSURE_OR_NIL, RECEIVER};

    private final Map<String, FrameSlot> slots = new HashMap<>();
    private final Frame frame;

    public ContextScope(final Frame frame) {
        this.frame = frame;
        final CompiledCodeObject code = FrameAccess.getMethodOrBlock(frame);
        for (final FrameSlot slot : code.getStackSlotsUnsafe()) {
            if (slot == null) {
                break;
            }
            slots.put(Objects.toString(slot.getIdentifier()), slot);
        }
    }

    @ExportMessage
    protected boolean hasLanguage() {
        return true;
    }

    @ExportMessage
    protected Class<? extends TruffleLanguage<?>> getLanguage() {
        return SqueakLanguage.class;
    }

    @ExportMessage
    protected boolean hasMembers() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    protected Object readMember(final String member) throws UnknownIdentifierException {
        if (frame == null) {
            return NilObject.SINGLETON;
        }
        if (SENDER.equals(member)) {
            return FrameAccess.getSender(frame);
        }
        if (PC.equals(member)) {
            return FrameAccess.getInstructionPointerSlow(frame);
        }
        if (STACKP.equals(member)) {
            return FrameAccess.getStackPointerSlow(frame);
        }
        if (METHOD.equals(member)) {
            return FrameAccess.getCodeObject(frame);
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
    protected Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
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
    protected boolean isMemberReadable(final String member) {
        for (final String field : ALL_FIELDS) {
            if (field.equals(member)) {
                return true;
            }
        }
        final FrameSlot slot = slots.get(member);
        if (slot == null) {
            return false;
        }
        final CompiledCodeObject code = FrameAccess.getMethodOrBlock(frame);
        int i = 0;
        for (final FrameSlot currentSlot : code.getStackSlotsUnsafe()) {
            if (currentSlot == slot) {
                return i < FrameAccess.getStackPointer(frame, code);
            }
            i++;
        }
        throw SqueakException.create("Unable to find slot");
    }

    @ExportMessage
    @TruffleBoundary
    protected boolean isMemberModifiable(final String member) {
        return slots.containsKey(member) && frame != null;
    }

    @ExportMessage
    @TruffleBoundary
    protected void writeMember(final String member, final Object value) throws UnknownIdentifierException, UnsupportedMessageException {
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
    protected boolean isMemberInsertable(@SuppressWarnings("unused") final String member) {
        return false;
    }

    @ExportMessage
    protected boolean hasMetaObject() {
        return true;
    }

    @ExportMessage
    protected Object getMetaObject(@CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.methodContextClass;
    }

    @ExportMessage
    protected boolean isScope() {
        return true;
    }

    @ExportMessage
    protected boolean hasScopeParent() {
        return FrameAccess.getSender(frame) instanceof ContextObject;
    }

    @ExportMessage
    protected Object getScopeParent() throws UnsupportedMessageException {
        if (hasScopeParent()) {
            return new ContextScope(FrameAccess.getSenderContext(frame).getTruffleFrame());
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @ExportMessage
    @TruffleBoundary
    protected Object toDisplayString(@SuppressWarnings("unused") final boolean allowSideEffects) {
        final CompiledCodeObject method = FrameAccess.getCodeObject(frame);
        final BlockClosureObject closure = FrameAccess.getClosure(frame);
        if (closure != null) {
            return "CTX [] in " + method;
        } else {
            return "CTX " + method;
        }
    }
}
