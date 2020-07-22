/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.EXCEPTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.SYNTAX_ERROR_NOTIFICATION;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.DebugUtils;

public final class SqueakExceptions {

    /**
     * Exception to signal an illegal state in TruffleSqueak.
     */
    public static final class SqueakException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private SqueakException(final String message, final Throwable cause) {
            super(message, cause);
            DebugUtils.printSqStackTrace();
        }

        private SqueakException(final Object... messageParts) {
            super(ArrayUtils.toJoinedString(" ", messageParts));
            DebugUtils.printSqStackTrace();
        }

        public static SqueakException create(final String message, final Throwable cause) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakException(message, cause);
        }

        public static SqueakException create(final Object... messageParts) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakException(messageParts);
        }
    }

    public static final class SqueakError extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;
        private final Node location;

        public SqueakError(final Node location, final String message) {
            super(message);
            this.location = location;
        }

        @Override
        public Node getLocation() {
            return location;
        }
    }

    public static final class SqueakAbortException extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;

        private SqueakAbortException(final Object... messageParts) {
            super(ArrayUtils.toJoinedString(" ", messageParts));
        }

        public static SqueakAbortException create(final Object... messageParts) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakAbortException(messageParts);
        }

        @Override
        public Node getLocation() {
            return null;
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }

    public static final class SqueakSyntaxError extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;
        private final FakeSourceCodeObjectNode dummyCodeObjectNode;

        public SqueakSyntaxError(final PointersObject syntaxErrorNotification) {
            super(((NativeObject) syntaxErrorNotification.instVarAt0Slow(SYNTAX_ERROR_NOTIFICATION.ERROR_MESSAGE)).asStringUnsafe());
            final int sourceOffset = (int) ((long) syntaxErrorNotification.instVarAt0Slow(SYNTAX_ERROR_NOTIFICATION.LOCATION) - 1);
            dummyCodeObjectNode = new FakeSourceCodeObjectNode(sourceOffset);
        }

        @TruffleBoundary
        public SqueakSyntaxError(final String message, final int position, final String source) {
            super("Syntax Error: \"" + message + "\" at position " + position);
            dummyCodeObjectNode = new FakeSourceCodeObjectNode(source, position);
        }

        @Override
        public Node getLocation() {
            return dummyCodeObjectNode;
        }

        @Override
        public boolean isSyntaxError() {
            return true;
        }

        protected static final class FakeSourceCodeObjectNode extends AbstractNode {
            private int sourceOffset;
            private SourceSection sourceSection;

            public FakeSourceCodeObjectNode(final int sourceOffset) {
                this.sourceOffset = sourceOffset;
            }

            public FakeSourceCodeObjectNode(final String source, final int position) {
                sourceSection = Source.newBuilder(SqueakLanguageConfig.ID, source, "<syntax error>").build().createSection(Math.max(position - 1, 0), 1);
            }

            @Override
            public SourceSection getSourceSection() {
                if (sourceSection == null) {
                    // - 1 for previous character.
                    sourceSection = SqueakLanguage.getContext().getLastParseRequestSource().createSection(Math.max(sourceOffset - 1, 0), 1);
                }
                return sourceSection;
            }
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return null;
        }
    }

    public static final class SqueakQuit extends ControlFlowException implements TruffleException {
        private static final long serialVersionUID = 1L;
        private final int exitStatus;

        public SqueakQuit(final int exitStatus) {
            this.exitStatus = exitStatus;
        }

        @Override
        public int getExitStatus() {
            return exitStatus;
        }

        @Override
        public Node getLocation() {
            return null;
        }

        @Override
        public boolean isExit() {
            return true;
        }
    }

    public static final class SqueakInteropException extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;
        private final PointersObject squeakException;

        public SqueakInteropException(final PointersObject original) {
            squeakException = original;
        }

        @Override
        public String getMessage() {
            CompilerAsserts.neverPartOfCompilation();
            final Object messageText = squeakException.instVarAt0Slow(EXCEPTION.MESSAGE_TEXT);
            if (messageText instanceof NativeObject && ((NativeObject) messageText).isString()) {
                return ((NativeObject) messageText).asStringUnsafe();
            } else {
                return squeakException.toString();
            }
        }

        @SuppressWarnings("sync-override")
        @Override
        public Throwable fillInStackTrace() {
            return this;
        }

        @Override
        public Object getExceptionObject() {
            return squeakException;
        }

        @Override
        public Node getLocation() {
            return null;
        }
    }

    public static final class SqueakInterrupt extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;

        @Override
        public Node getLocation() {
            return null;
        }

        @Override
        public boolean isCancelled() {
            return true;
        }
    }

    private SqueakExceptions() {
    }
}
