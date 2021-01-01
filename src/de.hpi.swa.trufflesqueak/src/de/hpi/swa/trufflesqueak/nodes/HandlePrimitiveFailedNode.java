/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Shared;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.HandlePrimitiveFailedNodeFactory.HandlePrimitiveFailedImplNodeGen;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameStackPushNode;

@NodeInfo(cost = NodeCost.NONE)
public abstract class HandlePrimitiveFailedNode extends AbstractNode {
    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        if (code.hasStoreIntoTemp1AfterCallPrimitive()) {
            return HandlePrimitiveFailedImplNodeGen.create();
        } else {
            return new HandlePrimitiveFailedNoopNode();
        }
    }

    public abstract void executeHandle(VirtualFrame frame, int reasonCode);

    protected abstract static class HandlePrimitiveFailedImplNode extends HandlePrimitiveFailedNode {
        /*
         * Look up error symbol in error table and push it to stack. The fallback code pops the
         * error symbol into the corresponding temporary variable. See
         * StackInterpreter>>#getErrorObjectFromPrimFailCode for more information.
         */
        @Specialization(guards = {"reasonCode < sizeNode.execute(image.primitiveErrorTable)"}, limit = "1")
        protected static final void doHandleWithLookup(final VirtualFrame frame, final int reasonCode,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final FrameStackPushNode pushNode,
                        @Cached final ArrayObjectReadNode readNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            pushNode.execute(frame, readNode.execute(image.primitiveErrorTable, reasonCode));
        }

        @Specialization(guards = {"reasonCode >= sizeNode.execute(image.primitiveErrorTable)"}, limit = "1")
        protected static final void doHandleRawValue(final VirtualFrame frame, final int reasonCode,
                        @SuppressWarnings("unused") @Shared("sizeNode") @Cached final ArrayObjectSizeNode sizeNode,
                        @Cached final FrameStackPushNode pushNode,
                        @SuppressWarnings("unused") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            pushNode.execute(frame, (long) reasonCode);
        }
    }

    private static final class HandlePrimitiveFailedNoopNode extends HandlePrimitiveFailedNode {
        @Override
        public void executeHandle(final VirtualFrame frame, final int reasonCode) {
            // nothing to do
        }
    }
}
