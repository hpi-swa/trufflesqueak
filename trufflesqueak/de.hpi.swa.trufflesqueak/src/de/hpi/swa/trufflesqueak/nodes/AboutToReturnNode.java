package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.Returns.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.TemporaryWriteNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PushStackNode;

public abstract class AboutToReturnNode extends AbstractNodeWithCode {
    @Child protected BlockActivationNode dispatch = BlockActivationNodeGen.create();
    private static BaseSqueakObject aboutToReturnSelector;
    @Child private SendSelectorNode sendAboutToReturnNode;
    @Child private PushStackNode pushNode;
    @Child private SqueakNode blockArgumentNode;
    @Child private SqueakNode completeTempReadNode;
    @Child private TemporaryWriteNode completeTempWriteNode;

    public static AboutToReturnNode create(CompiledMethodObject code) {
        return AboutToReturnNodeGen.create(code);
    }

    protected AboutToReturnNode(CompiledMethodObject method) {
        super(method);
        if (aboutToReturnSelector == null) {
            aboutToReturnSelector = (BaseSqueakObject) method.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorAboutToReturn);
        }
        sendAboutToReturnNode = new SendSelectorNode(method, -1, -1, aboutToReturnSelector, 2);
        pushNode = PushStackNode.create(method);
        blockArgumentNode = TemporaryReadNode.create(method, 0);
        completeTempReadNode = TemporaryReadNode.create(method, 1);
        completeTempWriteNode = TemporaryWriteNode.create(method, 1);
    }

    public abstract void executeAboutToReturn(VirtualFrame frame, NonLocalReturn nlr);

    /*
     * Virtualized version of Context>>aboutToReturn:through:, more specifically
     * Context>>resume:through:. This is only called if code.isUnwindMarked(), so there is no need to
     * unwind contexts here as this is already happening when NonLocalReturns are handled. Note that
     * this however does not check if the current context isDead nor does it terminate contexts (this
     * may be a problem).
     */
    @Specialization(guards = {"isVirtualized(frame)"})
    protected void doAboutToReturnVirtualized(VirtualFrame frame, @SuppressWarnings("unused") NonLocalReturn nlr) {
        if (completeTempReadNode.executeGeneric(frame) == code.image.nil) {
            completeTempWriteNode.executeWrite(frame, code.image.sqTrue);
            BlockClosureObject block = (BlockClosureObject) blockArgumentNode.executeGeneric(frame);
            try {
                dispatch.executeBlock(block, block.getFrameArguments(frame));
            } catch (LocalReturn blockLR) { // ignore
            } catch (NonLocalReturn blockNLR) {
                if (!blockNLR.hasArrivedAtTargetContext()) {
                    throw blockNLR;
                }
            }
        }
    }

    @Specialization(guards = {"!isVirtualized(frame)"})
    protected void doAboutToReturn(VirtualFrame frame, NonLocalReturn nlr,
                    @Cached("getContext(frame)") ContextObject context) {
        pushNode.executeWrite(frame, nlr.getTargetContext());
        pushNode.executeWrite(frame, nlr.getReturnValue());
        pushNode.executeWrite(frame, context);
        sendAboutToReturnNode.executeSend(frame);
    }
}
