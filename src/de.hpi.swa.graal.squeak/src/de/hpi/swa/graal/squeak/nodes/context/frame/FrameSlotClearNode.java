/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context.frame;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;

public abstract class FrameSlotClearNode extends AbstractFrameSlotNode {

    public static FrameSlotClearNode create(final FrameSlot frameSlot) {
        return FrameSlotClearNodeGen.create(frameSlot);
    }

    public abstract void executeClear(Frame frame);

    @Specialization(guards = "frame.isBoolean(getSlot())")
    protected final void clearBoolean(@SuppressWarnings("unused") final Frame frame) {
    }

    @Specialization(guards = "frame.isLong(getSlot())")
    protected final void clearLong(@SuppressWarnings("unused") final Frame frame) {
    }

    @Specialization(guards = "frame.isDouble(getSlot())")
    protected final void clearDouble(@SuppressWarnings("unused") final Frame frame) {
    }

    @Specialization(replaces = {"clearBoolean", "clearLong", "clearDouble"})
    protected final void clearObject(final Frame frame) {
        frame.setObject(getSlot(), null);
    }
}
