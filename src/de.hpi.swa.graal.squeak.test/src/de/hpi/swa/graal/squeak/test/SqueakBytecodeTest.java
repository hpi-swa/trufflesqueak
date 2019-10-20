/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.BooleanObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ASSOCIATION;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;

public class SqueakBytecodeTest extends AbstractSqueakTestCaseWithDummyImage {
    @Rule public ExpectedException exceptions = ExpectedException.none();

    @Test
    public void testPushReceiverVariables() {
        final Object[] expectedResults = getTestObjects(16);
        final AbstractSqueakObject rcvr = image.asArrayOfObjects(expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, i, 124));
        }
    }

    @Test
    public void testPopAndPushTemporaryLocations() {
        // header with numTemp=8
        final Object[] literals = new Object[]{2097154L, NilObject.SINGLETON, NilObject.SINGLETON};
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 8; i++) {
            // push true, popIntoTemp i, pushTemp i, returnTop
            final CompiledMethodObject method = makeMethod(literals, 113, 104 + i, 16 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(BooleanObject.TRUE, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testPushLiteralConstants() {
        final int bytecodeStart = 32;
        final Object[] expectedResults = getTestObjects(32);
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598L}));
        literalsList.addAll(Arrays.asList(expectedResults));
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), bytecodeStart + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(expectedResults[i], result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testPushLiteralVariables() {
        final int bytecodeStart = 64;
        final Object[] expectedResults = getTestObjects(32);
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598L}));
        literalsList.addAll(Arrays.asList(expectedResults));
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 32; i++) {
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), bytecodeStart + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(BooleanObject.FALSE, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testPopIntoReceiverVariables() {
        final int numberOfBytecodes = 8;
        final ArrayObject rcvr = image.asArrayOfObjects(createDummyLiterals(numberOfBytecodes));
        for (int i = 0; i < numberOfBytecodes; i++) {
            final int pushBC = i % 2 == 0 ? 113 : 114;
            final boolean pushValue = BooleanObject.wrap(i % 2 == 0);
            // push value; popIntoReceiver; push true; return top
            assertSame(BooleanObject.TRUE, runMethod(rcvr, pushBC, 96 + i, 113, 124));
            assertSame(pushValue, rcvr.getObject(i));
        }
    }

    // See testPopAndPushTemporaryLocations for testPopIntoTemporaryLocations.

    @Test
    public void testPushReceiver() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 112, 124));
    }

    @Test
    public void testPushConstants() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        final Object[] expectedResults = {true, false, NilObject.SINGLETON, -1L, 0L, 1L, 2L};
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 113 + i, 124));
        }
    }

    @Test
    public void testReturnReceiver() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        assertSame(rcvr, runMethod(rcvr, 120));
    }

    @Test
    public void testReturnConstants() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        final Object[] expectedResults = {true, false, NilObject.SINGLETON};
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 121 + i));
        }
    }

    @Test
    public void testUnknownBytecodes() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        int bytecode;
        for (int i = 0; i < 1; i++) {
            bytecode = 126 + i;
            try {
                runMethod(rcvr, bytecode);
                fail("Exception expected");
            } catch (final SqueakException e) {
                assertEquals("Unknown/uninterpreted bytecode: " + bytecode, e.getMessage());
            }
        }
    }

    @Test
    public void testExtendedPushReceiverVariables() {
        final Object[] expectedResults = getTestObjects(64);
        final AbstractSqueakObject rcvr = image.asArrayOfObjects(expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 128, i, 124));
        }
    }

    @Test
    public void testExtendedPushTemporaryVariables() {
        final int maxNumTemps = CONTEXT.MAX_STACK_SIZE - 1; // one stack slot required for code
        final Object[] literals = new Object[]{makeHeader(0, maxNumTemps, 0, false, true)}; // header
                                                                                            // with
        // numTemp=55
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < maxNumTemps; i++) {
            // push true, popIntoTemp i, pushTemp i, returnTop
            final CompiledMethodObject method = makeMethod(literals, 113, 130, 64 + i, 128, 64 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            final ExecuteTopLevelContextNode executeContextNode = createContext(method, rcvr);
            try {
                assertSame(BooleanObject.TRUE, executeContextNode.execute(frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testExtendedPushLiteralConstants() {
        final Object[] expectedResults = getTestObjects(64);
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598L}));
        literalsList.addAll(Arrays.asList(expectedResults));
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 128, 128 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(expectedResults[i], result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testExtendedPushLiteralVariables() {
        final Object[] expectedResults = getTestObjects(64);
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598L}));
        literalsList.addAll(Arrays.asList(expectedResults));
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 128, 192 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(BooleanObject.FALSE, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testExtendedStoreIntoReceiverVariables() {
        final int numberOfBytecodes = 64;
        final ArrayObject rcvr = image.asArrayOfObjects(createDummyLiterals(numberOfBytecodes));
        for (int i = 0; i < numberOfBytecodes; i++) {
            final int pushBC = i % 2 == 0 ? 113 : 114;
            final boolean pushValue = BooleanObject.wrap(i % 2 == 0);
            // push value; storeTopIntoReceiver; return top
            assertSame(pushValue, runMethod(rcvr, pushBC, 129, i, 124));
            assertSame(pushValue, rcvr.getObject(i));
        }
    }

    @Test
    public void testExtendedStoreIntoTemporaryVariables() {
        final int maxNumTemps = CONTEXT.MAX_STACK_SIZE - 2; // two stack slots required for code
        final Object[] literals = new Object[]{makeHeader(0, maxNumTemps, 0, false, true)};
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < maxNumTemps; i++) {
            // push true, push 1, storeIntoTemp i, pop, pushTemp i, returnTop
            final CompiledMethodObject method = makeMethod(literals, 113, 118, 129, 64 + i, 135, 128, 64 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                assertSame(1L, createContext(method, rcvr).execute(frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testExtendedStoreIntoAssociation() {
        final ArrayObject testObject = image.asArrayOfObjects(createDummyLiterals(64));

        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{64L})); // header
        // with
        // numLiterals=64
        for (int i = 0; i < 64; i++) {
            literalsList.add(testObject);
        }
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 64; i++) {
            // push true, storeIntoLiteral i, returnTop
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 113, 129, 192 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(BooleanObject.TRUE, result);
                final ArrayObject literal = (ArrayObject) method.getLiteral(i);
                assertSame(BooleanObject.TRUE, literal.getObject(ASSOCIATION.VALUE));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testExtendedPopIntoReceiverVariables() {
        final int numberOfBytecodes = 64;
        final ArrayObject rcvr = image.asArrayOfObjects(createDummyLiterals(numberOfBytecodes));
        for (int i = 0; i < numberOfBytecodes; i++) {
            final int pushBC = i % 2 == 0 ? 113 : 114;
            final boolean pushValue = BooleanObject.wrap(i % 2 == 0);
            // push value; popIntoReceiver; push true; return top
            assertSame(BooleanObject.TRUE, runMethod(rcvr, pushBC, 130, i, 113, 124));
            assertSame(pushValue, rcvr.getObject(i));
        }
    }

    @Test
    public void testExtendedPopIntoTemporaryVariables() {
        final int maxNumTemps = CONTEXT.MAX_STACK_SIZE - 2; // two stack slots required for code
        final Object[] literals = new Object[]{makeHeader(0, maxNumTemps, 0, false, true), NilObject.SINGLETON, NilObject.SINGLETON};
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < maxNumTemps; i++) {
            // push true, push 1, popIntoTemp i, pushTemp i, quickReturnTop
            final CompiledMethodObject method = makeMethod(literals, 113, 118, 130, 64 + i, 128, 64 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                assertSame(1L, createContext(method, rcvr).execute(frame));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testExtendedPopIntoLiteralVariables() {
        final int maxNumLiterals = 64; // number of accepted bytecodes
        final ArrayObject testObject = image.asArrayOfObjects(createDummyLiterals(maxNumLiterals));
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{makeHeader(0, 0, maxNumLiterals, false, true)}));
        for (int i = 0; i < maxNumLiterals; i++) {
            literalsList.add(testObject);
        }
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < maxNumLiterals; i++) {
            // push rcvr, push true, popIntoLiteral i, returnTop
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 112, 113, 130, 192 + i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(rcvr, result);
                final ArrayObject literal = (ArrayObject) method.getLiteral(i);
                assertSame(BooleanObject.TRUE, literal.getObject(ASSOCIATION.VALUE));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    // TODO: testSingleExtendedSend()
    // TODO: testDoubleExtendedSendSelf()
    // TODO: testDoubleExtendedSingleExtendedSend()

    @Test
    public void testDoubleExtendedPushReceiverVariables() {
        final Object[] expectedResults = getTestObjects(255);
        final AbstractSqueakObject rcvr = image.asArrayOfObjects(expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 132, 64, i, 124));
        }
    }

    @Test
    public void testDoubleExtendedPushLiteralConstants() {
        final Object[] expectedResults = getTestObjects(255);
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598L}));
        literalsList.addAll(Arrays.asList(expectedResults));
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 132, 96, i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(expectedResults[i], result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testDoubleExtendedPushLiteralVariables() {
        final Object[] expectedResults = getTestObjects(255);
        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{68419598L}));
        literalsList.addAll(Arrays.asList(expectedResults));
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < expectedResults.length; i++) {
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 132, 128, i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(BooleanObject.FALSE, result);
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    @Test
    public void testDoubleExtendedStoreIntoReceiverVariables() {
        final int numberOfVariables = 255;
        final ArrayObject rcvr = image.asArrayOfObjects(createDummyLiterals(numberOfVariables));
        for (int i = 0; i < numberOfVariables; i++) {
            final int pushBC = i % 2 == 0 ? 113 : 114;
            final boolean pushValue = BooleanObject.wrap(i % 2 == 0);
            // push value; storeTopIntoReceiver; return top
            assertSame(pushValue, runMethod(rcvr, pushBC, 132, 160, i, 124));
            assertSame(pushValue, rcvr.getObject(i));
        }
    }

    @Test
    public void testDoubleExtendedPopIntoReceiverVariables() {
        final int numberOfBytecodes = 255;
        final ArrayObject rcvr = image.asArrayOfObjects(createDummyLiterals(numberOfBytecodes));
        for (int i = 0; i < numberOfBytecodes; i++) {
            final int pushBC = i % 2 == 0 ? 113 : 114;
            final boolean pushValue = BooleanObject.wrap(i % 2 == 0);
            // push value; popIntoReceiver; push true; return top
            assertSame(BooleanObject.TRUE, runMethod(rcvr, pushBC, 132, 192, i, 113, 124));
            assertSame(pushValue, rcvr.getObject(i));
        }
    }

    @Test
    public void testDoubleExtendedStoreIntoAssociation() {
        final int numberOfAssociations = 255;
        final ArrayObject testObject = image.asArrayOfObjects(createDummyLiterals(numberOfAssociations));

        final List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[]{(long) numberOfAssociations})); // set
        // numLiterals
        for (int i = 0; i < numberOfAssociations; i++) {
            literalsList.add(testObject);
        }
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < numberOfAssociations; i++) {
            // push true, storeIntoLiteral i, returnTop
            final CompiledMethodObject method = makeMethod(literalsList.toArray(), 113, 132, 224, i, 124);
            final VirtualFrame frame = createTestFrame(method);
            try {
                final Object result = createContext(method, rcvr).execute(frame);
                assertSame(BooleanObject.TRUE, result);
                final ArrayObject literal = (ArrayObject) method.getLiteral(i);
                assertSame(BooleanObject.TRUE, literal.getObject(ASSOCIATION.VALUE));
            } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
                fail("broken test");
            }
        }
    }

    // TODO: testSingleExtendedSuper()

    @Test
    public void testPop() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        // push true, push false, pop, return top
        assertSame(runMethod(rcvr, 113, 114, 135, 124), BooleanObject.TRUE);
    }

    @Test
    public void testDup() {
        // push true, dup, dup, pop, pop, returnTop
        assertSame(BooleanObject.TRUE, runMethod(1L, 113, 136, 136, 135, 135, 124));
    }

    // TODO: testPushActiveContext()

    @Test
    public void testPushNewArray() {
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        final SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        final SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        // pushNewArray (size 127), returnTop
        CompiledMethodObject method = makeMethod(new Object[]{makeHeader(0, 0, 0, false, true)}, 138, 127, 124);
        Object result = runMethod(method, rcvr);
        assertTrue(result instanceof ArrayObject);
        ArrayObject resultList = (ArrayObject) result;
        assertEquals(127, sizeNode.execute(resultList));

        // pushNewArray and pop
        final int arraySize = CONTEXT.MAX_STACK_SIZE;
        final int[] intbytes = new int[arraySize + 3];
        for (int i = 0; i < arraySize; i++) {
            intbytes[i] = i % 2 == 0 ? 113 : 114; // push true or false
        }
        intbytes[arraySize] = 138; // pushNewArray
        intbytes[arraySize + 1] = 128 + arraySize; // pop, size 127
        intbytes[arraySize + 2] = 124; // returnTop
        method = makeMethod(new Object[]{makeHeader(0, 0, 0, false, true)}, intbytes);
        result = runMethod(method, rcvr);
        assertTrue(result instanceof ArrayObject);
        resultList = (ArrayObject) result;
        assertEquals(arraySize, sizeNode.execute(resultList));
        for (int i = 0; i < arraySize; i++) {
            assertEquals(BooleanObject.wrap(i % 2 == 0), at0Node.execute(resultList, i));
        }
    }

    @Test
    public void testCallPrimitive() {
        assertEquals(2L, runBinaryPrimitive(1, 1L, 1L));
    }

    @Test
    public void testCallPrimitiveFailure() {
        final int primCode = 1;
        final long rcvr = 1L;
        final NativeObject argument = image.asByteString("foo");
        // similar to SmallInteger>>#+ callPrimitive 1, returnTop
        final CompiledMethodObject method = makeMethod(new Object[]{makeHeader(1, 1, 0, true, false)}, 139, primCode & 0xFF, (primCode & 0xFF00) >> 8, 124);
        assertEquals(argument, runMethod(method, rcvr, argument));
    }

    @Test
    public void testPushRemoteTemp() {
        final Object[] literals = new Object[]{2097154L /* header with numTemp=8 */, NilObject.SINGLETON, NilObject.SINGLETON};
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        // push true, pushNewArray (size 1 and pop), popIntoTemp 2, pushRemoteTemp
        // (at(0), temp 2), returnTop
        final CompiledMethodObject method = makeMethod(literals, 113, 138, 128 + 1, 104 + 2, 140, 0, 2, 124);
        final VirtualFrame frame = createTestFrame(method);
        try {
            final Object result = createContext(method, rcvr).execute(frame);
            assertSame(BooleanObject.TRUE, result);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            fail("broken test");
        }
    }

    @Test
    public void testStoreRemoteTemp() {
        final SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        final SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        final Object[] literals = new Object[]{2097154L, NilObject.SINGLETON, NilObject.SINGLETON}; // header
                                                                                                    // with
        // numTemp=8
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        // pushNewArray (size 2), popIntoTemp 3, push true, push false,
        // storeIntoRemoteTemp (0, temp 3), storeIntoRemoteTemp (1, temp 3), pushTemp 3,
        // returnTop
        final CompiledMethodObject method = makeMethod(literals, 138, 2, 104 + 3, 113, 114, 141, 0, 3, 141, 1, 3, 19, 124);
        final VirtualFrame frame = createTestFrame(method);
        try {
            final Object result = createContext(method, rcvr).execute(frame);
            assertTrue(result instanceof ArrayObject);
            final ArrayObject resultList = (ArrayObject) result;
            assertEquals(2, sizeNode.execute(resultList));
            assertEquals(BooleanObject.FALSE, at0Node.execute(resultList, 0));
            assertEquals(BooleanObject.FALSE, at0Node.execute(resultList, 1));
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            fail("broken test");
        }
    }

    @Test
    public void testStoreAndPopRemoteTemp() {
        final SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        final SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        final Object[] literals = new Object[]{2097154L, NilObject.SINGLETON, NilObject.SINGLETON}; // header
                                                                                                    // with
        // numTemp=8
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        // pushNewArray (size 2), popIntoTemp 3, push true, push false,
        // storeIntoRemoteTemp (0, temp 3), storeIntoRemoteTemp (1, temp 3), pushTemp 3,
        // returnTop
        final CompiledMethodObject method = makeMethod(literals, 138, 2, 104 + 3, 113, 114, 142, 0, 3, 142, 1, 3, 19, 124);
        final VirtualFrame frame = createTestFrame(method);
        try {
            final Object result = createContext(method, rcvr).execute(frame);
            assertTrue(result instanceof ArrayObject);
            final ArrayObject resultList = (ArrayObject) result;
            assertEquals(2, sizeNode.execute(resultList));
            assertEquals(BooleanObject.FALSE, at0Node.execute(resultList, 0));
            assertEquals(BooleanObject.TRUE, at0Node.execute(resultList, 1));
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            fail("broken test");
        }
    }

    @Test
    public void testPushClosure() {
        // ^ [ :arg1 :arg2 | arg1 + arg2 ]
        final Object[] literals = new Object[]{2L, NilObject.SINGLETON, NilObject.SINGLETON};
        final long rcvr = 1L;
        final CompiledMethodObject method = makeMethod(literals, 0x8F, 0x02, 0x00, 0x04, 0x10, 0x11, 0xB0, 0x7D, 0x7C);
        final VirtualFrame frame = createTestFrame(method);
        final Object result = createContext(method, rcvr).execute(frame);
        assertTrue(result instanceof BlockClosureObject);
        final CompiledBlockObject block = ((BlockClosureObject) result).getCompiledBlock();
        assertEquals(2, block.getNumArgs());
        assertEquals(0, block.getNumArgsAndCopied() - block.getNumArgs());
        assertEquals(2, block.getNumTemps());
        assertArrayEquals(new byte[]{0x10, 0x11, (byte) 0xB0, 0x7D}, block.getBytes());
    }

    @Test
    public void testUnconditionalJump() {
        // 18 <90+x> jump: x
        // ...
        // x <75> pushConstant: 0
        // x+1 <7C> returnTop
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        for (int i = 0; i < 8; i++) {
            final int length = 4 + i;
            final int[] intBytes = new int[length];
            intBytes[0] = 0x90 + i;
            intBytes[length - 2] = 0x75;
            intBytes[length - 1] = 0x7C;
            assertSame(0L, runMethod(rcvr, intBytes));
        }

        // long jumpForward
        // ...
        // 40 <75> pushConstant: 0
        // 41 <7C> returnTop
        for (int i = 0; i < 4; i++) {
            final int bytecode = 164 + i;
            final int gap = ((bytecode & 7) - 4 << 8) + 20;
            final int length = 4 + gap;
            final int[] intBytes = new int[length];
            intBytes[0] = bytecode;
            intBytes[1] = 20;
            intBytes[length - 2] = 0x75;
            intBytes[length - 1] = 0x7C;
            assertSame(0L, runMethod(rcvr, intBytes));
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
        final AbstractSqueakObject rcvr = image.specialObjectsArray;
        assertSame(1L, runMethod(rcvr, 113, 0x99, 0x76, 0x7C, 0x75, 0x7C));
        assertSame(0L, runMethod(rcvr, 114, 0x99, 0x76, 0x7C, 0x75, 0x7C));

        // 17 <71> pushConstant: true
        // 18 <A8 14> jumpTrue: 40
        // ...
        // 40 <75> pushConstant: 0
        // 41 <7C> returnTop
        assertSame(0L, runMethod(rcvr,
                        113, 168, 0x14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x75, 0x7C));

        // 17 <72> pushConstant: false
        // 18 <AC 14> jumpFalse: 40
        // ...
        // 40 <75> pushConstant: 0
        // 41 <7C> returnTop
        assertSame(0L, runMethod(rcvr,
                        114, 172, 0x14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x75, 0x7C));
    }

    // TODO: testSendSelector()
    // TODO: testSend()

    private static Object[] createDummyLiterals(final int numLiterals) {
        final Object[] literals = new Object[numLiterals];
        Arrays.fill(literals, NilObject.SINGLETON);
        return literals;
    }

    private static Object[] getTestObjects(final int n) {
        final List<Object> list = new ArrayList<>();
        while (list.size() < n) {
            list.add(getTestObject());
        }
        return list.toArray();
    }

    private static ArrayObject getTestObject() {
        return image.asArrayOfObjects(NilObject.SINGLETON, BooleanObject.FALSE, BooleanObject.TRUE, image.characterClass, image.metaClass,
                        image.schedulerAssociation, image.smallIntegerClass, image.smalltalk,
                        image.specialObjectsArray);
    }
}
