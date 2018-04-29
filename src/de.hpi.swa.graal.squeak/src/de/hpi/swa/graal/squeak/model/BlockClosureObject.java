package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.AbstractImageChunk;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.exceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BLOCK_CLOSURE;
import de.hpi.swa.graal.squeak.nodes.EnterCodeNode;
import de.hpi.swa.graal.squeak.util.FrameAccess;

public final class BlockClosureObject extends BaseSqueakObject {
    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 1) private Object[] copied;
    @CompilationFinal private ContextObject outerContext;
    @CompilationFinal private CompiledBlockObject block;
    @CompilationFinal private long pc = -1;
    @CompilationFinal private long numArgs = -1;
    @CompilationFinal private RootCallTarget callTarget;
    @CompilationFinal private final CyclicAssumption callTargetStable = new CyclicAssumption("Compiled method assumption");
    @CompilationFinal private FrameSlot contextOrMarkerSlot;

    public BlockClosureObject(final SqueakImageContext image) {
        super(image);
        this.copied = new Object[0]; // ensure copied is set
    }

    public BlockClosureObject(final CompiledBlockObject compiledBlock, final Object receiver, final Object[] copied, final ContextObject outerContext, final FrameSlot contextOrMarkerSlot) {
        super(compiledBlock.image);
        assert outerContext.getFrameMarker() != null;
        this.block = compiledBlock;
        this.outerContext = outerContext;
        this.receiver = receiver;
        this.copied = copied;
        this.contextOrMarkerSlot = contextOrMarkerSlot;
    }

    private BlockClosureObject(final BlockClosureObject original) {
        super(original.image);
        this.block = (CompiledBlockObject) original.getCompiledBlock().shallowCopy();
        this.outerContext = original.outerContext;
        if (original.receiver instanceof BaseSqueakObject) {
            this.receiver = ((BaseSqueakObject) original.receiver).shallowCopy();
        } else {
            this.receiver = original.receiver;
        }
        this.copied = original.copied.clone();
        this.contextOrMarkerSlot = original.contextOrMarkerSlot;
    }

    @Override
    public void fillin(final AbstractImageChunk chunk) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] pointers = chunk.getPointers();
        assert pointers.length >= BLOCK_CLOSURE.FIRST_COPIED_VALUE;
        outerContext = (ContextObject) pointers[0];
        pc = (long) pointers[1];
        numArgs = (long) pointers[2];
        copied = new Object[pointers.length - BLOCK_CLOSURE.FIRST_COPIED_VALUE];
        for (int i = 0; i < copied.length; i++) {
            copied[i] = pointers[BLOCK_CLOSURE.FIRST_COPIED_VALUE + i];
        }
    }

    public long getPC() {
        if (pc == -1) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            pc = block.getInitialPC() + block.getOffset();
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

    private FrameSlot getContextOrMarkerSlot(final Frame frame) {
        if (contextOrMarkerSlot == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            contextOrMarkerSlot = FrameAccess.getContextOrMarkerSlot(frame);
        }
        return contextOrMarkerSlot;
    }

    @Override
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

    @Override
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
    public boolean become(final BaseSqueakObject other) {
        if (!(other instanceof BlockClosureObject)) {
            throw new PrimitiveExceptions.PrimitiveFailed();
        }
        if (!super.become(other)) {
            throw new SqueakException("Should not fail");
        }
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] stack2 = copied;
        copied = ((BlockClosureObject) other).copied;
        ((BlockClosureObject) other).copied = stack2;
        return true;
    }

    @Override
    public ClassObject getSqClass() {
        return image.blockClosureClass;
    }

    @Override
    public int size() {
        return copied.length + instsize();
    }

    @Override
    public int instsize() {
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
        if (callTarget == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            callTarget = Truffle.getRuntime().createCallTarget(EnterCodeNode.create(block.image.getLanguage(), block));
        }
        return callTarget;
    }

    public Assumption getCallTargetStable() {
        return callTargetStable.getAssumption();
    }

    public CompiledBlockObject getCompiledBlock() {
        if (block == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            final CompiledCodeObject code = outerContext.getMethod();
            final int offset = (int) pc - code.getInitialPC();
            final int j = code.getBytes()[offset - 2];
            final int k = code.getBytes()[offset - 1];
            final int blockSize = (j << 8) | (k & 0xff);
            block = CompiledBlockObject.create(code, ((Long) numArgs).intValue(), copied.length, offset, blockSize);
        }
        return block;
    }

    public Object[] getFrameArguments(final VirtualFrame frame, final Object... objects) {
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
        arguments[FrameAccess.SENDER_OR_SENDER_MARKER] = FrameAccess.getContextOrMarker(frame, getContextOrMarkerSlot(frame));
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

    @Override
    public BaseSqueakObject shallowCopy() {
        return new BlockClosureObject(this);
    }

    @Override
    public void pointersBecomeOneWay(final Object[] from, final Object[] to, final boolean copyHash) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        final Object[] newPointers = new Object[3 + copied.length];
        newPointers[0] = outerContext;
        newPointers[1] = getPC();
        newPointers[2] = getNumArgs();
        for (int i = 0; i < copied.length; i++) {
            newPointers[3 + i] = copied[i];
        }
        for (int i = 0; i < from.length; i++) {
            final Object fromPointer = from[i];
            for (int j = 0; j < newPointers.length; j++) {
                final Object newPointer = newPointers[j];
                if (newPointer == fromPointer) {
                    final Object toPointer = to[i];
                    newPointers[j] = toPointer;
                    if (copyHash && fromPointer instanceof BaseSqueakObject && toPointer instanceof SqueakObject) {
                        ((SqueakObject) toPointer).setSqueakHash(((BaseSqueakObject) fromPointer).squeakHash());
                    }
                }
            }
        }
        outerContext = (ContextObject) newPointers[0];
        pc = (long) newPointers[1];
        numArgs = ((Long) newPointers[2]).intValue();
        for (int i = 0; i < copied.length; i++) {
            copied[i] = newPointers[3 + i];
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
