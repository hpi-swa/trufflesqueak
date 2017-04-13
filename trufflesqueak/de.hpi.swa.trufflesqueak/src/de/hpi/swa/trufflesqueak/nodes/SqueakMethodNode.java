package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class SqueakMethodNode extends RootNode {
    @Child PrimitiveNode primitive;
    @Child SqueakBytecodeNode bytecode;

    public SqueakMethodNode(SqueakLanguage language, CompiledMethodObject method) {
        super(language);
        primitive = method.getPrimitiveNode();
        bytecode = method.getBytecodeNode();
    }

    @Override
    public BaseSqueakObject execute(VirtualFrame frame) {
        if (primitive != null) {
            try {
                return primitive.executeGeneric(frame);
            } catch (PrimitiveFailed e) {
                // primitive failed, fall through to execute bytecode
            }
        }
        try {
            return bytecode.executeGeneric(frame);
        } catch (NonLocalReturn e) {
            // TODO: unwind context chain towards target
        } catch (NonVirtualReturn e) {
            // TODO: unwind context chain towards e.targetContext
        } catch (ProcessSwitch e) {
            // TODO: switch
        }
        return getLanguage(SqueakLanguage.class).getContextReference().get().nil;
    }
}
