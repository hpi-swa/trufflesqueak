/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateCached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@GenerateInline
@GenerateCached(false)
public abstract class GetActiveProcessNode extends AbstractNode {
    public abstract PointersObject execute(Node node);

    @Specialization
    protected static final PointersObject getActiveProcess(final Node node,
                    @Cached(inline = false) final AbstractPointersObjectReadNode readNode) {
        return readNode.executePointers(getContext(node).getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }
}
