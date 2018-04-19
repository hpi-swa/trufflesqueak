package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.ContextObject;

public final class Returns {
    private static abstract class AbstractReturn extends ControlFlowException {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal protected final Object returnValue;

        public AbstractReturn(final Object result) {
            returnValue = result;
        }

        public final Object getReturnValue() {
            return returnValue;
        }
    }

    public static final class FreshReturn extends ControlFlowException {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal protected final AbstractReturn returnValue;

        public FreshReturn(final AbstractReturn result) {
            returnValue = result;
        }

        public final AbstractReturn getReturnException() {
            return returnValue;
        }

        @Override
        public final String toString() {
            return "Fresh (value: " + returnValue + ")";
        }
    }

    public static final class LocalReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;

        public LocalReturn(Object result) {
            super(result);
        }

        @Override
        public final String toString() {
            return "LR (value: " + returnValue + ")";
        }
    }

    public static final class NonLocalReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal private ContextObject targetContext;
        @CompilationFinal private boolean arrivedAtTargetContext = false;

        public NonLocalReturn(final Object returnValue, final ContextObject targetContext) {
            super(returnValue);
            this.targetContext = targetContext;
        }

        public final ContextObject getTargetContext() {
            return targetContext;
        }

        public final boolean hasArrivedAtTargetContext() {
            return arrivedAtTargetContext;
        }

        public final void setArrivedAtTargetContext() {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            arrivedAtTargetContext = true;
        }

        @Override
        public final String toString() {
            return "NLR (value: " + returnValue + ", arrived: " + arrivedAtTargetContext + ", target: " + targetContext + ")";
        }
    }

    public static final class NonVirtualContextModification extends Exception {
        @CompilationFinal private static final long serialVersionUID = 1L;
    }

    public static final class NonVirtualReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;
        @CompilationFinal private final ContextObject targetContext;
        @CompilationFinal private final ContextObject currentContext;

        public NonVirtualReturn(final Object returnValue, final ContextObject targetContext, final ContextObject currentContext) {
            super(returnValue);
            this.targetContext = targetContext;
            this.currentContext = currentContext;
        }

        public final ContextObject getTargetContext() {
            return targetContext;
        }

        public final ContextObject getCurrentContext() {
            return currentContext;
        }

        @Override
        public final String toString() {
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContext + ")";
        }
    }

    public static class TopLevelReturn extends AbstractReturn {
        @CompilationFinal private static final long serialVersionUID = 1L;

        public TopLevelReturn(final Object result) {
            super(result);
        }

        @Override
        public final String toString() {
            return "TLR (value: " + returnValue + ")";
        }
    }
}
