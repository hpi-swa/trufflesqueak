/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.accessing;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithHash;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class SqueakObjectHashNode extends AbstractNode {

    public abstract long execute(Object obj);

    @Specialization
    protected static final long doNil(@SuppressWarnings("unused") final NilObject receiver) {
        return NilObject.getSqueakHash();
    }

    @Specialization(guards = "obj == FALSE")
    protected static final long doBooleanFalse(@SuppressWarnings("unused") final boolean obj) {
        return BooleanObject.getFalseSqueakHash();
    }

    @Specialization(guards = "obj != FALSE")
    protected static final long doBooleanTrue(@SuppressWarnings("unused") final boolean obj) {
        return BooleanObject.getTrueSqueakHash();
    }

    @Specialization
    protected static final long doLong(final long obj) {
        return obj;
    }

    @Specialization
    protected static final long doAbstractSqueakObjectWithHash(final AbstractSqueakObjectWithHash obj,
                    @Cached final BranchProfile needsHashProfile) {
        return obj.getSqueakHash(needsHashProfile);
    }
}
