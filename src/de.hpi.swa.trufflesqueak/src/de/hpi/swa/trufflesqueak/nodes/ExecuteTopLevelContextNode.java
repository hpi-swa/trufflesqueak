/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.CannotReturnToTarget;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1Node.Dispatch1Node;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector1NodeFactory.Dispatch1NodeGen;
import de.hpi.swa.trufflesqueak.nodes.process.GetNextActiveContextNode;
import de.hpi.swa.trufflesqueak.nodes.process.WakeHighestPriorityNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.DebugUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.LogUtils;

@NodeInfo(language = SqueakLanguageConfig.ID)
public final class ExecuteTopLevelContextNode extends RootNode {
    private static final FrameDescriptor TOP_LEVEL_FRAME_DESCRIPTOR = new FrameDescriptor();

    private final SqueakImageContext image;
    private final boolean isImageResuming;
    private ContextObject initialContext;

    @Child private IndirectCallNode callNode = IndirectCallNode.create();
    @Child private GetNextActiveContextNode getNextActiveContextNode = GetNextActiveContextNode.create();
    @Child private Dispatch1Node sendCannotReturnNode;

    private ExecuteTopLevelContextNode(final SqueakImageContext image, final SqueakLanguage language, final ContextObject context, final boolean isImageResuming) {
        super(language, TOP_LEVEL_FRAME_DESCRIPTOR);
        this.image = image;
        initialContext = context;
        this.isImageResuming = isImageResuming;
        sendCannotReturnNode = Dispatch1NodeGen.create(image.cannotReturn);
    }

    public static ExecuteTopLevelContextNode create(final SqueakImageContext image, final SqueakLanguage language, final ContextObject context, final boolean isImageResuming) {
        return new ExecuteTopLevelContextNode(image, language, context, isImageResuming);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeLoop();
        } catch (final TopLevelReturn e) {
            return e.getReturnValue();
        } finally {
            if (isImageResuming) {
                image.interrupt.shutdown();
                if (image.hasDisplay()) {
                    image.getDisplay().close();
                }
            }
        }
        throw SqueakException.create("Top level context did not return");
    }

    private void executeLoop() {
        ContextObject activeContext = initialContext;
        if (isImageResuming) {
            /*
             * Free initialContext if resuming an image. Headless code execution requests can be
             * cached by Truffle. Therefore, they must keep their initialContext, so that they can
             * be restarted.
             */
            initialContext = null;
        } else {
            ensureCachedContextCanRunAgain(activeContext);
        }
        while (true) {
            final AbstractSqueakObject sender = activeContext.getSender();
            assert sender == NilObject.SINGLETON || ((ContextObject) sender).hasTruffleFrame();
            try {
                try {
                    image.resetContextStackDepth();
                    final Object result = callNode.call(activeContext.getCallTarget());
                    activeContext = returnTo(activeContext, sender, result);
                    LogUtils.SCHEDULING.log(Level.FINE, "Local Return on top-level: {0}", activeContext);
                } catch (final NonLocalReturn nlr) {
                    activeContext = commonNLReturn(sender, activeContext, nlr);
                    LogUtils.SCHEDULING.log(Level.FINE, "Non Local Return on top-level: {0}", activeContext);
                } catch (final NonVirtualReturn nvr) {
                    activeContext = commonNVReturn(activeContext, nvr);
                    LogUtils.SCHEDULING.log(Level.FINE, "Non Virtual Return on top-level: {0}", activeContext);
                } catch (final CannotReturnToTarget cr) {
                    activeContext = sendCannotReturn(cr.getTargetContext(), cr.getReturnValue());
                    LogUtils.SCHEDULING.log(Level.FINE, "Cannot Return on top-level: {0}", activeContext);
                }
            } catch (final ProcessSwitch ps) {
                activeContext = getNextActiveContextNode.execute();
                LogUtils.SCHEDULING.log(Level.FINE, "Process Switch: {0}", activeContext);
            }
        }
    }

    @TruffleBoundary
    private ContextObject sendCannotReturnOrReturnToTopLevel(final ContextObject startContext, final ContextObject targetContext, final Object returnValue) {
        // Exit the interpreter loop if the target is the context that started the loop.
        if (targetContext != null && targetContext == initialContext) {
            throw returnToTopLevel(targetContext, returnValue);
        }
        return sendCannotReturn(startContext, returnValue);
    }

    @TruffleBoundary
    private ContextObject returnTo(final ContextObject activeContext, final AbstractSqueakObject sender, final Object returnValue) {
        if (!(sender instanceof final ContextObject senderContext)) {
            assert sender == NilObject.SINGLETON;
            return sendCannotReturnOrReturnToTopLevel(activeContext, activeContext, returnValue);
        } else if (senderContext.isDead()) {
            return sendCannotReturnOrReturnToTopLevel(activeContext, senderContext, returnValue);
        }
        final ContextObject context;
        if (senderContext.isPrimitiveContext()) {
            context = (ContextObject) senderContext.getFrameSender(); // skip primitive contexts.
        } else {
            context = senderContext;
        }
        context.push(returnValue);
        return context;
    }

    @TruffleBoundary
    private ContextObject commonNVReturn(final ContextObject activeContext, final NonVirtualReturn nvr) {
        // Normal returns with modified senders end up here with a target but no start Context.
        final Object returnValue = nvr.getReturnValue();
        final ContextObject possibleTargetContext = nvr.getTargetContext();
        final ContextObject targetContext;
        // Skip over primitive contexts.
        if (possibleTargetContext.isPrimitiveContext()) {
            targetContext = (ContextObject) possibleTargetContext.getFrameSender();
        } else {
            targetContext = possibleTargetContext;
        }
        // Make sure that the targetContext can be returned to.
        if (!targetContext.hasClosure() && !targetContext.canBeReturnedTo()) {
            return sendCannotReturnOrReturnToTopLevel(activeContext, targetContext, returnValue);
        }
        // Return to the target context with the return value.
        targetContext.push(returnValue);
        return targetContext;
    }

    @TruffleBoundary
    private ContextObject commonNLReturn(final AbstractSqueakObject sender, final ContextObject activeContext, final NonLocalReturn nlr) {
        final ContextObject targetContext = nlr.getTargetContext();
        final Object returnValue = nlr.getReturnValue();
        if (sender == NilObject.SINGLETON) {
            return sendCannotReturnOrReturnToTopLevel(activeContext, targetContext, returnValue);
        }
        // Terminate the Contexts on sender chain.
        ContextObject context = (ContextObject) sender;
        while (context != targetContext) {
            final ContextObject currentSender = (ContextObject) context.getSender();
            context.terminate();
            context = currentSender;
        }
        targetContext.push(returnValue);
        return targetContext;
    }

    private static TopLevelReturn returnToTopLevel(final ContextObject targetContext, final Object returnValue) {
        assert "DoIt".equals(targetContext.getCodeObject().getCompiledInSelector().asStringUnsafe()) : DebugUtils.getSqStackTrace(targetContext);
        throw new TopLevelReturn(returnValue);
    }

    @TruffleBoundary
    private ContextObject sendCannotReturn(final ContextObject startContext, final Object returnValue) {
        try {
            sendCannotReturnNode.execute(startContext.getTruffleFrame(), startContext, returnValue);
            WakeHighestPriorityNode.executeAndThrowUncached(startContext.getTruffleFrame(), image);
        } catch (final NonVirtualReturn nvr) {
            return commonNVReturn(startContext, nvr);
        }
        throw CompilerDirectives.shouldNotReachHere("cannotReturn should trigger a ProcessSwitch or a NonVirtualReturn");
    }

    private static void ensureCachedContextCanRunAgain(final ContextObject activeContext) {
        if (activeContext.getInstructionPointerForBytecodeLoop() != 0) {
            /*
             * Reset instruction pointer and stack pointer of the context (see {@link
             * EnterCodeNode#initializeSlots}) in case it has previously been executed and needs to
             * run again, for example because the Source has been cached.
             */
            assert !activeContext.hasClosure() : "activeContext is expected to have no closure";
            final CompiledCodeObject method = activeContext.getCodeObject();
            final MaterializedFrame truffleFrame = activeContext.getTruffleFrame();
            FrameAccess.setInstructionPointer(truffleFrame, 0);
            FrameAccess.setStackPointer(truffleFrame, method.getNumTemps());
        }
    }

    @Override
    public String getName() {
        return "<" + SqueakLanguageConfig.ID + "-toplevel>";
    }

    @Override
    public boolean isInternal() {
        return true;
    }
}
