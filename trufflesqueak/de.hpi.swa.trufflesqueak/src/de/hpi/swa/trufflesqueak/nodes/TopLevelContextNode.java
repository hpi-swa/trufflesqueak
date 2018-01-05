package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.exceptions.TopLevelReturn;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public class TopLevelContextNode extends RootNode {
    @CompilationFinal private final SqueakImageContext image;
    @CompilationFinal private final ContextObject initialContext;
    @Child private PushStackNode pushStackNode;

    public static TopLevelContextNode create(SqueakLanguage language, ContextObject context) {
        return new TopLevelContextNode(language, context, context.getCodeObject());
    }

    private TopLevelContextNode(SqueakLanguage language, ContextObject context, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.image = code.image;
        this.initialContext = context;
        pushStackNode = new PushStackNode(code);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            executeLoop(frame);
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

    public void executeLoop(VirtualFrame frame) {
        ContextObject activeContext = initialContext;
        while (true) {
            ContextObject sender = activeContext.getSender();
            try {
                RootCallTarget target = Truffle.getRuntime().createCallTarget(new MethodContextNode(image.getLanguage(), activeContext, activeContext.getCodeObject()));
                Object result = target.call(activeContext.getFrameArguments());
                activeContext = unwindContextChain(frame, sender, activeContext, result);
            } catch (ProcessSwitch ps) {
                activeContext = ps.getNewContext();
            } catch (NonLocalReturn nlr) {
                ContextObject target = nlr.hasArrivedAtTargetContext() ? activeContext : nlr.getTargetContext();
                activeContext = unwindContextChain(frame, sender, target, nlr.getReturnValue());
            } catch (NonVirtualReturn nvr) {
                activeContext = unwindContextChain(frame, nvr.getCurrentContext(), nvr.getTargetContext(),
                                nvr.getReturnValue());
            }
        }
    }

    private ContextObject unwindContextChain(VirtualFrame frame, ContextObject startContext, ContextObject targetContext, Object returnValue) {
        if (startContext == null) {
            throw new TopLevelReturn(returnValue);
        }
        ContextObject context = startContext;
        while (context != targetContext) {
            if (context == null) {
                throw new RuntimeException("Unable to unwind context chain");
            }
            ContextObject sender = context.getSender();
            context.activateUnwindContext();
            context = sender;
        }
        pushStackNode.executeWrite(frame, returnValue);
        return context;
    }
}
