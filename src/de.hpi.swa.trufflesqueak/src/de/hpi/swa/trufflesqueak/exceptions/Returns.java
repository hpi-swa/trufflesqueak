/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FrameMarker;

public final class Returns {
    private abstract static class AbstractReturn extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        protected final transient Object returnValue;

        private AbstractReturn(final Object result) {
            assert result != null : "Unexpected `null` value";
            returnValue = result;
        }

        public final Object getReturnValue() {
            return returnValue;
        }
    }

    public static final class NonLocalReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        private final transient Object targetContextOrMarker;

        public NonLocalReturn(final Object returnValue, final Object targetContextOrMarker) {
            super(returnValue);
            assert targetContextOrMarker instanceof ContextObject || targetContextOrMarker instanceof FrameMarker;
            this.targetContextOrMarker = targetContextOrMarker;
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

    public static final class NonVirtualReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        private final transient Object targetContextOrMarker;
        private final transient ContextObject currentContext;

        public NonVirtualReturn(final Object returnValue, final Object targetContextOrMarker, final ContextObject currentContext) {
            super(returnValue);
            assert targetContextOrMarker instanceof ContextObject || targetContextOrMarker instanceof FrameMarker;
            this.targetContextOrMarker = targetContextOrMarker;
            this.currentContext = currentContext;
        }

        public Object getTargetContextOrMarker() {
            return targetContextOrMarker;
        }

        public ContextObject getTargetContext() {
            return (ContextObject) targetContextOrMarker;
        }

        public ContextObject getCurrentContext() {
            return currentContext;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContextOrMarker + ")";
        }
    }

    public static final class TopLevelReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;

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
