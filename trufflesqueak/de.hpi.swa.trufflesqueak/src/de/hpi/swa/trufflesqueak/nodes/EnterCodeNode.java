package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class EnterCodeNode extends RootNode {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child private GetOrCreateContextNode createContextNode = GetOrCreateContextNode.create();

    public static EnterCodeNode create(SqueakLanguage language, CompiledCodeObject code) {
        return EnterCodeNodeGen.create(language, code);
    }

    protected EnterCodeNode(SqueakLanguage language, CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
    }

    @ExplodeLoop
    @Specialization(assumptions = {"code.getNoContextNeededAssumption()"})
    protected Object enterVirtualized(VirtualFrame frame,
                    @Cached("create(code)") ExecuteContextNode contextNode) {
        CompilerDirectives.ensureVirtualized(frame);
        FrameAccess.initializeCodeSlots(frame);
        int numTemps = code.getNumTemps();
        // Initialize temps with nil in newContext.
        for (int i = 0; i < numTemps - code.getNumArgsAndCopiedValues(); i++) {
            frame.setObject(code.stackSlots[i], code.image.nil);
        }
        FrameAccess.setStackPointer(frame, numTemps);
        return contextNode.executeVirtualized(frame);
    }

    @ExplodeLoop
    @Specialization(guards = {"!code.getNoContextNeededAssumption().isValid()"})
    protected Object enter(VirtualFrame frame,
                    @Cached("create(code)") ExecuteContextNode contextNode) {
        FrameAccess.initializeCodeSlots(frame); //
        ContextObject newContext = createContextNode.executeGet(frame, true);
        Object[] arguments = frame.getArguments();
        // Push arguments and copied values onto the newContext.
        int numArgsAndCopiedValues = code.getNumArgsAndCopiedValues();
        for (int i = 0; i < numArgsAndCopiedValues; i++) {
            newContext.push(arguments[FrameAccess.RCVR_AND_ARGS_START + 1 + i]);
        }
        // Initialize temps with nil in newContext.
        int numTemps = code.getNumTemps();
        for (int i = 0; i < numTemps - numArgsAndCopiedValues; i++) {
            newContext.push(code.image.nil);
        }
        assert (int) newContext.at0(CONTEXT.STACKPOINTER) == numTemps;
        return contextNode.executeNonVirtualized(frame);
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        return code.toString();
    }
}
