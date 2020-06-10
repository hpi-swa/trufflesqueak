/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@GenerateUncached
public abstract class GetActiveProcessNode extends AbstractNode {

    public static GetActiveProcessNode create() {
        return GetActiveProcessNodeGen.create();
    }

    public static GetActiveProcessNode getUncached() {
        return GetActiveProcessNodeGen.getUncached();
    }

    public static PointersObject getSlow(final SqueakImageContext image) {
        return doGet(AbstractPointersObjectReadNode.getUncached(), image);
    }

    public abstract PointersObject execute();

    @Specialization
    protected static final PointersObject doGet(@Cached final AbstractPointersObjectReadNode readNode,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return readNode.executePointers(image.getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);

    }
}
