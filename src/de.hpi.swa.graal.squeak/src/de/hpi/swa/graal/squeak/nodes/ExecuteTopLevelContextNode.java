package de.hpi.swa.graal.squeak.nodes;

import java.util.logging.Level;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleLogger;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
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

    @Child private ExecuteContextNode executeContextNode;
    @Child private UnwindContextChainNode unwindContextChainNode = UnwindContextChainNode.create();

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code, final boolean needsShutdown) {
        super(language, code.getFrameDescriptor());
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
            if (needsShutdown) {
                CompilerDirectives.transferToInterpreter();
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
            CompilerDirectives.transferToInterpreter();
            assert activeContext.hasMaterializedSender() : "Context must have materialized sender: " + activeContext;
            final AbstractSqueakObject sender = activeContext.getSender();
            assert sender == NilObject.SINGLETON || ((ContextObject) sender).hasTruffleFrame();
            try {
                MaterializeContextOnMethodExitNode.reset();
                final CompiledCodeObject code = activeContext.getBlockOrMethod();
                // FIXME: do not create node here?
                if (executeContextNode == null) {
                    executeContextNode = insert(ExecuteContextNode.create(code));
                } else {
                    executeContextNode.replace(ExecuteContextNode.create(code));
                }
                // doIt: activeContext.printSqStackTrace();
                final Object result = executeContextNode.executeContext(activeContext.getTruffleFrame(), activeContext);
                activeContext = unwindContextChainNode.executeUnwind(sender, sender, result);
                LOG.log(Level.FINE, "Local Return on top-level: {0}", activeContext);
            } catch (final ProcessSwitch ps) {
                activeContext = ps.getNewContext();
                assert ExecuteContextNode.getStackDepth() == 0 : "Stack depth should be zero when switching to another context";
                LOG.log(Level.FINE, "Process Switch: {0}", activeContext);
            } catch (final NonLocalReturn nlr) {
                final AbstractSqueakObject target = (AbstractSqueakObject) nlr.getTargetContextOrMarker();
                activeContext = unwindContextChainNode.executeUnwind(sender, target, nlr.getReturnValue());
                LOG.log(Level.FINE, "Non Local Return on top-level: {0}", activeContext);
            } catch (final NonVirtualReturn nvr) {
                activeContext = unwindContextChainNode.executeUnwind(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                LOG.log(Level.FINE, "Non Virtual Return on top-level: {0}", activeContext);
            }
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
            FrameAccess.setStackPointer(truffleFrame, method, 0);
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
