package de.hpi.swa.trufflesqueak.nodes.roots;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.LocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ContextPartConstants;

/**
 * This class implements the global interpreter loop. It creates frames to execute from context
 * objects in the Squeak image and runs them. When they return, it will go up the sender chain as it
 * is in the Image and continue running. This is also the node that handles the process switching
 * logic.
 */
public class SqueakContextNode extends RootNode {
    private ContextObject context;

    public SqueakContextNode(SqueakLanguage language, ContextObject activeContext) {
        super(language);
        context = activeContext;
    }

    private static CompiledCodeObject getCurrentMethod(ContextObject currentContext) {
        return (CompiledCodeObject) currentContext.at0(ContextPartConstants.METHOD);
    }

    private static ContextObject getSender(ContextObject context) {
        Object sender = context.at0(ContextPartConstants.SENDER);
        if (sender instanceof ContextObject) {
            return (ContextObject) sender;
        } else {
            throw new RuntimeException("sender chain ended");
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        ContextObject currentContext = context;
        while (true) {
            CompiledCodeObject method = getCurrentMethod(currentContext);
            int pc = (int) currentContext.at0(ContextPartConstants.PC);
            try {
                // This will continue execution in the active context until that
                // context returns or switches to another Squeak process.
                currentContext.step();
            } catch (LocalReturn e) {
                currentContext = getSender(currentContext);
            } catch (NonLocalReturn e) {
                // TODO: unwind context chain towards target
            } catch (NonVirtualReturn e) {
                // TODO: unwind context chain towards e.targetContext
            } catch (ProcessSwitch e) {
                // TODO: switch
            }
            throw new RuntimeException("unimplemented exit from method");
        }
    }
}
