package de.hpi.swa.trufflesqueak.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;

public class TestBytecodes extends TestSqueak {
    @Rule public ExpectedException exceptions = ExpectedException.none();

    @Test
    public void testPushReceiverVariables() {
        Object[] expectedResults = getTestObjects(16);
        BaseSqueakObject rcvr = new PointersObject(image, image.arrayClass, expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, i, 124));
        }
    }

// TODO: testPushTemporaryVariables()

    @Test
    public void testPushLiteralConstants() {
        int bytecodeStart = 32;
        Object[] expectedResults = getTestObjects(32);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            CompiledCodeObject code = makeMethod(new byte[]{(byte) (bytecodeStart + i), 124}, literalsList.toArray());
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(expectedResults[i], result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testPushLiteralVariables() {
        int bytecodeStart = 64;
        Object[] expectedResults = getTestObjects(32);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 32; i++) {
            CompiledCodeObject code = makeMethod(new byte[]{(byte) (bytecodeStart + i), 124}, literalsList.toArray());
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(image.sqFalse, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testStoreAndPopReceiverVariables() {
        int numberOfBytecodes = 8;
        PointersObject rcvr = new PointersObject(image, image.arrayClass, new Object[numberOfBytecodes]);
        for (int i = 0; i < numberOfBytecodes; i++) {
            int pushBC = i % 2 == 0 ? 113 : 114;
            boolean pushValue = i % 2 == 0 ? image.sqTrue : image.sqFalse;
            // push value; popIntoReceiver; push true; return top
            assertSame(image.sqTrue, runMethod(rcvr, pushBC, 96 + i, 113, 124));
            assertSame(pushValue, rcvr.getPointers()[i]);
        }
    }

    @Test
    public void testStoreAndPopTemporaryVariables() {
        // | tempA |
        // tempA := true.
        // ^ tempA
        PointersObject rcvr = getTestObject();
        assertSame(image.sqTrue, runMethod(rcvr, 0x71, 0x68, 0x10, 0x7C));

    }

    @Test
    public void testPushReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 112, 124));
    }

    @Test
    public void testPushConstants() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        Object[] expectedResults = {true, false, image.nil, -1, 0, 1, 2};
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 113 + i, 124));
        }
    }

    @Test
    public void testReturnReceiver() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 120));
    }

    @Test
    public void testReturnConstants() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        Object[] expectedResults = {true, false, image.nil};
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 121 + i));
        }
    }

    @Test
    public void testUnknownBytecodes() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        int bytecode;
        for (int i = 0; i < 1; i++) {
            bytecode = 126 + i;
            try {
                runMethod(rcvr, bytecode);
                assertTrue("Exception expected", false);
            } catch (RuntimeException e) {
                assertEquals("Unknown/uninterpreted bytecode " + bytecode, e.getMessage());
            }
        }
    }

    // TODO: testExtendedPushes()
    // TODO: testExtendedStores()
    // TODO: testExtendedStoreAndPop()
    // TODO: testSingleExtendedSend()
    // TODO: testDoubleExtendedDoAnything()
    // TODO: testSingleExtendedSuper()
    // TODO: testSecondExtendedSend()

    @Test
    public void testPop() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // push true, push false, pop, return top
        assertSame(runMethod(rcvr, 113, 114, 135, 124), image.sqTrue);
    }

    @Test
    public void testDup() {
        BaseSqueakObject rcvr = image.wrap(1);
        // push true, dup, dup, pop, pop, return top
        assertSame(runMethod(rcvr, 113, 136, 136, 135, 135, 124), image.sqTrue);
    }
    // TODO: testPushActiveContext()
    // TODO: testPushNewArray()
    // TODO: testCallPrimitive()
    // TODO: testPushRemoteTemp()
    // TODO: testStoreRemoteTemp()
    // TODO: testStoreAndPopRemoteTemp()
    // TODO: testPushClosure()
    // TODO: testUnconditionalJump()
    // TODO: testConditionalJump()
    // TODO: testSendSelector()
    // TODO: testSend()

    private Object[] getTestObjects(int n) {
        List<Object> list = new ArrayList<>();
        while (list.size() < n) {
            list.add(getTestObject());
        }
        return list.toArray();
    }

    private PointersObject getTestObject() {
        return new PointersObject(
                        image,
                        image.arrayClass,
                        new Object[]{
                                        image.nil,
                                        image.sqFalse,
                                        image.sqTrue,
                                        image.characterClass,
                                        image.metaclass,
                                        image.schedulerAssociation,
                                        image.smallIntegerClass,
                                        image.smalltalk,
                                        image.specialObjectsArray});
    }
}
