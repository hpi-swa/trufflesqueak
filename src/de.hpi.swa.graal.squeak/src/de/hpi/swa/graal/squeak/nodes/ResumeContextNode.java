package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.ContextObject;

@NodeInfo(cost = NodeCost.NONE)
public final class ResumeContextNode extends RootNode {
    private final ContextObject activeContext;

    @Child private ExecuteContextNode executeContextNode;

    protected ResumeContextNode(final SqueakLanguage language, final ContextObject activeContext) {
        super(language, activeContext.getTruffleFrame().getFrameDescriptor());
        this.activeContext = activeContext;
        executeContextNode = ExecuteContextNode.create(activeContext.getBlockOrMethod());
    }

    protected ResumeContextNode(final ResumeContextNode codeNode) {
        super(codeNode.activeContext.image.getLanguage(), codeNode.getFrameDescriptor());
        activeContext = codeNode.activeContext;
        executeContextNode = codeNode.executeContextNode;
    }

    public static ResumeContextNode create(final SqueakLanguage language, final ContextObject activeContext) {
        return new ResumeContextNode(language, activeContext);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeContextNode.executeResume(frame, activeContext);
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return activeContext.toString();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
