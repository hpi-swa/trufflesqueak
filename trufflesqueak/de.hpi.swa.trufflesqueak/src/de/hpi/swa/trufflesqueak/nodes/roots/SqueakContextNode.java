package de.hpi.swa.trufflesqueak.nodes.roots;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;

/**
 * This class implements the global interpreter loop. It creates frames to execute from context
 * objects in the Squeak image and runs them. When they return, it will go up the sender chain as it
 * is in the Image and continue running. This is also the node that handles the process switching
 * logic.
 */
public class SqueakContextNode extends SqueakMethodNode {
    private ContextObject context;

    public SqueakContextNode(SqueakLanguage language, ContextObject activeContext) {
        super(language, activeContext.getCodeObject());
        context = activeContext;
    }

    @Override
    protected int initialPC() {
        int rawPC = (int) context.at0(CONTEXT.INSTRUCTION_POINTER);
        return rawPC - context.size() * 4 - 1;
    }

    @Override
    protected int initialSP() {
        return super.initialSP();
        // TODO: probably needs to be adjusted
        // return (int) context.at0(CONTEXT.STACKPOINTER);
    }

    private static ContextObject getSender(ContextObject context) {
        Object sender = context.at0(CONTEXT.SENDER);
        if (sender instanceof ContextObject) {
            return (ContextObject) sender;
        } else {
            throw new RuntimeException("sender chain ended");
        }
    }
}
