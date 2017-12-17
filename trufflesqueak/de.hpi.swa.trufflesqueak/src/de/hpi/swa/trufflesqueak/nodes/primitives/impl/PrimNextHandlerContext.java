package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExit;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeUnary;

public abstract class PrimNextHandlerContext extends PrimitiveNodeUnary {
    private static final int EXCEPTION_HANDLER_MARKER = 199;

    public PrimNextHandlerContext(CompiledMethodObject code) {
        super(code);
    }

    @Specialization
    Object findNext(ContextObject receiver) {
        Object handlerContext = Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
            final Object marker = receiver.getFrameMarker();
            boolean foundSelf = false;

            @Override
            public Object visitFrame(FrameInstance frameInstance) {
                Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                FrameDescriptor frameDescriptor = current.getFrameDescriptor();
                FrameSlot methodSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.METHOD);
                FrameSlot markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
                if (methodSlot != null && markerSlot != null) {
                    Object frameMethod = FrameUtil.getObjectSafe(current, methodSlot);
                    Object frameMarker = FrameUtil.getObjectSafe(current, markerSlot);
                    if (frameMarker == marker) {
                        foundSelf = true;
                    }
                    if (foundSelf) {
                        if (frameMethod instanceof CompiledCodeObject) {
                            if (((CompiledCodeObject) frameMethod).primitiveIndex() == EXCEPTION_HANDLER_MARKER) {
                                return frameMethod;
                            }
                        }
                    }
                }
                return null;
            }
        });
        if (handlerContext == null) {
            printException();
        }
        return handlerContext;
    }

    @TruffleBoundary
    private void printException() {
        code.image.getOutput().println("=== Unhandled Error ===");
        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
            @Override
            public Object visitFrame(FrameInstance frameInstance) {
                Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                FrameDescriptor frameDescriptor = current.getFrameDescriptor();
                FrameSlot methodSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.METHOD);
                if (methodSlot != null) {
                    code.image.getOutput().println(FrameUtil.getObjectSafe(current, methodSlot));
                    for (Object arg : current.getArguments()) {
                        code.image.getOutput().append("   ");
                        code.image.getOutput().println(arg);
                    }
                }
                return null;
            }
        });
        throw new SqueakExit(1);
    }
}
