package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class BlockClosureObject extends AbstractSqueakObject {
    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 1) private Object[] copied;
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledBlockObject block;
    @CompilationFinal private long pc = -1;
    @CompilationFinal private long numArgs = -1;
    @CompilationFinal private RootCallTarget callTarget;
    @CompilationFinal private final CyclicAssumption callTargetStable = new CyclicAssumption("BlockClosurObject assumption");

    public BlockClosureObject(final SqueakImageContext image) {
        super(image, image.blockClosureClass);
        this.copied = new Object[0]; // ensure copied is set
    }

    public BlockClosureObject(final CompiledBlockObject compiledBlock, final RootCallTarget callTarget, final Object receiver, final Object[] copied, final ContextObject outerContext) {
        super(compiledBlock.image, compiledBlock.image.blockClosureClass);
        assert outerContext.getFrameMarker() != null;
        this.block = compiledBlock;
        this.callTarget = callTarget;
        this.outerContext = outerContext;
        this.receiver = receiver;
        this.copied = copied;
    }

    private BlockClosureObject(final BlockClosureObject original) {
        super(original.image, original.image.blockClosureClass);
        this.block = original.getCompiledBlock();
        this.callTarget = original.callTarget;
        this.outerContext = original.outerContext;
        this.receiver = original.receiver;
        this.copied = original.copied;
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers = chunk.getPointers();
        assert pointers.length >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        outerContext = (ContextObject) pointers[BLOCK_CLOSURE.OUTER_CONTEXT];
        pc = (long) pointers[BLOCK_CLOSURE.INITIAL_PC];
        numArgs = (long) pointers[BLOCK_CLOSURE.ARGUMENT_COUNT];
        copied = new Object[pointers.length - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
        for (int i = 0; i < copied.length; i++) {
            copied[i] = pointers[BLOCK_CLOSURE.FIRST_COPIED_VALUE + i];
        }
    }

    public long getPC() {
        if (pc == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pc = block.getInitialPC();
        }
        return pc;
    }

    private long getNumArgs() {
        if (numArgs == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            numArgs = block.getNumArgs();
        }
        return numArgs;
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        switch (index) {
            case BLOCK_CLOSURE.OUTER_CONTEXT:
                return outerContext;
            case BLOCK_CLOSURE.INITIAL_PC:
                return getPC();
            case BLOCK_CLOSURE.ARGUMENT_COUNT:
                return getNumArgs();
            default:
                return copied[index - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
        }
    }

    public void atput0(final long longIndex, final Object obj) {
        final int index = (int) longIndex;
        switch (index) {
            case BLOCK_CLOSURE.OUTER_CONTEXT:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                outerContext = (ContextObject) obj;
                break;
            case BLOCK_CLOSURE.INITIAL_PC:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                pc = ((Long) obj).intValue();
                break;
            case BLOCK_CLOSURE.ARGUMENT_COUNT:
                CompilerDirectives.transferToInterpreterAndInvalidate();
                numArgs = ((Long) obj).intValue();
                break;
            default:
                copied[index - BLOCK_CLOSURE.FIRST_COPIED_VALUE] = obj;
                break;
        }
    }

    @Override
    public boolean become(final AbstractSqueakObject other) {
        if (!(other instanceof BlockClosureObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] otherCopied = copied;
        copied = ((BlockClosureObject) other).copied;
        ((BlockClosureObject) other).copied = otherCopied;
        return true;
    }

    public int size() {
        return copied.length + instsize();
    }

    public static int instsize() {
        return BLOCK_CLOSURE.FIRST_COPIED_VALUE;
    }

    public Object[] getStack() {
        return copied;
    }

    public Object getReceiver() {
        if (receiver == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            receiver = outerContext.getReceiver();
        }
        return receiver;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    public CompiledBlockObject getCompiledBlock() {
        if (block == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final CompiledCodeObject code = outerContext.getMethod();
            final CompiledMethodObject method;
            if (code instanceof CompiledMethodObject) {
                method = (CompiledMethodObject) code;
            } else {
                method = ((CompiledBlockObject) code).getMethod();
            }
            final int offset = (int) pc - method.getInitialPC();
            final int j = code.getBytes()[offset - 2];
            final int k = code.getBytes()[offset - 1];
            final int blockSize = (j << 8) | (k & 0xff);
            block = CompiledBlockObject.create(code, method, ((Long) numArgs).intValue(), copied.length, offset, blockSize);
            callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(block.image.getLanguage(), block));
        }
        return block;
    }

    public Object[] getFrameArguments(final Object senderOrMarker, final Object... objects) {
        CompilerAsserts.compilationConstant(objects.length);
        final CompiledBlockObject blockObject = getCompiledBlock();
        if (blockObject.getNumArgs() != objects.length) {
            throw new PrimitiveFailed();
        }
        final Object[] arguments = new Object[FrameAccess.ARGUMENTS_START +
                        objects.length +
                        copied.length];
        arguments[FrameAccess.METHOD] = blockObject;
        // Sender is thisContext (or marker)
        arguments[FrameAccess.SENDER_OR_SENDER_MARKER] = senderOrMarker;
        arguments[FrameAccess.CLOSURE_OR_NULL] = this;
        arguments[FrameAccess.RECEIVER] = getReceiver();
        for (int i = 0; i < objects.length; i++) {
            arguments[FrameAccess.ARGUMENTS_START + i] = objects[i];
        }
        for (int i = 0; i < copied.length; i++) {
            arguments[FrameAccess.ARGUMENTS_START + objects.length + i] = copied[i];
        }
        return arguments;
    }

    public ContextObject getHomeContext() {
        final BlockClosureObject closure = outerContext.getClosure();
        // recursively unpack closures until home context is reached
        if (closure != null) {
            CompilerDirectives.transferToInterpreter();
            return closure.getHomeContext();
        }
        return outerContext;
    }

    public AbstractSqueakObject shallowCopy() {
        return new BlockClosureObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] newPointers = new Object[BLOCK_CLOSURE.FIRST_COPIED_VALUE + copied.length];
        newPointers[BLOCK_CLOSURE.OUTER_CONTEXT] = outerContext;
        newPointers[BLOCK_CLOSURE.INITIAL_PC] = getPC();
        newPointers[BLOCK_CLOSURE.ARGUMENT_COUNT] = getNumArgs();
        for (int i = 0; i < copied.length; i++) {
            newPointers[BLOCK_CLOSURE.FIRST_COPIED_VALUE + i] = copied[i];
        }
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < newPointers.length; j++) {
                final Object newPointer = newPointers[j];
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    newPointers[j] = toPointer;
                    if (copyHash && fromPointer instanceof AbstractSqueakObject && toPointer instanceof AbstractSqueakObject) {
                        ((AbstractSqueakObject) toPointer).setSqueakHash(((AbstractSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
        outerContext = (ContextObject) newPointers[BLOCK_CLOSURE.OUTER_CONTEXT];
        pc = (long) newPointers[BLOCK_CLOSURE.INITIAL_PC];
        numArgs = ((Long) newPointers[BLOCK_CLOSURE.ARGUMENT_COUNT]).intValue();
        for (int i = 0; i < copied.length; i++) {
            copied[i] = newPointers[BLOCK_CLOSURE.FIRST_COPIED_VALUE + i];
        }
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
