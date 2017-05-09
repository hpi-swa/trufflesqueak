package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameSlotKind;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;

/**
 * This class implements the global interpreter loop. It creates frames to execute from context
 * objects in the Squeak image and runs them. When they return, it will go up the sender chain as it
 * is in the Image and continue running. This is also the node that handles the process switching
 * logic.
 */
public class SqueakContextNode extends RootNode {
    private static final Object[] EMPTY_ARRAY = new Object[0];

    private enum ContextParts {
        SENDER,
        PC,
        SP,
        METHOD,
        CLOSURE,
        RECEIVER,
        TEMP_FRAME_START,
    }

    private ListObject context;

    public SqueakContextNode(SqueakLanguage language, ListObject activeContext) {
        super(language);
        context = activeContext;
    }

    private static CompiledCodeObject getCurrentMethod(ListObject context) {
        return (CompiledCodeObject) context.at0(ContextParts.METHOD.ordinal());
    }

    private static VirtualFrame createFrame(CompiledCodeObject method, ListObject ctxt) {
        int pc = (int) ctxt.at0(ContextParts.PC.ordinal()).unsafeUnwrapInt();
        int sp = (int) ctxt.at0(ContextParts.SP.ordinal()).unsafeUnwrapInt();
        BaseSqueakObject closure = ctxt.at0(ContextParts.CLOSURE.ordinal());
        BaseSqueakObject receiver = ctxt.at0(ContextParts.RECEIVER.ordinal());
        VirtualFrame frame = Truffle.getRuntime().createVirtualFrame(EMPTY_ARRAY, method.getFrameDescriptor());
        frame.setInt(method.pcSlot, pc);
        frame.setInt(method.stackPointerSlot, sp);
        frame.setObject(method.selfSlot, ctxt);
        frame.setObject(method.closureSlot, closure);
        method.receiverSlot.setKind(FrameSlotKind.Object);
        frame.setObject(method.receiverSlot, receiver);
        int tempStart = ContextParts.TEMP_FRAME_START.ordinal();
        for (int i = tempStart; i < ctxt.size(); i++) {
            method.stackSlots[i - tempStart].setKind(FrameSlotKind.Object);
            frame.setObject(method.stackSlots[i - tempStart], ctxt.at0(i));
        }
        return frame;
    }

    private static ListObject getSender(ListObject context) {
        BaseSqueakObject sender = context.at0(ContextParts.SENDER.ordinal());
        if (sender instanceof ListObject) {
            return (ListObject) sender;
        } else {
            throw new RuntimeException("sender chain ended");
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ListObject currentContext = context;
        while (true) {
            CompiledCodeObject method = getCurrentMethod(currentContext);
            VirtualFrame currentFrame = createFrame(method, currentContext);
            int pc = (int) ((SmallInteger) currentContext.at0(ContextParts.PC.ordinal())).getValue();
            try {
                // This will continue execution in the active context until that
                // context returns or switches to another Squeak process.
                SqueakMethodNode squeakMethodNode = new SqueakMethodNode(this.getLanguage(SqueakLanguage.class), method);
                // squeakMethodNode.executeGeneric(currentFrame, pc);
            } catch (NonLocalReturn e) {
                // TODO: unwind context chain towards target
            } catch (NonVirtualReturn e) {
                // TODO: unwind context chain towards e.targetContext
            } catch (ProcessSwitch e) {
                // TODO: switch
            }
            throw new RuntimeException("unimplemented exit from method");
        }
    }
}
