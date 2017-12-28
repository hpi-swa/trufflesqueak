package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExit;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;

public class MainContextNode extends RootNode {
    @CompilationFinal private final DispatchNode dispatchNode = DispatchNode.create();
    @CompilationFinal private final CompiledCodeObject code;
    @CompilationFinal private final ContextObject initialContext;

    public static MainContextNode create(SqueakLanguage language, ContextObject context) {
        return new MainContextNode(language, context, context.getCodeObject());
    }

    public static MethodContextNode create(SqueakLanguage language, CompiledCodeObject code, BaseSqueakObject senderContext) {
        ContextObject newContext = ContextObject.createWriteableContextObject(code.image);
        newContext.initializePointers(code.frameSize());
        newContext.atput0(CONTEXT.METHOD, code);
        newContext.atput0(CONTEXT.SENDER, senderContext);
        newContext.atput0(CONTEXT.INSTRUCTION_POINTER, code.getBytecodeOffset() + 1);
        // newContext.atput0(CONTEXT.STACKPOINTER, 0); // not needed
        return new MethodContextNode(language, newContext, code);
    }

    private MainContextNode(SqueakLanguage language, ContextObject context, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        this.initialContext = context;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ContextObject activeContext = initialContext;
        frame.setObject(code.methodSlot, code);
        while (true) {
            try {
                return dispatchNode.executeDispatch(code, activeContext.getFrameArguments());
            } catch (ProcessSwitch e) {
                activeContext = e.getNewContext();
            } catch (SqueakExit e) {
                return e.code;
            }
        }

    }
}
