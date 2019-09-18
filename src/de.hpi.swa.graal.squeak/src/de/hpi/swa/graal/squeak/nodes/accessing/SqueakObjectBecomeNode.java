/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.WeakVariablePointersObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class SqueakObjectBecomeNode extends AbstractNode {

    public static SqueakObjectBecomeNode create() {
        return SqueakObjectBecomeNodeGen.create();
    }

    public abstract boolean execute(Object left, Object right);

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
        left.becomeLayout(right);
        return true;
    }

    @Specialization(guards = {"left != right"})
    protected static final boolean doPointers(final VariablePointersObject left, final VariablePointersObject right) {
        left.become(right);
        return true;
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"left != right"})
    protected static final boolean doWeakPointers(final WeakVariablePointersObject left, final WeakVariablePointersObject right) {
        left.become(right);
        return true;
    }
}
