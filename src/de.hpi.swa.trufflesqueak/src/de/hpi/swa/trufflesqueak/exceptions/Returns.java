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

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;
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
        private final transient ContextObject homeContext;
        private final transient Object targetContextOrMarker;

        public NonLocalReturn(final Object returnValue, final ContextObject homeContext) {
            super(returnValue);
            final Object target = homeContext.getFrameSender();
            assert target instanceof ContextObject || target instanceof FrameMarker;
            this.homeContext = homeContext;
            this.targetContextOrMarker = target;
        }

        public ContextObject getHomeContext() {
            return homeContext;
        }

        public boolean targetIsFrame(final VirtualFrame frame) {
            return targetContextOrMarker == FrameAccess.getMarker(frame) || targetContextOrMarker == FrameAccess.getContext(frame);
        }

        public Object getTargetContextOrMarker() {
            return targetContextOrMarker;
        }

        public ContextObject getTargetContext() {
            return (ContextObject) targetContextOrMarker;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NLR (value: " + returnValue + ", target: " + targetContextOrMarker + ")";
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
        private final transient Object targetContextMarkerOrNil;
        private final transient ContextObject currentContext;

        public NonVirtualReturn(final Object returnValue, final Object targetContextMarkerOrNil, final ContextObject currentContext) {
            super(returnValue);
            assert targetContextMarkerOrNil instanceof ContextObject || targetContextMarkerOrNil instanceof FrameMarker || targetContextMarkerOrNil == NilObject.SINGLETON;
            this.targetContextMarkerOrNil = targetContextMarkerOrNil;
            this.currentContext = currentContext;
        }

        public boolean targetIsFrame(final VirtualFrame frame) {
            return targetContextMarkerOrNil == FrameAccess.getMarker(frame) || targetContextMarkerOrNil == FrameAccess.getContext(frame);
        }

        public Object getTargetContextMarkerOrNil() {
            return targetContextMarkerOrNil;
        }

        public ContextObject getCurrentContext() {
            return currentContext;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContextMarkerOrNil + ")";
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
