/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLogger;
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
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ExecuteTopLevelContextNode extends RootNode {
    private static final TruffleLogger LOG = TruffleLogger.getLogger(SqueakLanguageConfig.ID, ExecuteTopLevelContextNode.class);

    private final SqueakImageContext image;
    private final ContextObject initialContext;
    private final boolean needsShutdown;

    @Child private UnwindContextChainNode unwindContextChainNode = UnwindContextChainNode.create();
    @Child private IndirectCallNode callNode = IndirectCallNode.create();

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code, final boolean needsShutdown) {
        super(language, new FrameDescriptor());
        image = code.image;
        initialContext = context;
        this.needsShutdown = needsShutdown;
    }

    public static ExecuteTopLevelContextNode create(final SqueakLanguage language, final ContextObject context, final boolean needsShutdown) {
        return new ExecuteTopLevelContextNode(language, context, context.getBlockOrMethod(), needsShutdown);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeLoop();
        } catch (final TopLevelReturn e) {
            return e.getReturnValue();
        } finally {
            CompilerAsserts.neverPartOfCompilation();
            if (needsShutdown) {
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
        ensureCachedContextCanRunAgain(activeContext);
        while (true) {
            assert activeContext.hasMaterializedSender() : "Context must have materialized sender: " + activeContext;
            final AbstractSqueakObject sender = activeContext.getSender();
            assert sender == NilObject.SINGLETON || ((ContextObject) sender).hasTruffleFrame();
            try {
                image.lastSeenContext = null;  // Reset materialization mechanism.
                // doIt: activeContext.printSqStackTrace();
                StringBuilder b = new StringBuilder("Starting top level stack trace:\n");
                activeContext.printSqMaterializedStackTraceOn(b);
                LOG.fine(b.toString());
                final Object result = callNode.call(activeContext.getCallTarget());
                activeContext = unwindContextChainNode.executeUnwind(sender, sender, result);
                b = new StringBuilder("Local Return on top-level:\n");
                activeContext.printSqMaterializedStackTraceOn(b);
                LOG.fine(b.toString());
            } catch (final ProcessSwitch ps) {
                activeContext = ps.getNewContext();
            } catch (final NonLocalReturn nlr) {
                final ContextObject target = (ContextObject) nlr.getTargetContextOrMarker();
                activeContext = unwindContextChainNode.executeUnwind(sender, target, nlr.getReturnValue());
                final StringBuilder b = new StringBuilder("Non Local Return on top-level:\n");
                activeContext.printSqMaterializedStackTraceOn(b);
                LOG.fine(b.toString());
            } catch (final NonVirtualReturn nvr) {
                activeContext = unwindContextChainNode.executeUnwind(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                final StringBuilder b = new StringBuilder("Non Virtual Return on top-level:\n");
                activeContext.printSqMaterializedStackTraceOn(b);
                LOG.fine(b.toString());
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
