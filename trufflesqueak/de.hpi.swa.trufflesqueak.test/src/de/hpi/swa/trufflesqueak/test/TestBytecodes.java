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
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotWriteNode;
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

    @Test
    public void testPopAndPushTemporaryVariables() {
        Object[] literals = new Object[]{2097154, image.nil, image.nil}; // header with numTemp=8
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 8; i++) {
            // push true, popIntoTemp i, pushTemp i, returnTop
            CompiledCodeObject code = makeMethod(new byte[]{113, (byte) (104 + i), (byte) (16 + i), 124}, literals);
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(image.sqTrue, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

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

    @Test
    public void testExtendedPushReceiverVariables() {
        Object[] expectedResults = getTestObjects(64);
        BaseSqueakObject rcvr = new PointersObject(image, image.arrayClass, expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 128, i, 124));
        }
    }

    @Test
    public void testExtendedPushLiteralConstants() {
        Object[] expectedResults = getTestObjects(64);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            CompiledCodeObject code = makeMethod(new byte[]{(byte) 128, (byte) (128 + i), 124}, literalsList.toArray());
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
    public void testExtendedPushLiteralVariables() {
        Object[] expectedResults = getTestObjects(64);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 32; i++) {
            CompiledCodeObject code = makeMethod(new byte[]{(byte) 128, (byte) (192 + i), 124}, literalsList.toArray());
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(image.sqFalse, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }
    // TODO: testExtendedStores()

    @Test
    public void testExtendedStoreReceiverVariables() {
        int numberOfBytecodes = 64;
        PointersObject rcvr = new PointersObject(image, image.arrayClass, new Object[numberOfBytecodes]);
        for (int i = 0; i < numberOfBytecodes; i++) {
            int pushBC = i % 2 == 0 ? 113 : 114;
            boolean pushValue = i % 2 == 0 ? image.sqTrue : image.sqFalse;
            // push value; storeTopIntoReceiver; return top
            assertSame(pushValue, runMethod(rcvr, pushBC, 129, i, 124));
            assertSame(pushValue, rcvr.getPointers()[i]);
        }
    }

    @Test
    public void testExtendedStoreTemporaryVariables() {
        Object[] literals = new Object[]{14548994, image.nil, image.nil}; // header with numTemp=55
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 64; i++) {
            // push true, storeIntoTemp i, returnTop
            CompiledCodeObject code = makeMethod(new byte[]{113, (byte) 129, (byte) (64 + i), 124}, literals);
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(image.sqTrue, result);
                assertSame(image.sqTrue, getTempValue(i, code, frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedStoreLiteralVariables() {
        PointersObject testObject = new PointersObject(
                        image,
                        image.arrayClass,
                        new Object[64]);

        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{64})); // header with numLiterals=64
        for (int i = 0; i < 64; i++) {
            literalsList.add(testObject);
        }
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 64; i++) {
            // push true, storeIntoLiteral i, returnTop
            CompiledCodeObject code = makeMethod(new byte[]{113, (byte) 129, (byte) (192 + i), 124}, literalsList.toArray());
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(image.sqTrue, result);
                PointersObject literal = (PointersObject) code.getLiteral(i);
                assertSame(image.sqTrue, literal.getPointers()[ExtendedStoreNode.ASSOCIATION_VALUE]);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedStoreAndPopReceiverVariables() {
        int numberOfBytecodes = 64;
        PointersObject rcvr = new PointersObject(image, image.arrayClass, new Object[numberOfBytecodes]);
        for (int i = 0; i < numberOfBytecodes; i++) {
            int pushBC = i % 2 == 0 ? 113 : 114;
            boolean pushValue = i % 2 == 0 ? image.sqTrue : image.sqFalse;
            // push value; popIntoReceiver; push true; return top
            assertSame(image.sqTrue, runMethod(rcvr, pushBC, 130, i, 113, 124));
            assertSame(pushValue, rcvr.getPointers()[i]);
        }
    }

    @Test
    public void testExtendedStoreAndPopTemporaryVariables() {
        Object[] literals = new Object[]{14548994, image.nil, image.nil}; // header with numTemp=55
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 55; i++) {
            // push true, storeIntoTemp i, quickReturnTop
            CompiledCodeObject code = makeMethod(new byte[]{113, (byte) 130, (byte) (64 + i), 124}, literals);
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(rcvr, result);
                assertSame(image.sqTrue, getTempValue(i, code, frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedStoreAndPopLiteralVariables() {
        PointersObject testObject = new PointersObject(
                        image,
                        image.arrayClass,
                        new Object[64]);

        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{64})); // header with numLiterals=64
        for (int i = 0; i < 64; i++) {
            literalsList.add(testObject);
        }
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 64; i++) {
            // push true, storeIntoLiteral i, returnTop
            CompiledCodeObject code = makeMethod(new byte[]{113, (byte) 130, (byte) (192 + i), 124}, literalsList.toArray());
            VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
            try {
                Object result = new SqueakMethodNode(null, code).execute(frame);
                assertSame(rcvr, result);
                PointersObject literal = (PointersObject) code.getLiteral(i);
                assertSame(image.sqTrue, literal.getPointers()[ExtendedStoreNode.ASSOCIATION_VALUE]);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

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

    private static Object getTempValue(int index, CompiledCodeObject code, VirtualFrame frame) {
        FrameSlotReadNode tempNode = FrameSlotReadNode.create(code.getStackSlot(index));
        return tempNode.executeRead(frame);
    }

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
