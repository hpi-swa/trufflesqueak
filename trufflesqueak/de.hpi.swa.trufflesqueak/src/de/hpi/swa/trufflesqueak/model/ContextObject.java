package de.hpi.swa.trufflesqueak.model;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameUtil;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualContextModification;
import de.hpi.swa.trufflesqueak.util.Constants.BLOCK_CONTEXT;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class ContextObject extends BaseSqueakObject {
    @CompilationFinal private ActualContextObject actualContext;

    public static ContextObject createReadOnlyContextObject(SqueakImageContext img, Frame virtualFrame) {
        MaterializedFrame frame = virtualFrame.materialize();
        FrameDescriptor frameDescriptor = frame.getFrameDescriptor();
        FrameSlot thisContextSlot = frameDescriptor.findFrameSlot(CompiledCodeObject.SLOT_IDENTIFIER.THIS_CONTEXT);
        ContextObject contextObject = (ContextObject) FrameUtil.getObjectSafe(frame, thisContextSlot);
        if (contextObject == null) {
            contextObject = new ContextObject(img, new ReadOnlyContextObject(img, frame));
            frame.setObject(thisContextSlot, contextObject);
        }
        return contextObject;
    }

    public static ContextObject createWriteableContextObject(SqueakImageContext img) {
        return new ContextObject(img, new WriteableContextObject(img));
    }

    public static ContextObject createWriteableContextObject(SqueakImageContext img, int size) {
        return new ContextObject(img, new WriteableContextObject(img, size));
    }

    private ContextObject(SqueakImageContext img, ActualContextObject context) {
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
    public Object at0(int l) {
        return actualContext.at0(l);
    }

    @Override
    public void atput0(int idx, Object object) {
        try {
            actualContext.atContextPut0(idx, object);
        } catch (NonVirtualContextModification e) {
            beWriteable();
            actualContext.atput0(idx, object);
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
        if (method instanceof Integer) { // if the method field is an integer, activeContex is a block context
            ContextObject homeContext = (ContextObject) at0(BLOCK_CONTEXT.HOME);
            return (CompiledCodeObject) homeContext.at0(CONTEXT.METHOD);
        } else if (method instanceof BlockClosure) {
            return ((BlockClosure) method).getCompiledBlock();
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
        if (method instanceof BlockClosure) {
            arguments[0] = ((BlockClosure) method).getReceiver();
        } else {
            arguments[0] = actualContext.at0(CONTEXT.RECEIVER);
        }
        for (int i = 0; i < numArgs; i++) {
            arguments[1 + i] = actualContext.at0(CONTEXT.TEMP_FRAME_START + i);
        }
        return arguments;
    }
}
