package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

@ImportStatic(FrameAccess.class)
public abstract class EnterMethodNode extends RootNode {
    @CompilationFinal protected final CompiledCodeObject code;

    public static EnterMethodNode create(SqueakLanguage language, CompiledCodeObject code) {
        return EnterMethodNodeGen.create(language, code);
    }

    protected EnterMethodNode(SqueakLanguage language, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
    }

    @Specialization(assumptions = {"code.getNoContextNeededAssumption()"})
    protected Object enterVirtualized(VirtualFrame frame,
                    @Cached("create(code)") MethodContextNode contextNode) {
        CompilerDirectives.ensureVirtualized(frame);
        frame.setObject(code.markerSlot, new FrameMarker());
        frame.setObject(code.thisContextSlot, null);
        int numTemps = code.getNumTemps() - code.getNumArgsAndCopiedValues();
        assert numTemps >= 0;
        for (int i = 0; i < numTemps; i++) {
            frame.setObject(code.stackSlots[i], code.image.nil);
        }
        frame.setInt(code.stackPointerSlot, numTemps); // sp points to the last temp slot
        return contextNode.execute(frame);
    }

    @Specialization(guards = {"!code.getNoContextNeededAssumption().isValid()"})
    protected Object enter(VirtualFrame frame,
                    @Cached("create(code)") GetMethodContextNode getContextNode,
                    @Cached("create(code)") MethodContextNode contextNode) {
        MethodContextObject context = getContextNode.executeGetMethodContext(frame, code.getBytecodeOffset() + 1);
        frame.setObject(code.markerSlot, context); // TODO: unify markerSlot and thisContextSlot
        Object[] arguments = frame.getArguments();
        assert arguments.length - (FrameAccess.RCVR_AND_ARGS_START + 1) == code.getNumArgs();
        for (int i = FrameAccess.RCVR_AND_ARGS_START + 1; i < arguments.length; i++) {
            context.push(arguments[i]);
        }
        int numTemps = code.getNumTemps() - code.getNumArgsAndCopiedValues();
        for (int i = 0; i < numTemps; i++) {
            context.push(code.image.nil);
        }
        return contextNode.execute(frame);
    }

    @Override
    public String getName() {
        return code.toString();
    }
}
