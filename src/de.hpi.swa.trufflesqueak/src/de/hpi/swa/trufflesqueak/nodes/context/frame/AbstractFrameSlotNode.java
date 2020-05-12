/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context.frame;

import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.frame.FrameSlot;

import de.hpi.swa.trufflesqueak.nodes.AbstractNode;

@NodeField(name = "slot", type = FrameSlot.class)
public abstract class AbstractFrameSlotNode extends AbstractNode {
    protected abstract FrameSlot getSlot();
}
