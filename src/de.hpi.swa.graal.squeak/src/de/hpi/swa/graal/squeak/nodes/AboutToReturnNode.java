package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.Returns.LocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.AbstractSendNode;
import de.hpi.swa.graal.squeak.nodes.bytecodes.SendBytecodes.SendSelectorNode;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryReadNode;
import de.hpi.swa.graal.squeak.nodes.context.TemporaryWriteNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public abstract class AboutToReturnNode extends AbstractNodeWithCode {
    @Child protected BlockActivationNode dispatch = BlockActivationNodeGen.create();
    @Child private AbstractSendNode sendAboutToReturnNode;
    @Child private StackPushNode pushNode = StackPushNode.create();
    @Child private SqueakNode blockArgumentNode;
    @Child private SqueakNode completeTempReadNode;
    @Child private TemporaryWriteNode completeTempWriteNode;

    public static AboutToReturnNode create(final CompiledCodeObject code) {
        return AboutToReturnNodeGen.create(code);
    }

    public abstract void executeAboutToReturn(VirtualFrame frame, NonLocalReturn nlr);

    protected AboutToReturnNode(final CompiledCodeObject code) {
        super(code);
        sendAboutToReturnNode = new SendSelectorNode(code, -1, -1, aboutToReturnSelector(), 2);
        blockArgumentNode = TemporaryReadNode.create(code, 0);
        completeTempReadNode = TemporaryReadNode.create(code, 1);
        completeTempWriteNode = TemporaryWriteNode.create(code, 1);
    }

    private Object aboutToReturnSelector() {
        return code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.SelectorAboutToReturn);
    }

    /*
     * Virtualized version of Context>>aboutToReturn:through:, more specifically
     * Context>>resume:through:. This is only called if code.isUnwindMarked(), so there is no need
     * to unwind contexts here as this is already happening when NonLocalReturns are handled. Note
     * that this however does not check if the current context isDead nor does it terminate contexts
     * (this may be a problem).
     */
    @Specialization(guards = {"aboutToReturnNeedsToBeSent()", "isVirtualized(frame)"})
    protected final void doAboutToReturnVirtualized(final VirtualFrame frame, @SuppressWarnings("unused") final NonLocalReturn nlr,
                    @Cached("create()") final GetBlockFrameArgumentsNode getFrameArguments) {
        if (completeTempReadNode.executeRead(frame) == code.image.nil) {
            completeTempWriteNode.executeWrite(frame, code.image.sqTrue);
            final BlockClosureObject block = (BlockClosureObject) blockArgumentNode.executeRead(frame);
            try {
                dispatch.executeBlock(block, getFrameArguments.execute(block, getContextOrMarker(frame), new Object[0]));
            } catch (LocalReturn blockLR) { // ignore
            } catch (NonLocalReturn blockNLR) {
                if (!blockNLR.hasArrivedAtTargetContext()) {
                    throw blockNLR;
                }
            }
        }
    }

    @Specialization(guards = {"aboutToReturnNeedsToBeSent()", "!isVirtualized(frame)"})
    protected final void doAboutToReturn(final VirtualFrame frame, final NonLocalReturn nlr) {
        final ContextObject context = getContext(frame);
        pushNode.executeWrite(frame, nlr.getTargetContext());
        pushNode.executeWrite(frame, nlr.getReturnValue());
        pushNode.executeWrite(frame, context);
        sendAboutToReturnNode.executeSend(frame);
    }

    @SuppressWarnings("unused")
    @Specialization(guards = {"!aboutToReturnNeedsToBeSent()"})
    protected final void doNothing(final VirtualFrame frame, final NonLocalReturn nlr) {
        // nothing to do
    }

    protected boolean aboutToReturnNeedsToBeSent() {
        return code instanceof CompiledMethodObject && code.isUnwindMarked();
    }
}
