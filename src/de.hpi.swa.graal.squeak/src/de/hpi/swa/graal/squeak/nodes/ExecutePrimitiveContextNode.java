/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.nodes.bytecodes.MiscellaneousBytecodes.CallPrimitiveNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ExecutePrimitiveContextNode extends AbstractExecuteContextNode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, CallPrimitiveNode.class);

    @Child private CallPrimitiveNode callPrimitiveNode;
    @Child private HandlePrimitiveFailedNode handlePrimitiveFailedNode;

    protected ExecutePrimitiveContextNode(final CompiledCodeObject code, final boolean resume) {
        super(code, resume);
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString() + " (" + getCallPrimitiveNode() + ")";
    }

    @Override
    public Object executeFresh(final VirtualFrame frame) {
        FrameAccess.setInstructionPointer(frame, code, 0);
        initializeFrame(frame);
        if (getCallPrimitiveNode().primitiveNode != null) {
            try {
                return callPrimitiveNode.primitiveNode.executePrimitive(frame);
            } catch (final PrimitiveFailed e) {
                getHandlePrimitiveFailedNode().executeHandle(frame, e.getReasonCode());
                LOG.finer(() -> callPrimitiveNode.primitiveNode +
                                " failed (" + ArrayUtils.toJoinedString(", ", FrameAccess.getReceiverAndArguments(frame)) + ")");
                /* continue with fallback code. */
            }
        }
        assert callPrimitiveNode.getSuccessorIndex() == CallPrimitiveNode.NUM_BYTECODES;
        return doExecuteFresh(frame);
    }

    private CallPrimitiveNode getCallPrimitiveNode() {
        if (callPrimitiveNode == null) {
            callPrimitiveNode = (CallPrimitiveNode) fetchNextBytecodeNode(0);
        }
        return callPrimitiveNode;
    }

    private HandlePrimitiveFailedNode getHandlePrimitiveFailedNode() {
        if (handlePrimitiveFailedNode == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            handlePrimitiveFailedNode = insert(HandlePrimitiveFailedNode.create(code));
        }
        return handlePrimitiveFailedNode;
    }

    @Override
    protected int startPc() {
        return CallPrimitiveNode.NUM_BYTECODES;
    }

}
