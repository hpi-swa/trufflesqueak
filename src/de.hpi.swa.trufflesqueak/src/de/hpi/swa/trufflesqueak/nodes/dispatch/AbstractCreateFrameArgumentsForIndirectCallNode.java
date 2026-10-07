/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.InlinedConditionProfile;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class AbstractCreateFrameArgumentsForIndirectCallNode extends AbstractNode {
    protected static final Object[] newMessage(final Node node,
                    final AbstractSqueakObject sender, final Object receiver, final Object[] arguments, final ClassObject receiverClass,
                    final NativeObject selector, final ClassObject.DispatchFailureResult result,
                    final SqueakImageContext image,
                    final InlinedConditionProfile isCannotInterpretProfile,
                    final AbstractPointersObjectWriteNode writeNode,
                    final CreateMessageNode createMessageNode) {
        final PointersObject message;
        if (isCannotInterpretProfile.profile(node, result.convention() == ClassObject.FallbackConvention.CANNOT_INTERPRET)) {
            message = DispatchUtils.buildNestedMessage(createMessageNode, selector, result.fallbackSelector(), receiver, arguments, result.fallbackDepth());
        } else {
            message = image.newMessage(writeNode, selector, receiverClass, arguments);
        }
        return FrameAccess.newMessageFallbackWith(sender, receiver, message);
    }
}
