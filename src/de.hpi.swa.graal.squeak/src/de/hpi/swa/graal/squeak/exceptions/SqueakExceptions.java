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
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SYNTAX_ERROR_NOTIFICATION;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class SqueakExceptions {

    /*
     * Exception to signal an illegal state in GraalSqueak.
     */
    public static final class SqueakException extends IllegalStateException {
        private static final long serialVersionUID = 1L;

        public SqueakException(final Object... messageParts) {
            super(ArrayUtils.toJoinedString(" ", messageParts));
            CompilerDirectives.transferToInterpreter();
            final FrameInstance currentFrame = Truffle.getRuntime().getCurrentFrame();
            if (currentFrame != null) {
                final Frame frame = currentFrame.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (frame.getArguments().length > FrameAccess.METHOD) {
                    final CompiledCodeObject code = FrameAccess.getMethod(frame);
                    code.image.printSqStackTrace();
                }
            }
        }
    }

    public static final class SqueakAbortException extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;

        public SqueakAbortException(final Object... messageParts) {
            super(ArrayUtils.toJoinedString(" ", messageParts));
        }

        public Node getLocation() {
            return null;
        }
    }

    public static final class SqueakSyntaxError extends RuntimeException implements TruffleException {
        private static final long serialVersionUID = 1L;
        private final FakeSourceCodeObjectNode dummyCodeObjectNode;

        public SqueakSyntaxError(final PointersObject syntaxErrorNotification) {
            super(((NativeObject) syntaxErrorNotification.at0(SYNTAX_ERROR_NOTIFICATION.ERROR_MESSAGE)).asString());
            final int sourceOffset = (int) ((long) syntaxErrorNotification.at0(SYNTAX_ERROR_NOTIFICATION.LOCATION) - 1);
            dummyCodeObjectNode = new FakeSourceCodeObjectNode(syntaxErrorNotification.image, sourceOffset);
        }

        public Node getLocation() {
            return dummyCodeObjectNode;
        }

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
                    sourceSection = image.getLastParseRequestSource().createSection(sourceOffset - 1, 1);
                }
                return sourceSection;
            }
        }
    }

    public static final class SqueakQuit extends ControlFlowException implements TruffleException {
        private static final long serialVersionUID = 1L;
        private final int exitStatus;

        public SqueakQuit(final int exitStatus) {
            this.exitStatus = exitStatus;
        }

        public int getExitStatus() {
            return exitStatus;
        }

        public Node getLocation() {
            return null;
        }

        public boolean isExit() {
            return true;
        }
    }

    private SqueakExceptions() {
    }
}
