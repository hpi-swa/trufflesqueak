package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;

public final class ExecuteTopLevelContextNode extends RootNode {
    private final SqueakImageContext image;
    private final ContextObject initialContext;
    @Child private ExecuteContextNode executeContextNode;
    @Child private UnwindContextChainNode unwindContextChainNode;

    public static ExecuteTopLevelContextNode create(final SqueakLanguage language, final ContextObject context) {
        return new ExecuteTopLevelContextNode(language, context, context.getClosureOrMethod());
    }

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        image = code.image;
        initialContext = context;
        unwindContextChainNode = UnwindContextChainNode.create(image);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeLoop();
        } catch (TopLevelReturn e) {
            return e.getReturnValue();
        } catch (SqueakQuit e) {
            image.printToStdOut(e);
            throw e;
        } finally {
            image.interrupt.shutdown();
            image.getDisplay().close();
        }
        throw new SqueakException("Top level context did not return");
    }

    private void executeLoop() {
        ContextObject activeContext = initialContext;
        while (true) {
            CompilerDirectives.transferToInterpreter();
            final AbstractSqueakObject sender = activeContext.getSender();
            try {
                MaterializeContextOnMethodExitNode.reset();
                final CompiledCodeObject code = activeContext.getClosureOrMethod();
                // FIXME: do not create node here?
                if (executeContextNode == null) {
                    executeContextNode = insert(ExecuteContextNode.create(code));
                } else {
                    executeContextNode.replace(ExecuteContextNode.create(code));
                }
                // doIt: activeContext.printSqStackTrace();
                final Object result = executeContextNode.executeContext(activeContext.getTruffleFrame(code.getNumArgsAndCopied()), activeContext);
                image.traceVerbose("Local Return on top-level: sender: ", sender);
                activeContext = unwindContextChainNode.executeUnwind(sender, sender, result);
                image.traceVerbose("Local Return on top-level, new context is ", activeContext);
            } catch (ProcessSwitch ps) {
                image.trace("Switching from", activeContext, "to", ps.getNewContext());
                activeContext = ps.getNewContext();
            } catch (NonLocalReturn nlr) {
                final AbstractSqueakObject target = nlr.hasArrivedAtTargetContext() ? sender : nlr.getTargetContext().getSender();
                activeContext = unwindContextChainNode.executeUnwind(sender, target, nlr.getReturnValue());
                image.traceVerbose("Non Local Return on top-level, new context is ", activeContext);
            } catch (NonVirtualReturn nvr) {
                activeContext = unwindContextChainNode.executeUnwind(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                image.traceVerbose("Non Virtual Return on top-level, new context is ", activeContext);
            }
        }
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }
}
