package de.hpi.swa.trufflesqueak.test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import junit.framework.TestCase;

public class TestBytecodes extends TestCase {
    private static final int CM_FORMAT = 24;
    private SqueakImageContext image;

    @Override
    public void setUp() {
        image = new SqueakImageContext(null, null, null);
    }

    public CompiledMethodObject makeMethod(byte[] bytes) {
        CompiledMethodObject cm = new CompiledMethodObject(image, bytes);
        return cm;
    }

    public void testPushReceiver() {
        CompiledMethodObject cm = makeMethod(new byte[]{112, 124}); // pushRcvr, returnTopFromMethod
        VirtualFrame frame = cm.createFrame(image.nil);
        try {
            BaseSqueakObject result = cm.getBytecodeAST().executeGeneric(frame);
            assertSame(result, image.nil);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            assert false;
        }
    }
}
