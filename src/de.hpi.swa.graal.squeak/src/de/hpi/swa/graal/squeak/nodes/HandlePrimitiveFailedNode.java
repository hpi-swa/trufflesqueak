/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackPushNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

@NodeInfo(cost = NodeCost.NONE)
public abstract class HandlePrimitiveFailedNode extends AbstractNodeWithCode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, HandlePrimitiveFailedNode.class);
    private static final boolean isLoggingEnabled = LOG.isLoggable(Level.FINER);

    @Child protected ArrayObjectSizeNode sizeNode = ArrayObjectSizeNode.create();

    private static final ERROR_TABLE[] errors = ERROR_TABLE.values();

    protected HandlePrimitiveFailedNode(final CompiledCodeObject code) {
        super(code);
    }

    public static HandlePrimitiveFailedNode create(final CompiledCodeObject code) {
        assert code instanceof CompiledMethodObject;
        return HandlePrimitiveFailedNodeGen.create(code);
    }

    public abstract void executeHandle(VirtualFrame frame, int reasonCode);

    /*
     * Look up error symbol in error table and push it to stack. The fallback code pops the error
     * symbol into the corresponding temporary variable. See
     * StackInterpreter>>#getErrorObjectFromPrimFailCode for more information.
     */
    @Specialization(guards = {"followedByExtendedStore(code)", "reasonCode < sizeNode.execute(code.image.primitiveErrorTable)"})
    protected final void doHandleWithLookup(final VirtualFrame frame, final int reasonCode,
                    @Cached("create(code)") final FrameStackPushNode pushNode,
                    @Cached final ArrayObjectReadNode readNode) {
        final Object reason = readNode.execute(code.image.primitiveErrorTable, reasonCode);
        if (isLoggingEnabled) {
            LOG.finer("Primitive failed in method " + code + " with reason " + reason);
        }
        pushNode.execute(frame, reason);
    }

    @Specialization(guards = {"followedByExtendedStore(code)", "reasonCode >= sizeNode.execute(code.image.primitiveErrorTable)"})
    protected final void doHandleRawValue(final VirtualFrame frame, final int reasonCode,
                    @Cached("create(code)") final FrameStackPushNode pushNode) {
        if (isLoggingEnabled) {
            LOG.finer("Primitive failed in method " + code + " with reason " + errors[reasonCode]);
        }
        pushNode.execute(frame, reasonCode);
    }

    @Specialization(guards = "!followedByExtendedStore(code)")
    protected final void doNothing(@SuppressWarnings("unused") final int reasonCode) {
        if (isLoggingEnabled) {
            LOG.finer("Primitive failed in method " + code + " with reason " + errors[reasonCode]);
        }
    }

    protected static final boolean followedByExtendedStore(final CompiledCodeObject codeObject) {
        // fourth bytecode indicates extended store after callPrimitive
        return codeObject.getBytes()[3] == (byte) 0x81;
    }
}
