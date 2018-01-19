package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.EnterCodeNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class BlockClosureObject extends BaseSqueakObject {
    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 1) private Object[] copied;
    @CompilationFinal private FrameMarker frameMarker;
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledBlockObject block;
    @CompilationFinal private int pc = -1;
    @CompilationFinal private int numArgs = -1;
    private RootCallTarget callTarget;
    private final CyclicAssumption callTargetStable = new CyclicAssumption("Compiled method assumption");

    public BlockClosureObject(SqueakImageContext image) {
        super(image);
    }

    public BlockClosureObject(FrameMarker frameMarker, CompiledBlockObject compiledBlock, Object receiver, Object[] copied) {
        super(compiledBlock.image);
        this.block = compiledBlock;
        this.frameMarker = frameMarker;
        this.receiver = receiver;
        this.copied = copied;
    }

    private BlockClosureObject(BlockClosureObject original) {
        this(original.frameMarker, original.block, original.receiver, original.copied);
        outerContext = original.outerContext;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        Object[] pointers = chunk.getPointers();
        assert pointers.length >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        copied = new Object[pointers.length - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
        for (int i = 0; i < pointers.length; i++) {
            atput0(i, pointers[i]);
        }
    }

    @TruffleBoundary
    private ContextObject getOrPrepareContext() {
        if (outerContext == null) {
            outerContext = FrameAccess.findContextForMarker(frameMarker, image);
            if (outerContext == null) {
                throw new RuntimeException("Unable to find context");
            }
        }
        return outerContext;
    }

    public int getPC() {
        if (pc == -1) {
            pc = block.getMethod().getInitialPC() + block.getBytecodeOffset();
        }
        return pc;
    }

    private int getNumArgs() {
        if (numArgs == -1) {
            numArgs = block.getNumArgs();
        }
        return numArgs;
    }

    @Override
    public Object at0(int i) {
        switch (i) {
            case BLOCK_CLOSURE.OUTER_CONTEXT:
                return getOrPrepareContext();
            case BLOCK_CLOSURE.INITIAL_PC:
                return getPC();
            case BLOCK_CLOSURE.ARGUMENT_COUNT:
                return getNumArgs();
            default:
                return copied[i - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
        }
    }

    @Override
    public void atput0(int i, Object obj) {
        switch (i) {
            case BLOCK_CLOSURE.OUTER_CONTEXT:
                outerContext = (ContextObject) obj;
                break;
            case BLOCK_CLOSURE.INITIAL_PC:
                pc = (int) obj;
                break;
            case BLOCK_CLOSURE.ARGUMENT_COUNT:
                numArgs = (int) obj;
                break;
            default:
                copied[i - BLOCK_CLOSURE.FIRST_COPIED_VALUE] = obj;
        }
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof BlockClosureObject && super.become(other)) {
            Object[] stack2 = copied;
            copied = ((BlockClosureObject) other).copied;
            ((BlockClosureObject) other).copied = stack2;
            return true;
        }
        return false;
    }

    @Override
    public ClassObject getSqClass() {
        return image.blockClosureClass;
    }

    @Override
    public int size() {
        return instsize() + varsize();
    }

    @Override
    public int instsize() {
        return BLOCK_CLOSURE.FIRST_COPIED_VALUE;
    }

    @Override
    public int varsize() {
        return copied.length;
    }

    public Object[] getStack() {
        return copied;
    }

    public Object getReceiver() {
        return receiver;
    }

    public RootCallTarget getCallTarget() {
        if (callTarget == null) {
            CompilerDirectives.transferToInterpreter();
            callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(block.image.getLanguage(), block));
        }
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    public CompiledBlockObject getCompiledBlock() {
        if (block == null) {
            CompiledMethodObject code = (CompiledMethodObject) getOrPrepareContext().at0(CONTEXT.METHOD);
            int codeStart = pc - code.getInitialPC();
            int j = code.getBytes()[codeStart - 2];
            int k = code.getBytes()[codeStart - 1];
            int blockSize = (j << 8) | k;
            int codeEnd = codeStart + blockSize;
            block = new CompiledBlockObject(code, numArgs, copied.length, codeStart, codeEnd);
        }
        return block;
    }

    public Object[] getFrameArguments(VirtualFrame frame, Object... objects) {
        CompilerAsserts.compilationConstant(objects.length);
        if (block.getNumArgs() != objects.length) {
            throw new PrimitiveFailed();
        }
        Object[] arguments = new Object[FrameAccess.RCVR_AND_ARGS_START + /* METHOD + CLOSURE_OR_NULL */
                        1 /* receiver */ +
                        objects.length +
                        copied.length];
        arguments[FrameAccess.METHOD] = block;
        arguments[FrameAccess.SENDER_OR_NULL] = FrameAccess.getSender(frame);
        arguments[FrameAccess.CLOSURE_OR_NULL] = this;
        arguments[FrameAccess.RCVR_AND_ARGS_START] = getReceiver();
        for (int i = 0; i < objects.length; i++) {
            arguments[FrameAccess.RCVR_AND_ARGS_START + 1 + i] = objects[i];
        }
        for (int i = 0; i < copied.length; i++) {
            arguments[FrameAccess.RCVR_AND_ARGS_START + 1 + objects.length + i] = copied[i];
        }
        return arguments;
    }

    public FrameMarker getFrameMarker() {
        return frameMarker;
    }

    public ContextObject getOuterContextOrNull() {
        return outerContext;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new BlockClosureObject(this);
    }

    public Object[] getTraceableObjects() {
        Object[] result = new Object[copied.length + 2];
        for (int i = 0; i < copied.length; i++) {
            result[i] = copied[i];
        }
        result[copied.length] = receiver;
        result[copied.length + 1] = outerContext;
        return result;
    }
}
