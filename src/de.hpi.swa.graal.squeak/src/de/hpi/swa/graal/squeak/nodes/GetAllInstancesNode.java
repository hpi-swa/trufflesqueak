package de.hpi.swa.graal.squeak.nodes;

import java.util.List;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.context.ObjectGraphNode;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;

public final class GetAllInstancesNode extends AbstractNodeWithCode {
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;
    @Child private ObjectGraphNode objectGraphNode;

    public static GetAllInstancesNode create(final CompiledCodeObject code) {
        return new GetAllInstancesNode(code);
    }

    protected GetAllInstancesNode(final CompiledCodeObject code) {
        super(code);
        getActiveProcessNode = GetActiveProcessNode.create(code.image);
        getOrCreateContextNode = GetOrCreateContextNode.create(code);
        objectGraphNode = ObjectGraphNode.create(code.image);
    }

    public List<AbstractSqueakObject> executeGet(final VirtualFrame frame) {
        final PointersObject activeProcess = getActiveProcessNode.executeGet();
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, getOrCreateContextNode.executeGet(frame));
        try {
            return objectGraphNode.allInstances();
        } finally {
            activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, code.image.nil);
        }
    }
}
