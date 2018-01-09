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
        // sp points to the last temp slot
        int sp = initialSP();
        assert sp >= -1;
        frame.setInt(code.stackPointerSlot, sp);
        return contextNode.execute(frame);
    }

    @Specialization(guards = {"!code.getNoContextNeededAssumption().isValid()"})
    protected Object enter(VirtualFrame frame,
                    @Cached("create(code)") GetMethodContextNode getContextNode,
                    @Cached("create(code)") MethodContextNode contextNode) {
        MethodContextObject context = getContextNode.executeGetMethodContext(frame, 0);
        frame.setInt(code.stackPointerSlot, 0);
        frame.setObject(code.markerSlot, context); // TODO: unify markerSlot and thisContextSlot
        Object[] arguments = frame.getArguments();
        for (int i = 1; i < arguments.length; i++) {
            context.push(arguments[i]);
        }
        for (int i = 0; i < code.getNumTemps(); i++) {
            context.push(code.image.nil);
        }
        return contextNode.execute(frame);
    }

    private int initialSP() {
        // no need to read context.at0(CONTEXT.STACKPOINTER)
        return code.getNumTemps() - 1;
    }

    @Override
    public String getName() {
        return code.toString();
    }
}
