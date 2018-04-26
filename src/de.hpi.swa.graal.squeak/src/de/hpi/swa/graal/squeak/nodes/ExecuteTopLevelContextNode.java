package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.exceptions.SqueakQuit;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class ExecuteTopLevelContextNode extends RootNode {
    @CompilationFinal private final SqueakImageContext image;
    @CompilationFinal private final ContextObject initialContext;
    @Child private FrameSlotWriteNode contextWriteNode;
    @Child private ExecuteContextNode executeContextNode;

    public static ExecuteTopLevelContextNode create(final SqueakLanguage language, final ContextObject context) {
        return new ExecuteTopLevelContextNode(language, context, context.getCodeObject());
    }

    private ExecuteTopLevelContextNode(final SqueakLanguage language, final ContextObject context, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.image = code.image;
        this.initialContext = context;
        contextWriteNode = FrameSlotWriteNode.create(code.thisContextOrMarkerSlot);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            executeLoop();
        } catch (TopLevelReturn e) {
            return e.getReturnValue();
        } catch (SqueakQuit e) {
            image.getOutput().println(e.toString());
            System.exit(e.getExitCode());
        } finally {
            image.display.close();
        }
        throw new SqueakException("Top level context did not return");
    }

    public void executeLoop() {
        ContextObject activeContext = initialContext;
        while (true) {
            final BaseSqueakObject sender = activeContext.getSender();
            try {
                final CompiledCodeObject code = activeContext.getCodeObject();
                final Object[] frameArgs = activeContext.getReceiverAndArguments();
                final BlockClosureObject closure = activeContext.getClosure();
                final MaterializedFrame frame = Truffle.getRuntime().createMaterializedFrame(FrameAccess.newWith(code, sender, closure, frameArgs), code.getFrameDescriptor());
                contextWriteNode.executeWrite(frame, activeContext);
                // FIXME: do not create node here
                executeContextNode = insert(ExecuteContextNode.create(code));
                // doIt: activeContext.printSqStackTrace();
                final Object result = executeContextNode.executeNonVirtualized(frame, activeContext);
                activeContext = unwindContextChain(sender, sender, result);
                image.traceVerbose("Local Return on top-level, new context is " + activeContext);
            } catch (ProcessSwitch ps) {
                image.trace("Switching from " + activeContext + " to " + ps.getNewContext());
                activeContext = ps.getNewContext();
            } catch (NonLocalReturn nlr) {
                final BaseSqueakObject target = nlr.hasArrivedAtTargetContext() ? sender : nlr.getTargetContext().getSender();
                activeContext = unwindContextChain(sender, target, nlr.getReturnValue());
                image.traceVerbose("Non Local Return on top-level, new context is " + activeContext);
            } catch (NonVirtualReturn nvr) {
                activeContext = unwindContextChain(nvr.getCurrentContext(), nvr.getTargetContext(), nvr.getReturnValue());
                image.traceVerbose("Non Virtual Return on top-level, new context is " + activeContext);
            }
        }
    }

    private ContextObject unwindContextChain(final BaseSqueakObject startContext, final BaseSqueakObject targetContext, final Object returnValue) {
        if (startContext.isNil()) {
            throw new TopLevelReturn(returnValue);
        }
        if (!(targetContext instanceof ContextObject)) {
            throw new SqueakException("targetContext is not a ContextObject: " + targetContext.toString());
        }
        ContextObject context = (ContextObject) startContext;
        while (context != targetContext) {
            final BaseSqueakObject sender = context.getSender();
            if (sender.isNil()) {
                handleNilSender(startContext, targetContext);
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
    private void handleNilSender(final BaseSqueakObject startContext, final BaseSqueakObject targetContext) {
        image.getError().println("Unable to unwind context chain (start: " + startContext + "; target: " + targetContext + ")");
        ((ContextObject) startContext).printSqStackTrace();
    }
}
