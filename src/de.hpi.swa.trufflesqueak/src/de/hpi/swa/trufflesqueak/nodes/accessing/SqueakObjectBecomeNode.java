/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@GenerateInline
@GenerateCached(false)
public abstract class SqueakObjectBecomeNode extends AbstractNode {

    public abstract boolean execute(Node node, Object left, Object right);

    @SuppressWarnings("unused")
    @Specialization(guards = {"left == right"})
    protected static final boolean doSameObject(final AbstractSqueakObject left, final AbstractSqueakObject right) {
        return false;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doClosure(final BlockClosureObject left, final BlockClosureObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doClass(final ClassObject left, final ClassObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doCode(final CompiledCodeObject left, final CompiledCodeObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doContext(final ContextObject left, final ContextObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doEmpty(final EmptyObject left, final EmptyObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doNative(final NativeObject left, final NativeObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doArray(final ArrayObject left, final ArrayObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doPointers(final PointersObject left, final PointersObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doVariablePointers(final VariablePointersObject left, final VariablePointersObject right) {
        left.become(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doEphemeron(final EphemeronObject left, final EphemeronObject right) {
        left.become(right);
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"left != right"})
    protected static final boolean doWeakPointers(final WeakVariablePointersObject left, final WeakVariablePointersObject right) {
        left.become(right);
        return true;
    }

    @TruffleBoundary
    @SuppressWarnings("unused")
    @Specialization(guards = "left.getClass() != right.getClass()")
    protected static final boolean doFail(final Object left, final Object right) {
        return false;
    }
}
