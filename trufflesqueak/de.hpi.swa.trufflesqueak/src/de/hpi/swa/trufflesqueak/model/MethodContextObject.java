package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;
import com.oracle.truffle.api.nodes.Node.Child;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNode;
import de.hpi.swa.trufflesqueak.nodes.BlockActivationNodeGen;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class MethodContextObject extends BaseSqueakObject {
    @Child private BlockActivationNode dispatch = BlockActivationNodeGen.create();
    @CompilationFinal private ActualContextObject actualContext;
    private int sp = CONTEXT.TEMP_FRAME_START - 1;
    private boolean isDirty;

    public static MethodContextObject createReadOnlyContextObject(SqueakImageContext img, Frame virtualFrame) {
        MaterializedFrame frame = virtualFrame.materialize();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        FrameSlot thisContextSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.THIS_CONTEXT);
        MethodContextObject contextObject = (MethodContextObject) FrameUtil.getObjectSafe(frame, thisContextSlot);
        if (contextObject == null) {
            contextObject = new MethodContextObject(img, new ReadOnlyContextObject(img, frame));
            frame.setObject(thisContextSlot, contextObject);
        }
        return contextObject;
    }

    public static MethodContextObject createWriteableContextObject(SqueakImageContext img) {
        return new MethodContextObject(img, new WriteableContextObject(img));
    }

    public static MethodContextObject createWriteableContextObject(SqueakImageContext img, int size) {
        return new MethodContextObject(img, new WriteableContextObject(img, size));
    }

    private MethodContextObject(SqueakImageContext img, ActualContextObject context) {
        super(img);
        actualContext = context;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        assert actualContext instanceof WriteableContextObject;
        actualContext.fillin(chunk);
    }

    @Override
    public ClassObject getSqClass() {
        return image.methodContextClass;
    }

    @Override
    public Object at0(int index) {
        return actualContext.at0(index);
    }

    @Override
    public void atput0(int index, Object value) {
        atput0(index, value, true);
    }

    public void atput0(int index, Object value, boolean flagDirty) {
        if (flagDirty && index == CONTEXT.SENDER) {
            isDirty = true;
        }
        try {
            actualContext.atContextPut0(index, value);
        } catch (NonVirtualContextModification e) {
            beWriteable();
            actualContext.atput0(index, value);
        }
    }

    private void beWriteable() {
        if (actualContext instanceof ReadOnlyContextObject) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            actualContext = new WriteableContextObject(image, (ReadOnlyContextObject) actualContext);
        }
    }

    @Override
    public int size() {
        return actualContext.size();
    }

    @Override
    public int instsize() {
        // the receiver is part of the "variable part", because it is on the stack in Squeak
        return CONTEXT.TEMP_FRAME_START - 1;
    }

    public CompiledCodeObject getCodeObject() {
        Object method = at0(CONTEXT.METHOD);
        if (method instanceof BlockClosureObject) {
            return ((BlockClosureObject) method).getCompiledBlock();
        } else {
            return (CompiledCodeObject) method;
        }
    }

    public Object getFrameMarker() {
        return actualContext.getFrameMarker();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return actualContext.shallowCopy();
    }

    public Object[] getFrameArguments() {
        int numArgs = getCodeObject().getNumArgsAndCopiedValues();
        Object[] arguments = new Object[1 + numArgs];
        Object method = at0(CONTEXT.METHOD);
        if (method instanceof BlockClosureObject) {
            arguments[0] = ((BlockClosureObject) method).getReceiver();
        } else {
            arguments[0] = actualContext.at0(CONTEXT.RECEIVER);
        }
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = actualContext.at0(CONTEXT.TEMP_FRAME_START + i);
        }
        return arguments;
    }

    public boolean isUnwindMarked() {
        CompiledCodeObject code = getCodeObject();
        return code.hasPrimitive() && code.primitiveIndex() == 198;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public MethodContextObject getSender() {
        Object sender = actualContext.at0(CONTEXT.SENDER);
        if (sender instanceof MethodContextObject) {
            return (MethodContextObject) sender;
        } else if (sender instanceof NilObject) {
            return null;
        }
        throw new RuntimeException("Unexpected sender: " + sender);
    }

    public void push(Object value) {
        atput0(++sp, value);
    }
}
