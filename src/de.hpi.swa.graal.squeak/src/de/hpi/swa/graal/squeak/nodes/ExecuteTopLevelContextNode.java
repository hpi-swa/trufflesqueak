package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
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

    public static ExecuteTopLevelContextNode create(final SqueakLanguage language, final ContextObject context) {
        return new ExecuteTopLevelContextNode(language, context, context.getClosureOrMethod());
    }

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        image = code.image;
        initialContext = context;
        image.interrupt.initializeSignalSemaphoreNode(initialContext.getMethod());
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

    public void executeLoop() {
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
                activeContext = unwindContextChain(sender, sender, result);
                image.traceVerbose("Local Return on top-level, new context is ", activeContext);
            } catch (ProcessSwitch ps) {
                image.trace("Switching from", activeContext, "to", ps.getNewContext());
                activeContext = ps.getNewContext();
            } catch (NonLocalReturn nlr) {
                final AbstractSqueakObject target = nlr.hasArrivedAtTargetContext() ? sender : nlr.getTargetContext().getSender();
                activeContext = unwindContextChain(sender, target, nlr.getReturnValue());
                image.traceVerbose("Non Local Return on top-level, new context is ", activeContext);
            } catch (NonVirtualReturn nvr) {
                activeContext = unwindContextChain(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                image.traceVerbose("Non Virtual Return on top-level, new context is ", activeContext);
            }
        }
    }

    private ContextObject unwindContextChain(final AbstractSqueakObject startContext, final AbstractSqueakObject targetContext, final Object returnValue) {
        if (startContext.isNil()) {
            throw new TopLevelReturn(returnValue);
        }
        ContextObject context = (ContextObject) startContext;
        while (context != targetContext) {
            final AbstractSqueakObject sender = context.getSender();
            if (sender.isNil()) {
                handleNilSender(startContext, targetContext); // FIXME
                context = (ContextObject) targetContext;
                break;
            }
            context.terminate();
            context = (ContextObject) sender;
        }
        context.push(returnValue);
        return context;
    }

    @TruffleBoundary
    private void handleNilSender(final AbstractSqueakObject startContext, final AbstractSqueakObject targetContext) {
        image.printToStdErr("Unable to unwind context chain (start: " + startContext + "; target: " + targetContext + ")");
        ((ContextObject) startContext).printSqStackTrace();
    }
}
