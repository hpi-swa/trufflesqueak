package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
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
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

@GenerateWrapper
public abstract class EnterCodeNode extends AbstractNodeWithCode implements InstrumentableNode {
    private SourceSection sourceSection;

    @Child private ExecuteContextNode executeContextNode;
    @Child private StackPushNode pushStackNode;

    protected EnterCodeNode(final EnterCodeNode codeNode) {
        this(codeNode.code);
    }

    protected EnterCodeNode(final CompiledCodeObject code) {
        super(code);
        executeContextNode = ExecuteContextNode.create(code);
        pushStackNode = StackPushNode.create(code);
    }

    public static SqueakCodeRootNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return new SqueakCodeRootNode(language, code);
    }

    public abstract Object execute(VirtualFrame frame);

    @NodeInfo(cost = NodeCost.NONE)
    protected static final class SqueakCodeRootNode extends RootNode {
        @Child private EnterCodeNode codeNode;

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
        public SourceSection getSourceSection() {
            return codeNode.getSourceSection();
        }

        @Override
        public String toString() {
            CompilerAsserts.neverPartOfCompilation();
            return codeNode.toString();
        }

        @Override
        public boolean isCloningAllowed() {
            return true;
        }
    }

    @Specialization(assumptions = {"code.getCanBeVirtualizedAssumption()"})
    protected final Object enterVirtualized(final VirtualFrame frame) {
        CompilerDirectives.ensureVirtualized(frame);
        initializeSlots(code, frame);
        initializeArgumentsAndTemps(frame);
        return executeContextNode.executeContext(frame, null);
    }

    @Fallback
    protected final Object enter(final VirtualFrame frame) {
        initializeSlots(code, frame);
        final ContextObject newContext = ContextObject.create(frame);
        assert newContext == FrameAccess.getContext(frame, code);
        initializeArgumentsAndTemps(frame);
        return executeContextNode.executeContext(frame, newContext);
    }

    private static void initializeSlots(final CompiledCodeObject code, final VirtualFrame frame) {
        FrameAccess.initializeMarker(frame, code);
        FrameAccess.setInstructionPointer(frame, code, 0);
        FrameAccess.setStackPointer(frame, code, 0);
    }

    @ExplodeLoop
    private void initializeArgumentsAndTemps(final VirtualFrame frame) {
        // Push arguments and copied values onto the newContext.
        final Object[] arguments = frame.getArguments();
        assert arguments.length == FrameAccess.expectedArgumentSize(code.getNumArgsAndCopied());
        for (int i = 0; i < code.getNumArgsAndCopied(); i++) {
            pushStackNode.executeWrite(frame, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            pushStackNode.executeWrite(frame, NilObject.SINGLETON);
        }
        assert FrameAccess.getStackPointer(frame, code) >= remainingTemps;
    }

    @Override
    public final String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    @Override
    public final boolean hasTag(final Class<? extends Tag> tag) {
        return tag == StandardTags.RootTag.class;
    }

    @Override
    public final boolean isInstrumentable() {
        return true;
    }

    @Override
    public final WrapperNode createWrapper(final ProbeNode probe) {
        return new EnterCodeNodeWrapper(this, this, probe);
    }

    @Override
    @TruffleBoundary
    public final SourceSection getSourceSection() {
        CompilerAsserts.neverPartOfCompilation();
        if (sourceSection == null) {
            sourceSection = code.getSource().createSection(1);
        }
        return sourceSection;
    }
}
