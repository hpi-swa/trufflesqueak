package de.hpi.swa.graal.squeak.nodes;

import java.util.List;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.process.GetActiveProcessNode;

public abstract class GetAllInstancesNode extends AbstractNodeWithCode {
    @Child private GetActiveProcessNode getActiveProcessNode;
    @Child private GetOrCreateContextNode getOrCreateContextNode;

    public static GetAllInstancesNode create(final CompiledCodeObject code) {
        return GetAllInstancesNodeGen.create(code);
    }

    protected GetAllInstancesNode(final CompiledCodeObject code) {
        super(code);
        getActiveProcessNode = GetActiveProcessNode.create(code.image);
        getOrCreateContextNode = GetOrCreateContextNode.create(code);
    }

    public abstract List<AbstractSqueakObject> execute(VirtualFrame frame);

    @Specialization
    protected List<AbstractSqueakObject> getInstancesArray(final VirtualFrame frame) {
        final PointersObject activeProcess = getActiveProcessNode.executeGet();
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, getOrCreateContextNode.executeGet(frame, false));
        try {
            return code.image.objects.allInstances();
        } finally {
            activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, code.image.nil);
        }
    }
}
