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
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchSelector2Node.Dispatch2Node;
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
    @Child private Dispatch1Node sendCannotReturnNode;
    @Child private Dispatch2Node sendAboutToReturnNode;

    private ExecuteTopLevelContextNode(final SqueakImageContext image, final SqueakLanguage language, final ContextObject context, final boolean isImageResuming) {
        super(language, TOP_LEVEL_FRAME_DESCRIPTOR);
        this.image = image;
        initialContext = context;
        this.isImageResuming = isImageResuming;
        sendCannotReturnNode = Dispatch1Node.create(image.cannotReturn);
        sendAboutToReturnNode = Dispatch2Node.create(image.aboutToReturnSelector);
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
            assert activeContext.hasMaterializedSender() : "Context must have materialized sender: " + activeContext;
            final AbstractSqueakObject sender = activeContext.getSender();
            assert sender == NilObject.SINGLETON || ((ContextObject) sender).hasTruffleFrame();
            try {
                try {
                    image.lastSeenContext = null;  // Reset materialization mechanism.
                    image.resetContextStackDepth();
                    final Object result = callNode.call(activeContext.getCallTarget());
                    activeContext = returnTo(activeContext, sender, result);
                    LogUtils.SCHEDULING.log(Level.FINE, "Local Return on top-level: {0}", activeContext);
                } catch (final NonLocalReturn nlr) {
                    activeContext = commonNLReturn(sender, nlr);
                    LogUtils.SCHEDULING.log(Level.FINE, "Non Local Return on top-level: {0}", activeContext);
                } catch (final NonVirtualReturn nvr) {
                    activeContext = commonReturn(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                    LogUtils.SCHEDULING.log(Level.FINE, "Non Virtual Return on top-level: {0}", activeContext);
                }
            } catch (final ProcessSwitch ps) {
                activeContext = ps.getNewContext();
                LogUtils.SCHEDULING.log(Level.FINE, "Process Switch: {0}", activeContext);
            }
        }
    }

    @TruffleBoundary
    private static ContextObject returnTo(final ContextObject activeContext, final AbstractSqueakObject sender, final Object returnValue) {
        if (!(sender instanceof final ContextObject senderContext)) {
            assert sender == NilObject.SINGLETON;
            throw returnToTopLevel(activeContext, returnValue);
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
    private ContextObject commonNLReturn(final AbstractSqueakObject sender, final NonLocalReturn nlr) {
        final ContextObject targetContext = nlr.getTargetContext();
        final Object returnValue = nlr.getReturnValue();
        if (!(sender instanceof final ContextObject senderContext)) {
            assert sender == NilObject.SINGLETON;
            throw returnToTopLevel(targetContext, returnValue);
        }
        ContextObject context = senderContext;
        while (context != targetContext) {
            if (context.getCodeObject().isUnwindMarked()) {
                try {
                    // TODO: make this better
                    AboutToReturnNode.create(context.getCodeObject()).executeAboutToReturn(context.getTruffleFrame(), nlr);
                } catch (NonVirtualReturn nvr) {
                    return commonReturn(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                }
            }
            final AbstractSqueakObject currentSender = context.getSender();
            if (currentSender instanceof final ContextObject o) {
                context = o;
            }
        }
        context = senderContext;
        while (context != targetContext) {
            final AbstractSqueakObject currentSender = context.getSender();
            if (currentSender instanceof final ContextObject o) {
                context.terminate();
                context = o;
            } else { // TODO: this might need to be handled by a cannotReturn send.
                image.printToStdErr("Unwind error: sender of", context, "is nil, unwinding towards", targetContext, "with return value:", returnValue);
                break;
            }
        }
        targetContext.push(returnValue);
        return targetContext;
    }

    @TruffleBoundary
    private ContextObject commonReturn(final ContextObject startContext, final ContextObject targetContext, final Object returnValue) {
        /* "make sure we can return to the given context" */
        if (!targetContext.hasClosure() && !targetContext.canBeReturnedTo()) {
            if (startContext == targetContext) {
                throw returnToTopLevel(targetContext, returnValue);
            }
            return sendCannotReturn(startContext, returnValue);
        }
        /*
         * "If this return is not to our immediate predecessor (i.e. from a method to its sender, or
         * from a block to its caller), scan the stack for the first unwind marked context and
         * inform this context and let it deal with it. This provides a chance for ensure unwinding
         * to occur."
         */
        AbstractSqueakObject contextOrNil = startContext;
        while (contextOrNil != targetContext) {
            if (!(contextOrNil instanceof final ContextObject context)) {
                /* "error: sender's instruction pointer or context is nil; cannot return" */
                assert contextOrNil == NilObject.SINGLETON;
                return sendCannotReturn(startContext, returnValue);
            }
            assert !context.isPrimitiveContext();
            if (context.getCodeObject().isUnwindMarked()) {
                assert !context.hasClosure();
                /* "context is marked; break out" */
                return sendAboutToReturn(startContext, returnValue, context);
            }
            contextOrNil = context.getSender();
        }
        /*
         * "If we get here there is no unwind to worry about. Simply terminate the stack up to the
         * localCntx - often just the sender of the method"
         */
        ContextObject currentContext = startContext;
        while (currentContext != targetContext) {
            final ContextObject sender = (ContextObject) currentContext.getFrameSender();
            currentContext.terminate();
            currentContext = sender;
        }
        targetContext.push(returnValue);
        return targetContext;
    }

    private static TopLevelReturn returnToTopLevel(final ContextObject targetContext, final Object returnValue) {
        assert "DoIt".equals(targetContext.getCodeObject().getCompiledInSelector().asStringUnsafe()) : DebugUtils.getSqStackTrace(targetContext);
        throw new TopLevelReturn(returnValue);
    }

    private ContextObject sendCannotReturn(final ContextObject startContext, final Object returnValue) {
        sendCannotReturnNode.execute(startContext.getTruffleFrame(), startContext, returnValue);
        throw CompilerDirectives.shouldNotReachHere("cannotReturn should trigger a ProcessSwitch");
    }

    private ContextObject sendAboutToReturn(final ContextObject startContext, final Object returnValue, final ContextObject context) {
        try {
            sendAboutToReturnNode.execute(startContext.getTruffleFrame(), startContext, returnValue, context);
        } catch (final NonVirtualReturn nvr) {
            return commonReturn(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
        }
        throw CompilerDirectives.shouldNotReachHere("aboutToReturn should trigger a ProcessSwitch or a NonVirtualReturn");
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
            FrameAccess.setInstructionPointer(truffleFrame, method.getInitialPC());
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
