package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExit;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public class SqueakMainNode extends SqueakMethodNode {
    public SqueakMainNode(SqueakLanguage language, CompiledCodeObject code) {
        super(language, code, false);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            return super.execute(frame);
        } catch (SqueakExit e) {
            return e.code;
        }
    }
}
