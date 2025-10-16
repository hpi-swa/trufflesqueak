/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import static de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash.assertNotForwarded;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.EphemeronObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectShallowCopyNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.NativeObjectNodes.NativeObjectShallowCopyNode;

@GenerateInline
@GenerateCached(false)
public abstract class SqueakObjectShallowCopyNode extends AbstractNode {

    public final Object execute(final Node node, final SqueakImageContext image, final Object object) {
        CompilerAsserts.partialEvaluationConstant(image);
        assert assertNotForwarded(object);
        return image.reportAllocation(executeAllocation(node, object));
    }

    protected abstract Object executeAllocation(Node node, Object obj);

    @Specialization
    protected static final NilObject doNil(final NilObject receiver) {
        return receiver;
    }

    @Specialization
    protected static final EmptyObject doEmpty(final EmptyObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final NativeObject doNative(final Node node, final NativeObject receiver,
                    @Cached final NativeObjectShallowCopyNode copyNode) {
        return copyNode.execute(node, receiver);
    }

    @Specialization
    protected static final PointersObject doPointers(final PointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final VariablePointersObject doVariablePointers(final VariablePointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final WeakVariablePointersObject doWeakPointers(final WeakVariablePointersObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final EphemeronObject doEphemeron(final EphemeronObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final ArrayObject doArray(final Node node, final ArrayObject receiver,
                    @Exclusive @Cached final ArrayObjectShallowCopyNode copyNode) {
        return copyNode.execute(node, receiver);
    }

    @Specialization
    protected static final BlockClosureObject doClosure(final BlockClosureObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final CompiledCodeObject doCode(final CompiledCodeObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final ContextObject doContext(final ContextObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final ClassObject doClass(final ClassObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final FloatObject doFloat(final FloatObject receiver) {
        return receiver.shallowCopy();
    }

    @Specialization
    protected static final CharacterObject doCharacterObject(final CharacterObject obj) {
        return obj.shallowCopy();
    }
}
