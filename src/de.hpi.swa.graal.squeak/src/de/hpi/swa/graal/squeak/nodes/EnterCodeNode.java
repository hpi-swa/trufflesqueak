package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetNumAllArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public abstract class EnterCodeNode extends RootNode {
    @CompilationFinal protected final CompiledCodeObject code;
    @Child private GetOrCreateContextNode createContextNode = GetOrCreateContextNode.create();
    @Child private ExecuteContextNode executeContextNode;
    @Child private FrameSlotWriteNode contextWriteNode = FrameSlotWriteNode.createForContextOrMarker();
    @Child private FrameSlotWriteNode instructionPointerWriteNode = FrameSlotWriteNode.createForInstructionPointer();
    @Child private FrameSlotWriteNode stackPointerWriteNode = FrameSlotWriteNode.createForStackPointer();
    @Child private FrameSlotReadNode stackPointerReadNode = FrameSlotReadNode.createForStackPointer();
    @Child private StackPushNode pushStackNode = StackPushNode.create();
    @Child private GetNumAllArgumentsNode getNumAllArgumentsNode = GetNumAllArgumentsNode.create();

    public static EnterCodeNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return EnterCodeNodeGen.create(language, code);
    }

    protected EnterCodeNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        executeContextNode = ExecuteContextNode.create(code);
    }

    private void initializeSlots(final VirtualFrame frame) {
        instructionPointerWriteNode.executeWrite(frame, 0);
        stackPointerWriteNode.executeWrite(frame, -1);
    }

    @ExplodeLoop
    @Specialization(assumptions = {"code.getCanBeVirtualizedAssumption()"})
    protected Object enterVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        initializeSlots(frame);
        contextWriteNode.executeWrite(frame, new FrameMarker());
        // Push arguments and copied values onto the newContext.
        final Object[] arguments = frame.getArguments();
        assert getNumAllArgumentsNode.execute(code) == (arguments.length - FrameAccess.ARGUMENTS_START);
        for (int i = 0; i < getNumAllArgumentsNode.execute(code); i++) {
            pushStackNode.executeWrite(frame, arguments[FrameAccess.ARGUMENTS_START + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            pushStackNode.executeWrite(frame, code.image.nil);
        }
        assert ((int) stackPointerReadNode.executeRead(frame)) + 1 >= remainingTemps;
        return executeContextNode.executeVirtualized(frame);
    }

    @ExplodeLoop
    @Specialization(guards = {"!code.getCanBeVirtualizedAssumption().isValid()"})
    protected Object enter(final VirtualFrame frame) {
        initializeSlots(frame);
        final ContextObject newContext = createContextNode.executeGet(frame, true, true);
        contextWriteNode.executeWrite(frame, newContext);
        // Push arguments and copied values onto the newContext.
        final Object[] arguments = frame.getArguments();
        assert getNumAllArgumentsNode.execute(code) == (arguments.length - FrameAccess.ARGUMENTS_START);
        for (int i = 0; i < getNumAllArgumentsNode.execute(code); i++) {
            newContext.push(arguments[FrameAccess.ARGUMENTS_START + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            newContext.push(code.image.nil);
        }
        assert newContext.getStackPointer() >= remainingTemps;
        return executeContextNode.executeNonVirtualized(frame, newContext);
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
