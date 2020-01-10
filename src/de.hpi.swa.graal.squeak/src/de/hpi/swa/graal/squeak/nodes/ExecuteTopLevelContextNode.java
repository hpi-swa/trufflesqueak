/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.context.UnwindContextChainNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.util.DebugUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.LogUtils;

public final class ExecuteTopLevelContextNode extends RootNode {
    private final SqueakImageContext image;
    private final boolean isImageResuming;
    private ContextObject initialContext;

    @Child private UnwindContextChainNode unwindContextChainNode = UnwindContextChainNode.create();
    @Child private IndirectCallNode callNode = IndirectCallNode.create();

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code, final boolean isImageResuming) {
        super(language, new FrameDescriptor());
        image = code.image;
        initialContext = context;
        this.isImageResuming = isImageResuming;
    }

    public static ExecuteTopLevelContextNode create(final SqueakLanguage language, final ContextObject context, final boolean isImageResuming) {
        return new ExecuteTopLevelContextNode(language, context, context.getBlockOrMethod(), isImageResuming);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeLoop();
        } catch (final TopLevelReturn e) {
            return e.getReturnValue();
        } finally {
            CompilerAsserts.neverPartOfCompilation();
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
                image.lastSeenContext = null;  // Reset materialization mechanism.
                final ContextObject active = activeContext;
                LogUtils.SCHEDULING.fine(() -> {
                    final StringBuilder b = new StringBuilder("Starting top level stack trace:\n");
                    DebugUtils.printSqMaterializedStackTraceOn(b, active);
                    return b.toString();
                });
                final Object result = callNode.call(activeContext.getCallTarget());
                activeContext = unwindContextChainNode.executeUnwind(sender, sender, result);
                LogUtils.SCHEDULING.log(Level.FINE, "Local Return on top-level: {0}", activeContext);
            } catch (final ProcessSwitch ps) {
                activeContext = ps.getNewContext();
                LogUtils.SCHEDULING.log(Level.FINE, "Process Switch: {0}", activeContext);
            } catch (final NonLocalReturn nlr) {
                final ContextObject target = (ContextObject) nlr.getTargetContextOrMarker();
                activeContext = unwindContextChainNode.executeUnwind(sender, target, nlr.getReturnValue());
                LogUtils.SCHEDULING.log(Level.FINE, "Non Local Return on top-level: {0}", activeContext);
            } catch (final NonVirtualReturn nvr) {
                activeContext = unwindContextChainNode.executeUnwind(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                LogUtils.SCHEDULING.log(Level.FINE, "Non Virtual Return on top-level: {0}", activeContext);
            }
            assert image.stackDepth == 0 : "Stack depth should be zero before switching to another context";
        }
    }

    private void ensureCachedContextCanRunAgain(final ContextObject activeContext) {
        if (activeContext.isTerminated() && image.getLastParseRequestSource().isCached()) {
            /**
             * Reset instruction pointer and stack pointer of the context (see
             * {@link EnterCodeNode#initializeSlots}) in case it has previously been executed and
             * needs to run again, because the Source has been cached.
             */
            assert !activeContext.hasClosure() : "activeContext is expected to have no closure";
            final CompiledMethodObject method = activeContext.getMethod();
            final MaterializedFrame truffleFrame = activeContext.getTruffleFrame();
            FrameAccess.setInstructionPointer(truffleFrame, method, 0);
            FrameAccess.setStackPointer(truffleFrame, method, method.getNumTemps());
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
