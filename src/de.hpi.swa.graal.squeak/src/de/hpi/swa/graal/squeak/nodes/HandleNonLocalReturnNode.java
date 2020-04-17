/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.GetContextNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class HandleNonLocalReturnNode extends AbstractNode {
    @Child private AboutToReturnNode aboutToReturnNode;

    protected HandleNonLocalReturnNode(final CompiledCodeObject code) {
        aboutToReturnNode = AboutToReturnNode.create(code);
    }

    public static HandleNonLocalReturnNode create(final CompiledCodeObject code) {
        return HandleNonLocalReturnNodeGen.create(code);
    }

    public abstract Object executeHandle(VirtualFrame frame, NonLocalReturn nlr);

    @Specialization
    protected final Object doHandle(final VirtualFrame frame, final NonLocalReturn nlr,
                    @Cached("getInstructionPointerSlot(frame)") final FrameSlot instructionPointerSlot,
                    @Cached final GetContextNode getContextNode,
                    @Cached("createBinaryProfile()") final ConditionProfile hasModifiedSenderProfile) {
        if (hasModifiedSenderProfile.profile(getContextNode.hasModifiedSender(frame))) {
            aboutToReturnNode.executeAboutToReturn(frame, nlr); // handle ensure: or ifCurtailed:
            // Sender has changed.
            final ContextObject newSender = FrameAccess.getSenderContext(frame);
            final ContextObject target = (ContextObject) nlr.getTargetContextOrMarker();
            FrameAccess.terminate(frame, instructionPointerSlot);
            throw new NonVirtualReturn(nlr.getReturnValue(), target, newSender);
        } else {
            aboutToReturnNode.executeAboutToReturn(frame, nlr); // handle ensure: or ifCurtailed:
            FrameAccess.terminate(frame, instructionPointerSlot);
            throw nlr;
        }
    }
}
