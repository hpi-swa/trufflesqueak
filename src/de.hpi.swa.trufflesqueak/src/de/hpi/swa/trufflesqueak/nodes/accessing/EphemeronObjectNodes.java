/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

public final class EphemeronObjectNodes {
    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(EphemeronObject.class)
    public abstract static class EphemeronObjectReadNode extends AbstractNode {
        public abstract Object execute(Node node, EphemeronObject obj, long index);

        @Specialization(guards = "isKeyIndex(index)")
        protected static final Object doEphemeronKey(final EphemeronObject obj, @SuppressWarnings("unused") final long index) {
            return obj.getKey();
        }

        @Specialization(guards = {"isValueIndex(index)"})
        protected static final Object doEphemeronValue(final EphemeronObject obj, @SuppressWarnings("unused") final long index) {
            return obj.getValue();
        }

        @Specialization(guards = {"isOtherIndex(index)"})
        protected static final Object doEphemeronOther(final EphemeronObject obj, final long index) {
            return obj.getOtherPointer((int) index);
        }
    }

    @GenerateInline
    @GenerateUncached
    @GenerateCached(false)
    @ImportStatic(EphemeronObject.class)
    public abstract static class EphemeronObjectWriteNode extends AbstractNode {

        public abstract void execute(Node node, EphemeronObject obj, long index, Object value);

        @Specialization(guards = "isKeyIndex(index)")
        protected static final void doEphemeronKey(final EphemeronObject obj, @SuppressWarnings("unused") final long index, final Object value) {
            obj.setKey(value);
        }

        @Specialization(guards = "isValueIndex(index)")
        protected static final void doEphemeronValue(final EphemeronObject obj, @SuppressWarnings("unused") final long index, final Object value) {
            obj.setValue(value);
        }

        @Specialization(guards = "isOtherIndex(index)")
        protected static final void doEphemeronOther(final EphemeronObject obj, final long index, final Object value) {
            obj.setOtherPointer((int) index, value);
        }
    }
}
