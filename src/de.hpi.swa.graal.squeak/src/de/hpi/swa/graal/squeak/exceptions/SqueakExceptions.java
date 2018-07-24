package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleException;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
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
