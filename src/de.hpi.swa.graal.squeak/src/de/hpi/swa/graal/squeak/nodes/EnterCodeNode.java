package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FrameMarker;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotReadNode;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameSlotWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@GenerateWrapper
public abstract class EnterCodeNode extends Node implements InstrumentableNode {
    protected final CompiledCodeObject code;
    @CompilationFinal private SourceSection sourceSection;
    @Child private GetOrCreateContextNode createContextNode = GetOrCreateContextNode.create();
    @Child private ExecuteContextNode executeContextNode;
    @Child private FrameSlotWriteNode contextWriteNode = FrameSlotWriteNode.createForContextOrMarker();
    @Child private FrameSlotWriteNode instructionPointerWriteNode = FrameSlotWriteNode.createForInstructionPointer();
    @Child private FrameSlotWriteNode stackPointerWriteNode = FrameSlotWriteNode.createForStackPointer();
    @Child private FrameSlotReadNode stackPointerReadNode = FrameSlotReadNode.createForStackPointer();
    @Child private StackPushNode pushStackNode = StackPushNode.create();

    public static SqueakCodeRootNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return new SqueakCodeRootNode(language, code);
    }

    public abstract Object execute(VirtualFrame frame);

    protected EnterCodeNode(final CompiledCodeObject code) {
        this.code = code;
        executeContextNode = ExecuteContextNode.create(code);
    }

    protected EnterCodeNode(final EnterCodeNode codeNode) {
        this(codeNode.code);
    }

    protected static final class SqueakCodeRootNode extends RootNode {
        @Child private EnterCodeNode codeNode;
        @Child private GetOrCreateContextNode getOrCreateContextNode = GetOrCreateContextNode.create();

        protected SqueakCodeRootNode(final SqueakLanguage language, final CompiledCodeObject code) {
            super(language, code.getFrameDescriptor());
            codeNode = EnterCodeNodeGen.create(code);
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return codeNode.execute(frame);
        }

        @Override
        public String getName() {
            return codeNode.toString();
        }

        @Override
        public String toString() {
            return codeNode.toString();
        }
    }

    private void initializeSlots(final VirtualFrame frame) {
        instructionPointerWriteNode.executeWrite(frame, 0);
        stackPointerWriteNode.executeWrite(frame, -1);
    }

    @ExplodeLoop
    @Specialization(assumptions = {"code.getCanBeVirtualizedAssumption()"})
    protected final Object enterVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        initializeSlots(frame);
        contextWriteNode.executeWrite(frame, new FrameMarker());
        // Push arguments and copied values onto the newContext.
        final Object[] arguments = frame.getArguments();
        assert code.getNumArgsAndCopied() == (arguments.length - FrameAccess.ARGUMENTS_START);
        for (int i = 0; i < code.getNumArgsAndCopied(); i++) {
            pushStackNode.executeWrite(frame, arguments[FrameAccess.ARGUMENTS_START + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            pushStackNode.executeWrite(frame, code.image.nil);
        }
        assert ((int) stackPointerReadNode.executeRead(frame)) + 1 >= remainingTemps;
        return executeContextNode.executeContext(frame, null);
    }

    @ExplodeLoop
    @Fallback
    protected final Object enter(final VirtualFrame frame) {
        initializeSlots(frame);
        final ContextObject newContext = createContextNode.executeGet(frame);
        contextWriteNode.executeWrite(frame, newContext);
        // Push arguments and copied values onto the newContext.
        final Object[] arguments = frame.getArguments();
        assert code.getNumArgsAndCopied() == (arguments.length - FrameAccess.ARGUMENTS_START);
        for (int i = 0; i < code.getNumArgsAndCopied(); i++) {
            newContext.push(arguments[FrameAccess.ARGUMENTS_START + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            newContext.push(code.image.nil);
        }
        assert newContext.getStackPointer() >= remainingTemps;
        return executeContextNode.executeContext(frame, newContext);
    }

    @Override
    public final String toString() {
        return code.toString();
    }

    public final boolean hasTag(final Class<? extends Tag> tag) {
        return tag == StandardTags.RootTag.class;
    }

    public final boolean isInstrumentable() {
        return true;
    }

    public final WrapperNode createWrapper(final ProbeNode probe) {
        return new EnterCodeNodeWrapper(this, this, probe);
    }

    @Override
    @TruffleBoundary
    public final SourceSection getSourceSection() {
        if (sourceSection == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            sourceSection = code.getSource().createSection(1);
        }
        return sourceSection;
    }
}
