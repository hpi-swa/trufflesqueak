package de.hpi.swa.graal.squeak.nodes;

import java.util.List;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.context.ObjectGraphNode;

public final class GetAllInstancesNode extends AbstractNodeWithCode {
    @Child private GetOrCreateContextNode getOrCreateContextNode;
    @Child private ObjectGraphNode objectGraphNode;

    protected GetAllInstancesNode(final CompiledCodeObject code) {
        super(code);
        getOrCreateContextNode = GetOrCreateContextNode.create(code);
        objectGraphNode = ObjectGraphNode.create(code.image);
    }

    public static GetAllInstancesNode create(final CompiledCodeObject code) {
        return new GetAllInstancesNode(code);
    }

    public List<AbstractSqueakObject> executeGet(final VirtualFrame frame) {
        final PointersObject activeProcess = code.image.getActiveProcess();
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, getOrCreateContextNode.executeGet(frame));
        try {
            return objectGraphNode.allInstances();
        } finally {
            activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, code.image.nil);
        }
    }
}
