/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.exceptions;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ControlFlowException;

import de.hpi.swa.graal.squeak.model.ContextObject;

public final class Returns {
    private abstract static class AbstractReturn extends ControlFlowException {
        private static final long serialVersionUID = 1L;
        protected final Object returnValue;

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
        private final Object targetContextOrMarker;

        public NonLocalReturn(final Object returnValue, final Object targetContext) {
            super(returnValue);
            targetContextOrMarker = targetContext;
        }

        public Object getTargetContextOrMarker() {
            return targetContextOrMarker;
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return "NLR (value: " + returnValue + ", target: " + targetContextOrMarker + ")";
        }
    }

    public static final class NonVirtualContextModification extends Exception {
        private static final long serialVersionUID = 1L;
    }

    public static final class NonVirtualReturn extends AbstractReturn {
        private static final long serialVersionUID = 1L;
        private final ContextObject targetContext;
        private final ContextObject currentContext;

        public NonVirtualReturn(final Object returnValue, final ContextObject targetContext, final ContextObject currentContext) {
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
            CompilerAsserts.neverPartOfCompilation();
            return "NVR (value: " + returnValue + ", current: " + currentContext + ", target: " + targetContext + ")";
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
