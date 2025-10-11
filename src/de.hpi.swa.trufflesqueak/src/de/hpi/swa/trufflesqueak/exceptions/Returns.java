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
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class Returns {
    private abstract static class AbstractReturn extends ControlFlowException {
        @Serial private static final long serialVersionUID = 1L;
        protected final transient Object returnValue;

        private AbstractReturn(final Object result) {
            assert result != null : "Unexpected `null` value";
            returnValue = result;
        }

        public final Object getReturnValue() {
            return returnValue;
        }
    }

    /**
     * NonLocalReturn represents a return to a targetContext that is on the sender chain.
     */
    public static final class NonLocalReturn extends AbstractReturn {
        @Serial private static final long serialVersionUID = 1L;
        private final transient ContextObject targetContext;

        public NonLocalReturn(final Object returnValue, final ContextObject homeContext) {
            super(returnValue);
            this.targetContext = (ContextObject) homeContext.getFrameSender();
        }

        public boolean targetIsFrame(final VirtualFrame frame) {
            return targetContext == FrameAccess.getContext(frame);
        }

        public ContextObject getTargetContext() {
            return targetContext;
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
    public static final class CannotReturnToTarget extends AbstractReturn {
        @Serial private static final long serialVersionUID = 1L;
        private final transient ContextObject startingContext;

        public CannotReturnToTarget(final Object returnValue, final ContextObject startingContext) {
            super(returnValue);
            this.startingContext = startingContext;
        }

        public ContextObject getStartingContext() {
            return startingContext;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "CR-NLR (value: " + returnValue + ", starting: " + startingContext + ")";
        }
    }

    public static final class NonVirtualReturn extends AbstractReturn {
        @Serial private static final long serialVersionUID = 1L;
        private final transient AbstractSqueakObject targetContextOrNil;
        private final transient ContextObject currentContext;

        public NonVirtualReturn(final Object returnValue, final AbstractSqueakObject targetContextOrNil, final ContextObject currentContext) {
            super(returnValue);
            assert targetContextOrNil instanceof ContextObject || targetContextOrNil == NilObject.SINGLETON;
            this.targetContextOrNil = targetContextOrNil;
            this.currentContext = currentContext;
        }

        public boolean targetIsFrame(final VirtualFrame frame) {
            return targetContextOrNil == FrameAccess.getContext(frame);
        }

        public AbstractSqueakObject getTargetContextOrNil() {
            return targetContextOrNil;
        }

        public ContextObject getCurrentContext() {
            return currentContext;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContextOrNil + ")";
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
