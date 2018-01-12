package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertArrayEquals;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.TopLevelContextNode;

public class SqueakBytecodeTest extends AbstractSqueakTestCase {
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
    public void testPopAndPushTemporaryLocations() {
        Object[] literals = new Object[]{2097154, image.nil, image.nil}; // header with numTemp=8
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 8; i++) {
            // push true, popIntoTemp i, pushTemp i, returnTop
            CompiledCodeObject code = makeMethod(literals, 113, 104 + i, 16 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
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
            CompiledCodeObject code = makeMethod(literalsList.toArray(), bytecodeStart + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
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
            CompiledCodeObject code = makeMethod(literalsList.toArray(), bytecodeStart + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(image.sqFalse, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testPopIntoReceiverVariables() {
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

    // See testPopAndPushTemporaryLocations for testPopIntoTemporaryLocations.

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
    public void testExtendedPushTemporaryVariables() {
        Object[] literals = new Object[]{14548994}; // header with numTemp=55
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 55; i++) {
            // push true, popIntoTemp i, pushTemp i, returnTop
            CompiledCodeObject code = makeMethod(literals, 113, 130, 64 + i, 128, 64 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            TopLevelContextNode method = createContext(code, rcvr);
            try {
                assertSame(image.sqTrue, method.execute(frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedPushLiteralConstants() {
        Object[] expectedResults = getTestObjects(64);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 128, 128 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
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
        for (int i = 0; i < expectedResults.length; i++) {
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 128, 192 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(image.sqFalse, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedStoreIntoReceiverVariables() {
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
    public void testExtendedStoreIntoTemporaryVariables() {
        Object[] literals = new Object[]{14548994, image.nil, image.nil}; // header with numTemp=55
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 55; i++) {
            // push true, push 1, storeIntoTemp i, pop, pushTemp i, returnTop
            CompiledCodeObject code = makeMethod(literals, 113, 118, 129, 64 + i, 135, 128, 64 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                assertSame(1, createContext(code, rcvr).execute(frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedStoreIntoAssociation() {
        PointersObject testObject = new PointersObject(image, image.arrayClass, new Object[64]);

        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{64})); // header with numLiterals=64
        for (int i = 0; i < 64; i++) {
            literalsList.add(testObject);
        }
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 64; i++) {
            // push true, storeIntoLiteral i, returnTop
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 113, 129, 192 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(image.sqTrue, result);
                PointersObject literal = (PointersObject) code.getLiteral(i);
                assertSame(image.sqTrue, literal.getPointers()[ASSOCIATION.VALUE]);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedPopIntoReceiverVariables() {
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
    public void testExtendedPopIntoTemporaryVariables() {
        Object[] literals = new Object[]{14548994, image.nil, image.nil}; // header with numTemp=55
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 55; i++) {
            // push true, push 1; popIntoTemp i, pushTemp i, quickReturnTop
            CompiledCodeObject code = makeMethod(literals, 113, 118, 130, 64 + i, 128, 64 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                assertSame(1, createContext(code, rcvr).execute(frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testExtendedPopIntoLiteralVariables() {
        PointersObject testObject = new PointersObject(image, image.arrayClass, new Object[64]);

        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{64})); // header with numLiterals=64
        for (int i = 0; i < 64; i++) {
            literalsList.add(testObject);
        }
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 64; i++) {
            // push true, popIntoLiteral i, returnTop
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 113, 130, 192 + i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(rcvr, result);
                PointersObject literal = (PointersObject) code.getLiteral(i);
                assertSame(image.sqTrue, literal.getPointers()[ASSOCIATION.VALUE]);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    // TODO: testSingleExtendedSend()
    // TODO: testDoubleExtendedSendSelf()
    // TODO: testDoubleExtendedSingleExtendedSend()

    @Test
    public void testDoubleExtendedPushReceiverVariables() {
        Object[] expectedResults = getTestObjects(255);
        BaseSqueakObject rcvr = new PointersObject(image, image.arrayClass, expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 132, 64, i, 124));
        }
    }

    @Test
    public void testDoubleExtendedPushLiteralConstants() {
        Object[] expectedResults = getTestObjects(255);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 132, 96, i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(expectedResults[i], result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testDoubleExtendedPushLiteralVariables() {
        Object[] expectedResults = getTestObjects(255);
        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598}));
        literalsList.addAll(Arrays.asList(expectedResults));
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 132, 128, i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(image.sqFalse, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    @Test
    public void testDoubleExtendedStoreIntoReceiverVariables() {
        int numberOfVariables = 255;
        PointersObject rcvr = new PointersObject(image, image.arrayClass, new Object[numberOfVariables]);
        for (int i = 0; i < numberOfVariables; i++) {
            int pushBC = i % 2 == 0 ? 113 : 114;
            boolean pushValue = i % 2 == 0 ? image.sqTrue : image.sqFalse;
            // push value; storeTopIntoReceiver; return top
            assertSame(pushValue, runMethod(rcvr, pushBC, 132, 160, i, 124));
            assertSame(pushValue, rcvr.getPointers()[i]);
        }
    }

    @Test
    public void testDoubleExtendedPopIntoReceiverVariables() {
        int numberOfBytecodes = 255;
        PointersObject rcvr = new PointersObject(image, image.arrayClass, new Object[numberOfBytecodes]);
        for (int i = 0; i < numberOfBytecodes; i++) {
            int pushBC = i % 2 == 0 ? 113 : 114;
            boolean pushValue = i % 2 == 0 ? image.sqTrue : image.sqFalse;
            // push value; popIntoReceiver; push true; return top
            assertSame(image.sqTrue, runMethod(rcvr, pushBC, 132, 192, i, 113, 124));
            assertSame(pushValue, rcvr.getPointers()[i]);
        }
    }

    @Test
    public void testDoubleExtendedStoreIntoAssociation() {
        int numberOfAssociations = 255;
        PointersObject testObject = new PointersObject(image, image.arrayClass, new Object[numberOfAssociations]);

        List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{numberOfAssociations})); // set numLiterals
        for (int i = 0; i < numberOfAssociations; i++) {
            literalsList.add(testObject);
        }
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < numberOfAssociations; i++) {
            // push true, storeIntoLiteral i, returnTop
            CompiledCodeObject code = makeMethod(literalsList.toArray(), 113, 132, 224, i, 124);
            VirtualFrame frame = createTestFrame(code);
            try {
                Object result = createContext(code, rcvr).execute(frame);
                assertSame(image.sqTrue, result);
                PointersObject literal = (PointersObject) code.getLiteral(i);
                assertSame(image.sqTrue, literal.getPointers()[ASSOCIATION.VALUE]);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                assertTrue("broken test", false);
            }
        }
    }

    // TODO: testSingleExtendedSuper()

    @Test
    public void testPop() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // push true, push false, pop, return top
        assertSame(runMethod(rcvr, 113, 114, 135, 124), image.sqTrue);
    }

    @Test
    public void testDup() {
        BaseSqueakObject rcvr = image.wrap(1);
        // push true, dup, dup, pop, pop, returnTop
        assertSame(image.sqTrue, runMethod(rcvr, 113, 136, 136, 135, 135, 124));
    }

    // TODO: testPushActiveContext()

    @Test
    public void testPushNewArray() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // pushNewArray (size 127), returnTop
        Object result = runMethod(rcvr, 138, 127, 124);
        assertTrue(result instanceof ListObject);
        ListObject resultList = ((ListObject) result);
        assertEquals(127, resultList.size());

        // pushNewArray and pop
        int arraySize = 100;
        int[] intbytes = new int[arraySize + 3];
        for (int i = 0; i < arraySize; i++) {
            intbytes[i] = i % 2 == 0 ? 113 : 114; // push true or false
        }
        intbytes[arraySize] = 138; // pushNewArray
        intbytes[arraySize + 1] = 128 + arraySize; // pop, size 127
        intbytes[arraySize + 2] = 124; // returnTop
        result = runMethod(rcvr, intbytes);
        assertTrue(result instanceof ListObject);
        resultList = ((ListObject) result);
        assertEquals(arraySize, resultList.size());
        for (int i = 0; i < arraySize; i++) {
            assertEquals(i % 2 == 0 ? image.sqTrue : image.sqFalse, resultList.at0(i));
        }
    }

    @Test
    public void testCallPrimitive() {
        BaseSqueakObject rcvr = image.wrap(1);
        assertEquals(BigInteger.valueOf(2), runBinaryPrimitive(1, rcvr, rcvr));
    }

    @Test
    public void testCallPrimitiveFailure() {
        int primCode = 1;
        BaseSqueakObject rcvr = image.wrap(1);
        CompiledCodeObject cm = makeMethod(new Object[]{65538},
                        // callPrimitive 1, returnTop
                        139, primCode & 0xFF, (primCode & 0xFF00) >> 8, 124);
        assertEquals(rcvr, runMethod(cm, rcvr));
    }

    @Test
    public void testPushRemoteTemp() {
        Object[] literals = new Object[]{2097154, image.nil, image.nil}; // header with numTemp=8
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // push true, pushNewArray (size 1 and pop), popIntoTemp 2, pushRemoteTemp
        // (at(0), temp 2), returnTop
        CompiledCodeObject code = makeMethod(literals, 113, 138, 128 + 1, 104 + 2, 140, 0, 2, 124);
        VirtualFrame frame = createTestFrame(code);
        try {
            Object result = createContext(code, rcvr).execute(frame);
            assertSame(image.sqTrue, result);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
    }

    @Test
    public void testStoreRemoteTemp() {
        Object[] literals = new Object[]{2097154, image.nil, image.nil}; // header with numTemp=8
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // pushNewArray (size 2), popIntoTemp 3, push true, push false,
        // storeIntoRemoteTemp (0, temp 3), storeIntoRemoteTemp (1, temp 3), pushTemp 3,
        // returnTop
        CompiledCodeObject code = makeMethod(literals, 138, 2, 104 + 3, 113, 114, 141, 0, 3, 141, 1, 3, 19, 124);
        VirtualFrame frame = createTestFrame(code);
        try {
            Object result = createContext(code, rcvr).execute(frame);
            assertTrue(result instanceof ListObject);
            ListObject resultList = ((ListObject) result);
            assertEquals(2, resultList.size());
            assertEquals(image.sqFalse, resultList.at0(0));
            assertEquals(image.sqFalse, resultList.at0(1));
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
    }

    @Test
    public void testStoreAndPopRemoteTemp() {
        Object[] literals = new Object[]{2097154, image.nil, image.nil}; // header with numTemp=8
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // pushNewArray (size 2), popIntoTemp 3, push true, push false,
        // storeIntoRemoteTemp (0, temp 3), storeIntoRemoteTemp (1, temp 3), pushTemp 3,
        // returnTop
        CompiledCodeObject code = makeMethod(literals, 138, 2, 104 + 3, 113, 114, 142, 0, 3, 142, 1, 3, 19, 124);
        VirtualFrame frame = createTestFrame(code);
        try {
            Object result = createContext(code, rcvr).execute(frame);
            assertTrue(result instanceof ListObject);
            ListObject resultList = ((ListObject) result);
            assertEquals(2, resultList.size());
            assertEquals(image.sqFalse, resultList.at0(0));
            assertEquals(image.sqTrue, resultList.at0(1));
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            assertTrue("broken test", false);
        }
    }

    @Test
    public void testPushClosure() {
        // ^ [ :arg1 :arg2 | arg1 + arg2 ]
        Object[] literals = new Object[]{2, image.nil, image.nil};
        BaseSqueakObject rcvr = image.wrap(1);
        CompiledCodeObject code = makeMethod(literals, 0x8F, 0x02, 0x00, 0x04, 0x10, 0x11, 0xB0, 0x7D, 0x7C);
        VirtualFrame frame = createTestFrame(code);
        Object result = createContext(code, rcvr).execute(frame);
        assertTrue(result instanceof BlockClosureObject);
        CompiledBlockObject block = ((BlockClosureObject) result).getCompiledBlock();
        assertEquals(2, block.getNumArgs());
        assertEquals(0, block.getNumCopiedValues());
        assertEquals(0, block.getNumTemps());
        assertArrayEquals(new byte[]{0x10, 0x11, (byte) 0xB0, 0x7D}, block.getBytes());
    }

    @Test
    public void testUnconditionalJump() {
        // 18 <90+x> jump: x
        // ...
        // x <75> pushConstant: 0
        // x+1 <7C> returnTop
        BaseSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 8; i++) {
            int length = 4 + i;
            int[] intBytes = new int[length];
            intBytes[0] = 0x90 + i;
            intBytes[length - 2] = 0x75;
            intBytes[length - 1] = 0x7C;
            assertSame(0, runMethod(rcvr, intBytes));
        }

        // long jumpForward
        // ...
        // 40 <75> pushConstant: 0
        // 41 <7C> returnTop
        for (int i = 0; i < 4; i++) {
            int bytecode = 164 + i;
            int gap = (((bytecode & 7) - 4) << 8) + 20;
            int length = 4 + gap;
            int[] intBytes = new int[length];
            intBytes[0] = bytecode;
            intBytes[1] = 20;
            intBytes[length - 2] = 0x75;
            intBytes[length - 1] = 0x7C;
            assertSame(0, runMethod(rcvr, intBytes));
        }
    }

    @Test
    public void testConditionalJump() {
        // 17 <71/72> pushConstant: true/false
        // 18 <99> jumpFalse: 21
        // 19 <76> pushConstant: 1
        // 20 <7C> returnTop
        // 21 <75> pushConstant: 0
        // 22 <7C> returnTop
        BaseSqueakObject rcvr = image.specialObjectsArray;
        assertSame(1, runMethod(rcvr, 113, 0x99, 0x76, 0x7C, 0x75, 0x7C));
        assertSame(0, runMethod(rcvr, 114, 0x99, 0x76, 0x7C, 0x75, 0x7C));

        // 17 <71> pushConstant: true
        // 18 <A8 14> jumpTrue: 40
        // ...
        // 40 <75> pushConstant: 0
        // 41 <7C> returnTop
        assertSame(0, runMethod(rcvr,
                        113, 168, 0x14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x75, 0x7C));

        // 17 <72> pushConstant: false
        // 18 <AC 14> jumpFalse: 40
        // ...
        // 40 <75> pushConstant: 0
        // 41 <7C> returnTop
        assertSame(0, runMethod(rcvr,
                        114, 172, 0x14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x75, 0x7C));
    }

    // TODO: testSendSelector()
    // TODO: testSend()

    private Object[] getTestObjects(int n) {
        List<Object> list = new ArrayList<>();
        while (list.size() < n) {
            list.add(getTestObject());
        }
        return list.toArray();
    }

    private static PointersObject getTestObject() {
        return new PointersObject(image, image.arrayClass,
                        new Object[]{image.nil, image.sqFalse, image.sqTrue, image.characterClass, image.metaclass,
                                        image.schedulerAssociation, image.smallIntegerClass, image.smalltalk,
                                        image.specialObjectsArray});
    }
}
