package de.hpi.swa.trufflesqueak.nodes.bytecodes.jump;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.SqueakNodeWithCode;

public class IfNilCheck extends SqueakNodeWithCode {
    @Child public SqueakNode checkNode;
    @Children public final SqueakNode[] statements;
    @Child public SqueakNode result;
    public final boolean runIfNil;

    public IfNilCheck(CompiledCodeObject code, SqueakNode pop, boolean ifNil) {
        super(code);
        checkNode = pop;
        runIfNil = ifNil;
        statements = null;
        result = null;
    }

    public IfNilCheck(IfNilCheck base, SqueakNode[] altNodes, SqueakNode altResult) {
        super(base.code);
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
        if (runIfNil == (nilOrObject == null || nilOrObject == code.image.nil)) {
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
}
