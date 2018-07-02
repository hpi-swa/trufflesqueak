package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.ContextObject;

public final class Returns {
    private abstract static class AbstractReturn extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        protected final Object returnValue;

        private AbstractReturn(final Object result) {
            assert result != null;
            returnValue = result;
        }

        public final Object getReturnValue() {
            return returnValue;
        }
    }

    public static final class LocalReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;

        public LocalReturn(final Object result) {
            super(result);
        }

        @Override
        public String toString() {
            return "LR (value: " + returnValue + ")";
        }
    }

    public static final class NonLocalReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        private final ContextObject targetContext;
        private boolean arrivedAtTargetContext = false;

        public NonLocalReturn(final Object returnValue, final ContextObject targetContext) {
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

    public static final class NonVirtualContextModification extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static final class NonVirtualReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        private final ContextObject targetContext;
        private final ContextObject currentContext;

        private ContextObject lastSeenContext;

        public NonVirtualReturn(final Object returnValue, final ContextObject targetContext, final ContextObject currentContext, final ContextObject lastSeenContext) {
            super(returnValue);
            assert !targetContext.hasVirtualSender();
            assert !currentContext.hasVirtualSender();
            this.targetContext = targetContext;
            this.currentContext = currentContext;
            this.lastSeenContext = lastSeenContext;
        }

        public ContextObject getTargetContext() {
            return targetContext;
        }

        public ContextObject getCurrentContext() {
            return currentContext;
        }

        public ContextObject getLastSeenContext() {
            return lastSeenContext;
        }

        public void setLastSeenContext(final ContextObject lastSeenContext) {
            this.lastSeenContext = lastSeenContext;
        }

        @Override
        public String toString() {
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContext + ")";
        }
    }

    public static class TopLevelReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;

        public TopLevelReturn(final Object result) {
            super(result);
        }

        @Override
        public final String toString() {
            return "TLR (value: " + returnValue + ")";
        }
    }
}
