/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import java.io.Serial;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class Returns {
    private abstract static class AbstractReturn extends ControlFlowException {
        @Serial private static final long serialVersionUID = 1L;
        protected final transient Object returnValue;

        private AbstractReturn(final Object returnValue) {
            assert returnValue != null : "Unexpected `null` value";
            this.returnValue = returnValue;
        }

        public final Object getReturnValue() {
            return returnValue;
        }
    }

    private abstract static class AbstractReturnWithContext extends AbstractReturn {
        @Serial private static final long serialVersionUID = 1L;
        protected final transient ContextObject targetContext;

        private AbstractReturnWithContext(final Object returnValue, final ContextObject targetContext) {
            super(returnValue);
            this.targetContext = targetContext;
        }

        public final boolean targetIsFrame(final VirtualFrame frame) {
            return targetContext == FrameAccess.getContext(frame);
        }

        public final ContextObject getTargetContext() {
            return targetContext;
        }
    }

    public abstract static class AbstractStandardSendReturn extends AbstractReturnWithContext {
        @Serial private static final long serialVersionUID = 1L;

        private AbstractStandardSendReturn(final Object returnValue, final ContextObject targetContext) {
            super(returnValue, targetContext);
        }
    }

    /**
     * NonLocalReturn represents a return to a targetContext that is on the sender chain.
     */
    public static final class NonLocalReturn extends AbstractStandardSendReturn {
        @Serial private static final long serialVersionUID = 1L;

        public NonLocalReturn(final Object returnValue, final ContextObject homeContext) {
            super(returnValue, (ContextObject) homeContext.getFrameSender());
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NLR (value: " + returnValue + ", target: " + targetContext + ")";
        }
    }

    /**
     * CannotReturnToTarget represents a return to a targetContext that cannot be found on the
     * sender chain.
     */
    public static final class CannotReturnToTarget extends AbstractReturnWithContext {
        @Serial private static final long serialVersionUID = 1L;

        public CannotReturnToTarget(final Object returnValue, final ContextObject startingContext) {
            super(returnValue, startingContext);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "CR-NLR (value: " + returnValue + ", starting: " + targetContext + ")";
        }
    }

    public static final class NonVirtualReturn extends AbstractStandardSendReturn {
        @Serial private static final long serialVersionUID = 1L;

        public NonVirtualReturn(final Object returnValue, final AbstractSqueakObject targetContextOrNil) {
            super(returnValue, (ContextObject) targetContextOrNil);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NVR (value: " + returnValue + ", target: " + targetContext + ")";
        }
    }

    public static final class TopLevelReturn extends AbstractReturn {
        @Serial private static final long serialVersionUID = 1L;

        public TopLevelReturn(final Object result) {
            super(result);
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "TLR (value: " + returnValue + ")";
        }
    }

    private Returns() {
    }
}
