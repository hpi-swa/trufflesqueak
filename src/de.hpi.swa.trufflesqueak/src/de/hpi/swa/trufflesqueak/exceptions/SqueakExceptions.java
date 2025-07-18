/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.exception.AbstractTruffleException;
import com.oracle.truffle.api.interop.ExceptionType;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.EXCEPTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SYNTAX_ERROR_NOTIFICATION;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.DebugUtils;

public final class SqueakExceptions {

    @SuppressWarnings("static-method")
    @ExportLibrary(InteropLibrary.class)
    protected abstract static class AbstractSqueakException extends AbstractTruffleException {
        private static final long serialVersionUID = 1L;

        protected AbstractSqueakException() {
        }

        protected AbstractSqueakException(final String message) {
            super(message);
        }

        protected AbstractSqueakException(final Node node) {
            super(node);
        }

        protected AbstractSqueakException(final String message, final Node location) {
            super(message, location);
        }

        @ExportMessage
        @TruffleBoundary
        protected final Object toDisplayString(@SuppressWarnings("unused") final boolean allowSideEffects) {
            return toString();
        }

        @ExportMessage
        protected final boolean hasLanguage() {
            return true;
        }

        @ExportMessage
        protected final Class<? extends TruffleLanguage<?>> getLanguage() {
            return SqueakLanguage.class;
        }
    }

    public static final class SqueakException extends AbstractSqueakException {
        private static final long serialVersionUID = 1L;

        public SqueakException(final String message, final Node location) {
            super(message, location);
        }

        private SqueakException(final Object... messageParts) {
            super(ArrayUtils.toJoinedString(" ", messageParts));
            DebugUtils.printSqStackTrace();
        }

        public static SqueakException create(final Object... messageParts) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakException(messageParts);
        }
    }

    @SuppressWarnings("static-method")
    @ExportLibrary(InteropLibrary.class)
    public static final class SqueakSyntaxError extends AbstractSqueakException {
        private static final long serialVersionUID = 1L;
        private final transient SourceSection sourceSection;

        public SqueakSyntaxError(final PointersObject syntaxErrorNotification) {
            super(((NativeObject) syntaxErrorNotification.instVarAt0Slow(SYNTAX_ERROR_NOTIFICATION.ERROR_MESSAGE)).asStringUnsafe());
            final int sourceOffset = (int) ((long) syntaxErrorNotification.instVarAt0Slow(SYNTAX_ERROR_NOTIFICATION.LOCATION) - 1);
            sourceSection = SqueakImageContext.getSlow().getLastParseRequestSource().createSection(Math.max(sourceOffset - 1, 0), 1);
        }

        @TruffleBoundary
        public SqueakSyntaxError(final String message, final int position, final String source) {
            super("Syntax Error: \"" + message + "\" at position " + position);
            sourceSection = Source.newBuilder(SqueakLanguageConfig.ID, source, "<syntax error>").build().createSection(Math.max(position - 1, 0), 1);
        }

        @ExportMessage
        protected ExceptionType getExceptionType() {
            return ExceptionType.PARSE_ERROR;
        }

        @ExportMessage
        protected boolean isExceptionIncompleteSource() {
            return true;
        }

        @ExportMessage
        protected boolean hasSourceLocation() {
            return true;
        }

        @ExportMessage(name = "getSourceLocation")
        protected SourceSection getSourceSection() {
            return sourceSection;
        }
    }

    @ExportLibrary(InteropLibrary.class)
    public static final class SqueakQuit extends AbstractSqueakException {
        private static final long serialVersionUID = 1L;
        private final int exitStatus;

        public SqueakQuit(final Node location, final int exitStatus) {
            super(location);
            this.exitStatus = exitStatus;
        }

        @ExportMessage
        @SuppressWarnings("static-method")
        protected ExceptionType getExceptionType() {
            return ExceptionType.EXIT;
        }

        @ExportMessage
        protected int getExceptionExitStatus() {
            return exitStatus;
        }
    }

    @ExportLibrary(value = InteropLibrary.class, delegateTo = "squeakException")
    public static final class SqueakExceptionWrapper extends AbstractSqueakException {
        private static final long serialVersionUID = 1L;
        protected final transient PointersObject squeakException;

        public SqueakExceptionWrapper(final PointersObject exception) {
            squeakException = exception;
        }

        @Override
        @TruffleBoundary
        public String getMessage() {
            final Object messageTextObject = squeakException.instVarAt0Slow(EXCEPTION.MESSAGE_TEXT);
            if (messageTextObject instanceof final NativeObject messageText && messageText.isTruffleStringType()) {
                return messageText.asStringUnsafe();
            } else {
                return squeakException.getSqueakClassName();
            }
        }
    }

    private SqueakExceptions() {
    }
}
