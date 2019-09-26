/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SYNTAX_ERROR_NOTIFICATION;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class SqueakExceptions {

    /**
     * Exception to signal an illegal state in GraalSqueak.
     */
    public static final class SqueakException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        private SqueakException(final String message, final Throwable cause) {
            super(message, cause);
            printSqueakStackTrace();
        }

        private SqueakException(final Object... messageParts) {
            super(ArrayUtils.toJoinedString(" ", messageParts));
            printSqueakStackTrace();
        }

        public static SqueakException create(final String message, final Throwable cause) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakException(message, cause);
        }

        public static SqueakException create(final Object... messageParts) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakException(messageParts);
        }

        public static SqueakException illegalState(final Throwable cause) {
            CompilerDirectives.transferToInterpreter();
            return new SqueakException("Illegal state in " + SqueakLanguageConfig.NAME, cause);
        }

        private static void printSqueakStackTrace() {
            final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();
            if (currentFrame != null) {
                final Frame frame = currentFrame.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (FrameAccess.isGraalSqueakFrame(frame)) {
                    FrameAccess.getMethod(frame).image.printSqStackTrace();
                }
            }
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
            super(((NativeObject) syntaxErrorNotification.at0(SYNTAX_ERROR_NOTIFICATION.ERROR_MESSAGE)).asStringUnsafe());
            final int sourceOffset = (int) ((long) syntaxErrorNotification.at0(SYNTAX_ERROR_NOTIFICATION.LOCATION) - 1);
            dummyCodeObjectNode = new FakeSourceCodeObjectNode(syntaxErrorNotification.image, sourceOffset);
        }

        @Override
        public Node getLocation() {
            return dummyCodeObjectNode;
        }

        @Override
        public boolean isSyntaxError() {
            return true;
        }

        protected class FakeSourceCodeObjectNode extends AbstractNodeWithImage {
            private final int sourceOffset;
            private SourceSection sourceSection;

            public FakeSourceCodeObjectNode(final SqueakImageContext image, final int sourceOffset) {
                super(image);
                this.sourceOffset = sourceOffset;
            }

            @Override
            public SourceSection getSourceSection() {
                if (sourceSection == null) {
                    // - 1 for previous character.
                    sourceSection = image.getLastParseRequestSource().createSection(Math.max(sourceOffset - 1, 0), 1);
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

    private SqueakExceptions() {
    }
}
