package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakQuit;
import de.hpi.swa.trufflesqueak.exceptions.TopLevelReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.KnownClasses.CONTEXT;

public class TopLevelContextNode extends RootNode {
    @CompilationFinal private final DispatchNode dispatchNode = DispatchNode.create();
    @CompilationFinal private final CompiledCodeObject code;
    @CompilationFinal private final ContextObject initialContext;

    public static TopLevelContextNode create(SqueakLanguage language, ContextObject context) {
        return new TopLevelContextNode(language, context, context.getCodeObject());
    }

    public static TopLevelContextNode create(SqueakLanguage language, Object receiver, CompiledCodeObject code, BaseSqueakObject senderContext) {
        ContextObject newContext = ContextObject.createWriteableContextObject(code.image, code.frameSize());
        newContext.atput0(CONTEXT.INSTRUCTION_POINTER, code.getBytecodeOffset() + 1);
        newContext.atput0(CONTEXT.METHOD, code);
        newContext.atput0(CONTEXT.RECEIVER, receiver);
        newContext.atput0(CONTEXT.SENDER, senderContext);
        // newContext.atput0(CONTEXT.STACKPOINTER, 0); // not needed
        return new TopLevelContextNode(language, newContext, code);
    }

    private TopLevelContextNode(SqueakLanguage language, ContextObject context, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        this.initialContext = context;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            executeLoop(frame);
        } catch (TopLevelReturn e) {
            return e.returnValue;
        } catch (SqueakQuit e) {
            System.out.println("Squeak is quitting...");
            System.exit(e.getExitCode());
        } finally {
            code.image.display.close();
        }
        throw new RuntimeException("Top level context did not return");
    }

    public Object executeLoop(VirtualFrame frame) {
        ContextObject activeContext = initialContext;
        while (true) {
            frame.setObject(code.thisContextSlot, activeContext);
            try {
                return dispatchNode.executeDispatch(activeContext.getCodeObject(), activeContext.getFrameArguments());
            } catch (ProcessSwitch e) {
                activeContext = e.getNewContext();
            }
        }
    }
}
