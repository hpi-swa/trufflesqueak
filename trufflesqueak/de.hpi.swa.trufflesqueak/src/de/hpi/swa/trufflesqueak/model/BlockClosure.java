package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class BlockClosure extends BaseSqueakObject {
    private static class BLKCLSR {
        public static final byte OUTER_CONTEXT = 0;
        public static final byte COMPILEDBLOCK = 1;
        public static final byte NUMARGS = 2;
        public static final byte RECEIVER = 3;
        public static final byte SIZE = 4;
    }

    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 1) private Object[] copied;
    @CompilationFinal private Object frameMarker;
    @CompilationFinal private Object context;
    @CompilationFinal private CompiledBlockObject block;

    public BlockClosure(Object frameId, CompiledBlockObject compiledBlock, Object receiver, Object[] copied) {
        super(compiledBlock.image);
        block = compiledBlock;
        frameMarker = frameId;
        this.receiver = receiver;
        this.copied = copied;
    }

    private BlockClosure(BlockClosure original) {
        this(original.frameMarker, original.block, original.receiver, original.copied);
        context = original.context;
    }

    @Override
    public void fillin(Chunk chunk) {
        // FIXME
    }

    private Object getOrPrepareContext() {
        if (context == null) {
            return Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
                @Override
                public Object visitFrame(FrameInstance frameInstance) {
                    Frame frame = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                    FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
                    FrameSlot markerSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.MARKER);
                    Object marker = FrameUtil.getObjectSafe(frame, markerSlot);
                    if (marker == frame) {
                        context = ContextObject.createReadOnlyContextObject(image, frame);
                        return context;
                    }
                    return null;
                }
            });
        }
        return context;
    }

    @Override
    public Object at0(int i) {
        switch (i) {
            case BLKCLSR.OUTER_CONTEXT:
                return getOrPrepareContext();
            case BLKCLSR.COMPILEDBLOCK:
                return block;
            case BLKCLSR.NUMARGS:
                return block.getNumArgs();
            case BLKCLSR.RECEIVER:
                return receiver;
            default:// FIXME
                return copied[i - BLKCLSR.SIZE];
        }
    }

    @Override
    public void atput0(int i, Object obj) {
        switch (i) {
            case BLKCLSR.OUTER_CONTEXT:
                context = obj;
                break;
            case BLKCLSR.COMPILEDBLOCK:
                block = (CompiledBlockObject) obj;
                break;
            case BLKCLSR.NUMARGS:
                throw new PrimitiveFailed();
            case BLKCLSR.RECEIVER:
                receiver = obj;
                break;
            default:
                copied[i - BLKCLSR.SIZE] = obj;
                break;
        }
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof BlockClosure && super.become(other)) {
            Object[] stack2 = copied;
            copied = ((BlockClosure) other).copied;
            ((BlockClosure) other).copied = stack2;
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
        return BLKCLSR.SIZE;
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
        return block.getCallTarget();
    }

    public Assumption getCallTargetStable() {
        return block.getCallTargetStable();
    }

    public CompiledBlockObject getCompiledBlock() {
        return block;
    }

    public Object[] getFrameArguments(Object... objects) {
        if (block.getNumArgs() != objects.length) {
            throw new PrimitiveFailed();
        }
        Object[] arguments = new Object[1 /* receiver */ +
                        objects.length +
                        copied.length +
                        1 /* this */];
        arguments[0] = getReceiver();
        for (int i = 0; i < objects.length; i++) {
            arguments[1 + i] = objects[i];
        }
        for (int i = 0; i < copied.length; i++) {
            arguments[1 + objects.length + i] = copied[i];
        }
        arguments[arguments.length - 1] = this;
        return arguments;
    }

    public Object getFrameMarker() {
        return frameMarker;
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new BlockClosure(this);
    }
}
