package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.context.frame.FrameStackWriteNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class EnterCodeNode extends RootNode {
    private final CompiledCodeObject code;

    @Child private ExecuteContextNode executeContextNode;
    @Child private FrameStackWriteNode pushNode;

    protected EnterCodeNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        executeContextNode = ExecuteContextNode.create(code);
        pushNode = FrameStackWriteNode.create(code);
    }

    protected EnterCodeNode(final EnterCodeNode codeNode) {
        this(codeNode.code.image.getLanguage(), codeNode.code);
    }

    public static EnterCodeNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return new EnterCodeNode(language, code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        initializeSlots(code, frame);
        initializeArgumentsAndTemps(frame);
        return executeContextNode.executeContext(frame, null);
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
            pushNode.executePush(frame, arguments[FrameAccess.getArgumentStartIndex() + i]);
        }
        // Initialize remaining temporary variables with nil in newContext.
        final int remainingTemps = code.getNumTemps() - code.getNumArgs();
        for (int i = 0; i < remainingTemps; i++) {
            pushNode.executePush(frame, NilObject.SINGLETON);
        }
        assert FrameAccess.getStackPointer(frame, code) >= remainingTemps;
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return code.toString();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
