/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ConditionProfile;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

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
                    @Cached final ConditionProfile hasModifiedSenderProfile) {
        if (hasModifiedSenderProfile.profile(FrameAccess.hasModifiedSender(frame))) {
            aboutToReturnNode.executeAboutToReturn(frame, nlr); // handle ensure: or ifCurtailed:
            // Sender might have changed.
            final ContextObject newSender = FrameAccess.getSenderContext(frame);
            final ContextObject target = (ContextObject) nlr.getTargetContextOrMarker();
            FrameAccess.terminate(frame);
            // TODO: `target == newSender` may could use special handling?
            throw new NonVirtualReturn(nlr.getReturnValue(), target, newSender);
        } else {
            aboutToReturnNode.executeAboutToReturn(frame, nlr); // handle ensure: or ifCurtailed:
            FrameAccess.terminate(frame);
            throw nlr;
        }
    }
}
