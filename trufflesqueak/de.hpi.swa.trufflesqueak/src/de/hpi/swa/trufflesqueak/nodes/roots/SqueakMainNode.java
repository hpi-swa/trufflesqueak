package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.SqueakExit;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

public class SqueakMainNode extends SqueakMethodNode {
    private final ContextObject context;

    public SqueakMainNode(SqueakLanguage language, CompiledCodeObject code) {
        super(language, code);
        this.context = null;
    }

    public SqueakMainNode(SqueakLanguage language, CompiledCodeObject code, ContextObject activeContext) {
        super(language, code);
        this.context = activeContext;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            if (context != null) {
                return context.execute(frame);
            } else {
                return super.execute(frame);
            }
        } catch (SqueakExit e) {
            return e.code;
        }
    }
}
