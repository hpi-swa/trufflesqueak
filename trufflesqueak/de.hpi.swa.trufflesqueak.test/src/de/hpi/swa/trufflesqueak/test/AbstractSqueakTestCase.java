package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertTrue;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;

public abstract class AbstractSqueakTestCase {
    protected static SqueakImageContext image;

    protected CompiledCodeObject makeMethod(byte[] bytes) {
        // Always add three literals...
        return makeMethod(bytes, new Object[]{68419598L, null, null});
    }

    protected static CompiledCodeObject makeMethod(byte[] bytes, Object[] literals) {
        CompiledMethodObject code = new CompiledMethodObject(image, bytes, literals);
        return code;
    }

    protected static CompiledCodeObject makeMethod(Object[] literals, int... intbytes) {
        byte[] bytes = new byte[intbytes.length];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        return makeMethod(bytes, literals);
    }

    protected static long makeHeader(int numArgs, int numTemps, int numLiterals, boolean hasPrimitive, boolean needsLargeFrame) { // shortcut
        return CompiledCodeObject.makeHeader(numArgs, numTemps, numLiterals, hasPrimitive, needsLargeFrame);
    }

    protected CompiledCodeObject makeMethod(int... intbytes) {
        return makeMethod(new Object[]{makeHeader(4, 5, 14, false, true)}, intbytes);
    }

    protected static Object runMethod(CompiledCodeObject code, Object receiver, Object... arguments) {
        VirtualFrame frame = createTestFrame(code);
        Object result = null;
        try {
            result = createContext(code, receiver, arguments).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
        return result;
    }

    protected ExecuteTopLevelContextNode createContext(CompiledCodeObject code, Object receiver) {
        return createContext(code, receiver, new Object[0]);
    }

    protected static ExecuteTopLevelContextNode createContext(CompiledCodeObject code, Object receiver, Object[] arguments) {
        // always use large instance size and large frame size for testing
        ContextObject testContext = ContextObject.create(code.image, 50 + CONTEXT.LARGE_FRAMESIZE);
        testContext.atput0(CONTEXT.METHOD, code);
        testContext.atput0(CONTEXT.RECEIVER, receiver);
        testContext.setInstructionPointer(0);
        testContext.setStackPointer(0);
        testContext.atput0(CONTEXT.CLOSURE_OR_NIL, code.image.nil);
        testContext.setSender(code.image.nil);
        for (int i = 0; i < arguments.length; i++) {
            testContext.push(arguments[i]);
        }
        // Initialize temps with nil in newContext.
        int numTemps = code.getNumTemps();
        for (int i = 0; i < numTemps - arguments.length; i++) {
            testContext.push(code.image.nil);
        }
        testContext.setFrameMarker(new FrameMarker());
        return ExecuteTopLevelContextNode.create(null, testContext);
    }

    protected Object runMethod(Object receiver, int... intbytes) {
        return runMethod(receiver, new BaseSqueakObject[0], intbytes);
    }

    protected Object runMethod(Object receiver, Object[] arguments, int... intbytes) {
        CompiledCodeObject cm = makeMethod(intbytes);
        return runMethod(cm, receiver, arguments);
    }

    protected Object runBinaryPrimitive(int primCode, Object rcvr, Object... arguments) {
        return runPrim(new Object[]{17104899L}, primCode, rcvr, arguments);
    }

    protected Object runQuinaryPrimitive(int primCode, Object rcvr, Object... arguments) {
        return runPrim(new Object[]{68222979L}, primCode, rcvr, arguments);
    }

    protected Object runPrim(Object[] literals, int primCode, Object rcvr, Object... arguments) {
        CompiledCodeObject cm = makeMethod(literals, new int[]{139, primCode & 0xFF, (primCode & 0xFF00) >> 8});
        return runMethod(cm, rcvr, arguments);
    }

    protected static VirtualFrame createTestFrame(CompiledCodeObject code) {
        Object[] arguments = FrameAccess.newWith(code, code.image.nil, null, new Object[0]);
        return Truffle.getRuntime().createVirtualFrame(arguments, code.getFrameDescriptor());
    }
}