package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithMethod;

public class IfNilCheck extends SqueakNodeWithMethod {
    @Child SqueakNode checkNode;
    @Children final SqueakNode[] statements;
    @Child SqueakNode result;
    private final boolean runIfNil;

    public IfNilCheck(CompiledCodeObject cc, SqueakNode pop, boolean ifNil) {
        super(cc);
        checkNode = pop;
        runIfNil = ifNil;
        statements = null;
        result = null;
    }

    public IfNilCheck(IfNilCheck base, SqueakNode[] altNodes, SqueakNode altResult) {
        super(base.method);
        checkNode = base.checkNode;
        runIfNil = base.runIfNil;
        statements = altNodes;
        result = altResult;
    }

    @Override
    @ExplodeLoop
    public Object executeGeneric(VirtualFrame frame) {
        assert statements != null || result != null;
        Object nilOrObject = checkNode.executeGeneric(frame);
        if (runIfNil == (nilOrObject == null || nilOrObject == method.image.nil)) {
            if (statements != null) {
                for (SqueakNode node : statements) {
                    node.executeGeneric(frame);
                }
            }
            if (result != null) {
                return result.executeGeneric(frame);
            } else {
                return null;
            }
        }
        return nilOrObject;
    }

    // TODO: pretty print
}
