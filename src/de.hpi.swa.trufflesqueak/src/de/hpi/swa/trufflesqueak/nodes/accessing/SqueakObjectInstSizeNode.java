/*
 * Copyright (c) 2017-2023 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2023 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectInstSizeNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectInstSizeNode extends AbstractNode {

    @NeverDefault
    public static SqueakObjectInstSizeNode create() {
        return SqueakObjectInstSizeNodeGen.create();
    }

    public abstract int execute(Object obj);

    @Specialization
    protected static final int doNil(final NilObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doArray(final ArrayObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doAbstractPointersObject(final AbstractPointersObject obj,
                    @Cached final AbstractPointersObjectInstSizeNode instSizeNode) {
        return instSizeNode.execute(obj);
    }

    @Specialization
    protected static final int doClass(final ClassObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doContext(final ContextObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doClosure(final BlockClosureObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doCode(final CompiledCodeObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doEmpty(final EmptyObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doNative(final NativeObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doFloat(final FloatObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doLarge(final LargeIntegerObject obj) {
        return obj.instsize();
    }

    @Specialization
    protected static final int doCharacterObject(final CharacterObject obj) {
        return obj.instsize();
    }
}
