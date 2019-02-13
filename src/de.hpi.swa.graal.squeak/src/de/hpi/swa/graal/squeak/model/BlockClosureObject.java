package de.hpi.swa.graal.squeak.model;

import java.util.Arrays;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.image.reading.SqueakImageChunk;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.util.ArrayUtils;

public final class BlockClosureObject extends AbstractSqueakObject {
    @CompilationFinal private Object receiver;
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledBlockObject block;
    @CompilationFinal private long pc = -1;
    @CompilationFinal private long numArgs = -1;
    @CompilationFinal(dimensions = 1) private Object[] copied;
    @CompilationFinal private RootCallTarget callTarget;

    private final CyclicAssumption callTargetStable = new CyclicAssumption("BlockClosureObject assumption");

    public BlockClosureObject(final SqueakImageContext image, final long hash) {
        super(image, hash, image.blockClosureClass);
        this.copied = ArrayUtils.EMPTY_ARRAY; // ensure copied is set
    }

    public BlockClosureObject(final SqueakImageContext image) {
        super(image, image.blockClosureClass);
        this.copied = ArrayUtils.EMPTY_ARRAY; // ensure copied is set
    }

    public BlockClosureObject(final CompiledBlockObject compiledBlock, final RootCallTarget callTarget, final Object receiver, final Object[] copied, final ContextObject outerContext) {
        super(compiledBlock.image, compiledBlock.image.blockClosureClass);
        this.block = compiledBlock;
        this.callTarget = callTarget;
        this.outerContext = outerContext;
        this.receiver = receiver;
        this.copied = copied;
        this.pc = block.getInitialPC();
        this.numArgs = block.getNumArgs();
    }

    private BlockClosureObject(final BlockClosureObject original) {
        super(original.image, original.image.blockClosureClass);
        this.block = original.block;
        this.callTarget = original.callTarget;
        this.outerContext = original.outerContext;
        this.receiver = original.receiver;
        this.copied = original.copied;
        this.pc = original.pc;
        this.numArgs = original.numArgs;
    }

    public void fillin(final SqueakImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers = chunk.getPointers();
        assert pointers.length >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        outerContext = (ContextObject) pointers[BLOCK_CLOSURE.OUTER_CONTEXT];
        pc = (long) pointers[BLOCK_CLOSURE.START_PC];
        numArgs = (long) pointers[BLOCK_CLOSURE.ARGUMENT_COUNT];
        copied = Arrays.copyOfRange(pointers, BLOCK_CLOSURE.FIRST_COPIED_VALUE, pointers.length);
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        switch (index) {
            case BLOCK_CLOSURE.OUTER_CONTEXT:
                return getOuterContext();
            case BLOCK_CLOSURE.START_PC:
                return getStartPC();
            case BLOCK_CLOSURE.ARGUMENT_COUNT:
                return getNumArgs();
            default:
                return getCopiedAt0(index);
        }
    }

    public void atput0(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        switch (index) {
            case BLOCK_CLOSURE.OUTER_CONTEXT:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                outerContext = (ContextObject) obj;
                break;
            case BLOCK_CLOSURE.START_PC:
                setStartPC((int) (long) obj);
                break;
            case BLOCK_CLOSURE.ARGUMENT_COUNT:
                setNumArgs((int) (long) obj);
                break;
            default:
                setCopiedAt0(index, obj);
                break;
        }
    }

    public ContextObject getOuterContext() {
        return outerContext;
    }

    public long getStartPC() {
        if (pc == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pc = block.getInitialPC();
        }
        return pc;
    }

    public long getNumArgs() {
        if (numArgs == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArgs = block.getNumArgs();
        }
        return numArgs;
    }

    public Object getCopiedAt0(final int index) {
        return copied[index - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
    }

    public Object[] getCopied() {
        return copied;
    }

    public void setOuterContext(final ContextObject outerContext) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.outerContext = outerContext;
    }

    public void setStartPC(final int pc) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.pc = pc;
    }

    public void setNumArgs(final int numArgs) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.numArgs = numArgs;
    }

    public void setCopiedAt0(final int index, final Object value) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        copied[index - BLOCK_CLOSURE.FIRST_COPIED_VALUE] = value;
    }

    public void setCopied(final Object[] copied) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.copied = copied;
    }

    public void become(final BlockClosureObject other) {
        becomeOtherClass(other);
        final Object[] otherCopied = other.copied;
        other.setCopied(this.copied);
        this.setCopied(otherCopied);
    }

    @Override
    public int instsize() {
        return BLOCK_CLOSURE.FIRST_COPIED_VALUE;
    }

    @Override
    public int size() {
        return copied.length + instsize();
    }

    public Object getReceiver() {
        if (receiver == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            receiver = outerContext.getReceiver();
        }
        return receiver;
    }

    public void setReceiver(final Object receiver) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        this.receiver = receiver;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    private void initializeCompiledBlock(final CompiledMethodObject method) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        assert pc >= 0;
        final int offset = (int) pc - method.getInitialPC();
        final int j = method.getBytes()[offset - 2];
        final int k = method.getBytes()[offset - 1];
        final int blockSize = (j << 8) | (k & 0xff);
        block = CompiledBlockObject.create(method, method, (int) numArgs, copied.length, offset, blockSize);
        callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(block.image.getLanguage(), block));
    }

    public CompiledBlockObject getCompiledBlock() {
        if (block == null) {
            initializeCompiledBlock(outerContext.getMethod());
        }
        return block;
    }

    /** Special version of getCompiledBlock for image loader. */
    public CompiledBlockObject getCompiledBlock(final CompiledMethodObject method) {
        initializeCompiledBlock(method);
        return block;
    }

    public boolean hasHomeContext() {
        return outerContext != null;
    }

    public ContextObject getHomeContext() {
        // Recursively unpack closures until home context is reached.
        final BlockClosureObject closure = outerContext.getClosure();
        if (closure != null) {
            return closure.getHomeContextWithBoundary();
        } else {
            return outerContext;
        }
    }

    @TruffleBoundary
    private ContextObject getHomeContextWithBoundary() {
        final BlockClosureObject closure = outerContext.getClosure();
        if (closure != null) {
            return closure.getHomeContextWithBoundary();
        } else {
            return outerContext;
        }
    }

    public AbstractSqueakObject shallowCopy() {
        return new BlockClosureObject(this);
    }

    public Object[] getTraceableObjects() {
        final Object[] result = new Object[copied.length + 2];
        for (int i = 0; i < copied.length; i++) {
            result[i] = copied[i];
        }
        result[copied.length] = receiver;
        result[copied.length + 1] = outerContext;
        return result;
    }
}
