/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.model;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnknownIdentifierException;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.InteropArray;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

@ExportLibrary(InteropLibrary.class)
@SuppressWarnings("static-method")
public final class ContextScope implements TruffleObject {
    private static final String SENDER = "sender";
    private static final String PC = "pc";
    private static final String STACKP = "stackp";
    private static final String METHOD = "method";
    private static final String CLOSURE_OR_NIL = "closureOrNil";
    private static final String RECEIVER = "receiver";
    private static final String[] ALL_FIELDS = {SENDER, PC, STACKP, METHOD, CLOSURE_OR_NIL, RECEIVER};

    private final Frame frame;

    public ContextScope(final Frame frame) {
        this.frame = frame;
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
            return FrameAccess.getInstructionPointer(frame);
        }
        if (STACKP.equals(member)) {
            return FrameAccess.getStackPointer(frame);
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
        try {
            final int index = Integer.parseInt(member);
            if (index >= getContextSize()) {
                throw UnknownIdentifierException.create(member);
            }
            return FrameAccess.getStackValue(frame, index);
        } catch (final NumberFormatException e) {
            LogUtils.INTEROP.log(Level.WARNING, "Invalid number format: " + member, e);
        }
        throw UnknownIdentifierException.create(member);
    }

    @ExportMessage
    @TruffleBoundary
    protected Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        final String[] members = new String[getContextSize()];
        System.arraycopy(ALL_FIELDS, 0, members, 0, ALL_FIELDS.length);
        for (int i = CONTEXT.TEMP_FRAME_START; i < members.length; i++) {
            members[i] = Integer.toString(i);
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
        try {
            return Integer.parseInt(member) < getContextSize();
        } catch (final NumberFormatException e) {
            return false;
        }
    }

    @ExportMessage
    protected boolean isMemberModifiable(@SuppressWarnings("unused") final String member) {
        return false; // TODO: allow modifications
    }

    @SuppressWarnings("unused")
    @ExportMessage
    protected void writeMember(final String member, final Object value) {
        // TODO: allow modifications
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
    protected Object getMetaObject(@CachedLibrary("this") final InteropLibrary lib) {
        return SqueakImageContext.get(lib).methodContextClass;
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

    private int getContextSize() {
        final CompiledCodeObject code;
        if (FrameAccess.hasClosure(frame)) {
            code = FrameAccess.getClosure(frame).getCompiledBlock();
        } else {
            code = FrameAccess.getCodeObject(frame);
        }
        return CONTEXT.TEMP_FRAME_START + code.getSqueakContextSize();
    }
}
