package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;

public class SqueakMethodNode extends RootNode {
    @Child BytecodeSequence bytecode;

    public SqueakMethodNode(SqueakLanguage language, CompiledMethodObject method) {
        super(language);
        bytecode = method.getBytecodeAST();
    }

    @Override
    public BaseSqueakObject execute(VirtualFrame frame) {
        try {
            return bytecode.executeGeneric(frame);
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
