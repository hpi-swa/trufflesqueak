package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakBlockNode;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class BlockClosure extends BaseSqueakObject {
    private static final int BLKCLSR_OUTER_CONTEXT = 0;
    private static final int BLKCLSR_STARTPC = 1;
    private static final int BLKCLSR_NUMARGS = 2;
    private static final int BLKCLSR_SIZE = 3;
    @CompilationFinal private Object receiver;
    @CompilationFinal(dimensions = 1) private Object[] stack;
    private Object frame;
    private int argCount;
    @CompilationFinal private RootCallTarget callTarget;
    private SqueakNode[] ast;

    public BlockClosure(SqueakImageContext img) {
        super(img);
    }

    public BlockClosure(SqueakImageContext image,
                    FrameDescriptor fd,
                    SqueakNode[] statements,
                    Object frameMarker,
                    int numArgs,
                    Object rcvr,
                    Object[] copiedValues) {
        this(image);
        ast = statements;
        frame = frameMarker;
        argCount = numArgs;
        receiver = rcvr;
        stack = copiedValues;
        callTarget = Truffle.getRuntime().createCallTarget(new SqueakBlockNode(image.getLanguage(), this, fd));
    }

    @Override
    public void fillin(Chunk chunk) {
        // FIXME
    }

    @Override
    public BaseSqueakObject at0(int i) {
        switch (i) {
            case BLKCLSR_OUTER_CONTEXT:
                return image.nil;
            // return frame; // FIXME
            case BLKCLSR_STARTPC:
                return image.wrapInt(0);
            case BLKCLSR_NUMARGS:
                return image.wrapInt(getNumArgs());
            default:// FIXME
                return (BaseSqueakObject) stack[i - BLKCLSR_SIZE];
        }
    }

    public int getNumArgs() {
        return argCount;
    }

    @Override
    public void atput0(int i, BaseSqueakObject obj) {
        switch (i) {
            default:// FIXME
                stack[i - BLKCLSR_SIZE] = obj;
        }
    }

    @Override
    public boolean become(BaseSqueakObject other) {
        if (other instanceof BlockClosure && super.become(other)) {
            Object[] stack2 = stack;
            stack = ((BlockClosure) other).stack;
            ((BlockClosure) other).stack = stack2;
            return true;
        }
        return false;
    }

    @Override
    public BaseSqueakObject getSqClass() {
        return image.blockClosureClass;
    }

    @Override
    public int size() {
        return instsize() + varsize();
    }

    @Override
    public int instsize() {
        return BLKCLSR_SIZE;
    }

    @Override
    public int varsize() {
        return stack.length;
    }

    public RootCallTarget getCallTarget() {
        return callTarget;
    }

    public SqueakNode[] getAST() {
        return ast;
    }

    public Object[] getStack() {
        return stack;
    }

    public Object getReceiver() {
        return receiver;
    }
}
