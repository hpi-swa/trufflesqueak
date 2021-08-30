/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.process;

import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

public final class GetActiveProcessNode extends AbstractNode {
    @Child private AbstractPointersObjectReadNode readNode = AbstractPointersObjectReadNode.create();

    public static GetActiveProcessNode create() {
        return new GetActiveProcessNode();
    }

    public PointersObject execute() {
        return readNode.executePointers(getContext().getScheduler(), PROCESS_SCHEDULER.ACTIVE_PROCESS);
    }
}
