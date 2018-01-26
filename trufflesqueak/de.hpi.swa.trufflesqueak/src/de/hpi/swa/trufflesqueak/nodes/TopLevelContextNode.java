package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.trufflesqueak.exceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public class TopLevelContextNode extends RootNode {
    @CompilationFinal private final SqueakImageContext image;
    @CompilationFinal private final ContextObject initialContext;

    public static TopLevelContextNode create(SqueakLanguage language, ContextObject context) {
        return new TopLevelContextNode(language, context, context.getCodeObject());
    }

    private TopLevelContextNode(SqueakLanguage language, ContextObject context, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.image = code.image;
        this.initialContext = context;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            executeLoop();
        } catch (TopLevelReturn e) {
            return e.getReturnValue();
        } catch (SqueakQuit e) {
            System.out.println("Squeak is quitting...");
            System.exit(e.getExitCode());
        } finally {
            image.display.close();
        }
        throw new RuntimeException("Top level context did not return");
    }

    public void executeLoop() {
        ContextObject activeContext = initialContext;
        while (true) {
            ContextObject sender = activeContext.getSender();
            try {
                CompiledCodeObject code = activeContext.getCodeObject();
                Object[] frameArgs = activeContext.getReceiverAndArguments();
                BlockClosureObject closure = activeContext.getClosure();
                VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(FrameAccess.newWith(code, sender, closure, frameArgs), code.getFrameDescriptor());
                frame.setObject(code.thisContextSlot, activeContext);
                frame.setObject(code.markerSlot, new FrameMarker());
                Object result = new MethodContextNode(code).execute(frame); // TODO don't generate node here
                activeContext = unwindContextChain(sender, activeContext, result);
            } catch (ProcessSwitch ps) {
                activeContext = ps.getNewContext();
            } catch (NonLocalReturn nlr) {
                ContextObject target = nlr.hasArrivedAtTargetContext() ? sender : nlr.getTargetContext(image);
                activeContext = unwindContextChain(sender, target, nlr.getReturnValue());
            } catch (NonVirtualReturn nvr) {
                activeContext = unwindContextChain(nvr.getCurrentContext(), nvr.getTargetContext(),
                                nvr.getReturnValue());
            }
        }
    }

    private static ContextObject unwindContextChain(ContextObject startContext, ContextObject targetContext, Object returnValue) {
        if (startContext == null) {
            throw new TopLevelReturn(returnValue);
        }
        ContextObject context = startContext;
        while (context != targetContext) {
            if (context == null) {
                throw new RuntimeException("Unable to unwind context chain");
            }
            ContextObject sender = context.getSender();
            context.terminate();
            context = sender;
        }
        context.push(returnValue);
        return context;
    }
}
