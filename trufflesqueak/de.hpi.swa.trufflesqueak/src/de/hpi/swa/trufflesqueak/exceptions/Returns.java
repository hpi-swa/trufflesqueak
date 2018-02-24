package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.ContextObject;

public final class Returns {
    private static abstract class AbstractReturn extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        @CompilationFinal protected final Object returnValue;

        public AbstractReturn(Object result) {
            returnValue = result;
        }

        public Object getReturnValue() {
            return returnValue;
        }
    }

    public static class FreshReturn extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        @CompilationFinal protected final AbstractReturn returnValue;

        public FreshReturn(AbstractReturn result) {
            returnValue = result;
        }

        public AbstractReturn getReturnException() {
            return returnValue;
        }

        @Override
        public String toString() {
            return "Fresh (value: " + returnValue + ")";
        }
    }

    public static class LocalReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;

        public LocalReturn(Object result) {
            super(result);
        }

        @Override
        public String toString() {
            return "LR (value: " + returnValue + ")";
        }
    }

    public static class NonLocalReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        @CompilationFinal private ContextObject targetContext;
        private boolean arrivedAtTargetContext = false;

        public NonLocalReturn(Object returnValue, ContextObject targetContext) {
            super(returnValue);
            this.targetContext = targetContext;
        }

        public ContextObject getTargetContext() {
            return targetContext;
        }

        public boolean hasArrivedAtTargetContext() {
            return arrivedAtTargetContext;
        }

        public void setArrivedAtTargetContext() {
            arrivedAtTargetContext = true;
        }

        @Override
        public String toString() {
            return "NLR (value: " + returnValue + ", arrived: " + arrivedAtTargetContext + ", target: " + targetContext + ")";
        }
    }

    public static class NonVirtualContextModification extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static class NonVirtualReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        @CompilationFinal private final ContextObject targetContext;
        @CompilationFinal private final ContextObject currentContext;

        public NonVirtualReturn(Object returnValue, ContextObject targetContext, ContextObject currentContext) {
            super(returnValue);
            this.targetContext = targetContext;
            this.currentContext = currentContext;
        }

        public ContextObject getTargetContext() {
            return targetContext;
        }

        public ContextObject getCurrentContext() {
            return currentContext;
        }

        @Override
        public String toString() {
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContext + ")";
        }
    }

    public static class TopLevelReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;

        public TopLevelReturn(Object result) {
            super(result);
        }

        @Override
        public String toString() {
            return "TLR (value: " + returnValue + ")";
        }
    }
}
