package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public abstract class AboutToReturnNode extends AbstractContextNode {
    private static BaseSqueakObject aboutToReturnSelector;
    @Child private SendSelectorNode sendAboutToReturnNode;
    @Child private PushStackNode pushNode;

    public static AboutToReturnNode create(CompiledCodeObject code) {
        return AboutToReturnNodeGen.create(code);
    }

    protected AboutToReturnNode(CompiledCodeObject code) {
        super(code);
        if (aboutToReturnSelector == null) {
            aboutToReturnSelector = (BaseSqueakObject) code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorAboutToReturn);
        }
        sendAboutToReturnNode = new SendSelectorNode(code, -1, -1, aboutToReturnSelector, 2);
        pushNode = PushStackNode.create(code);
    }

    public abstract void executeAboutToReturn(VirtualFrame frame, NonLocalReturn nlr);

// @Specialization(guards = {"context == null"})
// protected void doAboutToReturnVirtualized(VirtualFrame frame, NonLocalReturn nlr,
// @SuppressWarnings("unused") @Cached("getContext(frame)") MethodContextObject context) {
// //TODO implement virtualized alternative
// }

    @Specialization // (guards = {"context != null"})
    protected void doAboutToReturn(VirtualFrame frame, NonLocalReturn nlr,
                    @Cached("getContext(frame)") MethodContextObject context) {
        pushNode.executeWrite(frame, nlr.getTargetContext());
        pushNode.executeWrite(frame, nlr.getReturnValue());
        pushNode.executeWrite(frame, context);
        sendAboutToReturnNode.executeSend(frame);
    }
}
