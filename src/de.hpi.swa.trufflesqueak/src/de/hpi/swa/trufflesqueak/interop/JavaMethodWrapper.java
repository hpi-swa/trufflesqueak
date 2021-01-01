/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

@ExportLibrary(InteropLibrary.class)
final class JavaMethodWrapper implements TruffleObject {
    private static final String NAME_MEMBER = "name";
    private static final String RECEIVER_MEMBER = "receiver";
    private static final String PARAMETER_COUNT = "parameterCount";
    private static final InteropArray MEMBERS = new InteropArray(new String[]{NAME_MEMBER, RECEIVER_MEMBER, PARAMETER_COUNT});
    private final Object receiver;
    private final Method method;

    JavaMethodWrapper(final Object receiver, final Method method) {
        this.receiver = receiver;
        this.method = method;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    protected boolean hasMembers() {
        return true;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    @TruffleBoundary
    protected boolean isMemberReadable(final String member) {
        return NAME_MEMBER.equals(member) || RECEIVER_MEMBER.equals(member) || PARAMETER_COUNT.equals(member);
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    @ExportMessage(name = "isMemberInsertable")
    @ExportMessage(name = "isMemberInvocable")
    protected boolean isMemberModifiable(@SuppressWarnings("unused") final String member) {
        return false;
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    protected Object getMembers(@SuppressWarnings("unused") final boolean includeInternal) {
        return MEMBERS;
    }

    @SuppressWarnings({"static-method", "unused"})
    @ExportMessage
    protected void writeMember(final String member, final Object value) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @SuppressWarnings({"static-method", "unused"})
    @ExportMessage
    protected Object invokeMember(final String member, final Object[] arguments) throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    @TruffleBoundary
    protected Object readMember(final String member) throws UnsupportedMessageException {
        if (NAME_MEMBER.equals(member)) {
            return method.getName();
        } else if (RECEIVER_MEMBER.equals(member)) {
            return receiver;
        } else if (PARAMETER_COUNT.equals(member)) {
            return method.getParameterCount();
        } else {
            throw UnsupportedMessageException.create();
        }
    }

    @SuppressWarnings("static-method")
    @ExportMessage
    protected boolean isExecutable() {
        return true;
    }

    @ExportMessage
    @TruffleBoundary
    protected Object execute(final Object[] arguments) throws UnsupportedMessageException {
        try {
            return JavaObjectWrapper.wrap(method.invoke(receiver, arguments));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
            throw UnsupportedMessageException.create();
        }
    }
}
