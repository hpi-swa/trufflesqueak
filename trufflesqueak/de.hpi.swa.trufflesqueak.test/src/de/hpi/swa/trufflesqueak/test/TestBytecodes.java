package de.hpi.swa.trufflesqueak.test;

import java.math.BigInteger;
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
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.ExtendedStoreNode;
import de.hpi.swa.trufflesqueak.nodes.context.FrameSlotReadNode;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;

public class TestBytecodes extends TestSqueak {
	@Rule
	public ExpectedException exceptions = ExpectedException.none();

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
		Object[] literals = new Object[] { 2097154, image.nil, image.nil }; // header with numTemp=8
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 8; i++) {
			// push true, popIntoTemp i, pushTemp i, returnTop
			CompiledCodeObject code = makeMethod(new byte[] { 113, (byte) (104 + i), (byte) (16 + i), 124 }, literals);
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
		List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[] { 68419598 }));
		literalsList.addAll(Arrays.asList(expectedResults));
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < expectedResults.length; i++) {
			CompiledCodeObject code = makeMethod(new byte[] { (byte) (bytecodeStart + i), 124 },
					literalsList.toArray());
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
		List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[] { 68419598 }));
		literalsList.addAll(Arrays.asList(expectedResults));
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 32; i++) {
			CompiledCodeObject code = makeMethod(new byte[] { (byte) (bytecodeStart + i), 124 },
					literalsList.toArray());
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
		Object[] expectedResults = { true, false, image.nil, -1, 0, 1, 2 };
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
		Object[] expectedResults = { true, false, image.nil };
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
		Object[] literals = new Object[] { 14548994 }; // header with numTemp=55
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 55; i++) {
			// push true, popIntoTemp i, pushTemp i, returnTop
			CompiledCodeObject code = makeMethod(
					new byte[] { 113, (byte) 130, (byte) (64 + i), (byte) 128, (byte) (64 + i), 124 }, literals);
			VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
			SqueakMethodNode method = new SqueakMethodNode(null, code);
			try {
				assertSame(image.sqTrue, method.execute(frame));
				assertSame(image.sqTrue, getTempValue(i, code, frame));
			} catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
				assertTrue("broken test", false);
			}
		}
	}

	@Test
	public void testExtendedPushLiteralConstants() {
		Object[] expectedResults = getTestObjects(64);
		List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[] { 68419598 }));
		literalsList.addAll(Arrays.asList(expectedResults));
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < expectedResults.length; i++) {
			CompiledCodeObject code = makeMethod(new byte[] { (byte) 128, (byte) (128 + i), 124 },
					literalsList.toArray());
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
		List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[] { 68419598 }));
		literalsList.addAll(Arrays.asList(expectedResults));
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 32; i++) {
			CompiledCodeObject code = makeMethod(new byte[] { (byte) 128, (byte) (192 + i), 124 },
					literalsList.toArray());
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
		Object[] literals = new Object[] { 14548994, image.nil, image.nil }; // header with numTemp=55
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 55; i++) {
			// push true, push 1, storeIntoTemp i, pop, returnTop
			CompiledCodeObject code = makeMethod(new byte[] { 113, 118, (byte) 129, (byte) (64 + i), (byte) 135, 124 },
					literals);
			VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
			try {
				Object result = new SqueakMethodNode(null, code).execute(frame);
				assertSame(image.sqTrue, result);
				assertSame(1, getTempValue(i, code, frame));
			} catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
				assertTrue("broken test", false);
			}
		}
	}

	@Test
	public void testExtendedStoreLiteralVariables() {
		PointersObject testObject = new PointersObject(image, image.arrayClass, new Object[64]);

		List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[] { 64 })); // header with numLiterals=64
		for (int i = 0; i < 64; i++) {
			literalsList.add(testObject);
		}
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 64; i++) {
			// push true, storeIntoLiteral i, returnTop
			CompiledCodeObject code = makeMethod(new byte[] { 113, (byte) 129, (byte) (192 + i), 124 },
					literalsList.toArray());
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
		Object[] literals = new Object[] { 14548994, image.nil, image.nil }; // header with numTemp=55
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 55; i++) {
			// push true, push 1; storeIntoTemp i, quickReturnTop
			CompiledCodeObject code = makeMethod(new byte[] { 113, 118, (byte) 130, (byte) (64 + i), 124 }, literals);
			VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
			try {
				Object result = new SqueakMethodNode(null, code).execute(frame);
				assertSame(image.sqTrue, result);
				assertSame(1, getTempValue(i, code, frame));
			} catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
				assertTrue("broken test", false);
			}
		}
	}

	@Test
	public void testExtendedStoreAndPopLiteralVariables() {
		PointersObject testObject = new PointersObject(image, image.arrayClass, new Object[64]);

		List<Object> literalsList = new ArrayList<>(Arrays.asList(new Object[] { 64 })); // header with numLiterals=64
		for (int i = 0; i < 64; i++) {
			literalsList.add(testObject);
		}
		BaseSqueakObject rcvr = image.specialObjectsArray;
		for (int i = 0; i < 64; i++) {
			// push true, storeIntoLiteral i, returnTop
			CompiledCodeObject code = makeMethod(new byte[] { 113, (byte) 130, (byte) (192 + i), 124 },
					literalsList.toArray());
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
		BaseSqueakObject arg = image.specialObjectsArray;
		CompiledCodeObject cm = makeMethod(new Object[] { 17104899 }, // numTemps=1
				// callPrimitive 1, pop(temp1), pop(arg), returnTop
				new int[] { 139, primCode & 0xFF, (primCode & 0xFF00) >> 8, 135, 135, 124 });
		assertEquals(rcvr, runMethod(cm, rcvr, arg));
	}

	@Test
	public void testPushRemoteTemp() {
		Object[] literals = new Object[] { 2097154, image.nil, image.nil }; // header with numTemp=8
		BaseSqueakObject rcvr = image.specialObjectsArray;
		// push true, pushNewArray (size 1 and pop), popIntoTemp 2, pushRemoteTemp
		// (at(0), temp 2), returnTop
		CompiledCodeObject code = makeMethod(
				new byte[] { 113, (byte) 138, (byte) (128 + 1), (byte) (104 + 2), (byte) (140), 0, 2, 124 }, literals);
		VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
		try {
			Object result = new SqueakMethodNode(null, code).execute(frame);
			assertSame(image.sqTrue, result);
		} catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
			assertTrue("broken test", false);
		}
	}

	@Test
	public void testStoreRemoteTemp() {
		Object[] literals = new Object[] { 2097154, image.nil, image.nil }; // header with numTemp=8
		BaseSqueakObject rcvr = image.specialObjectsArray;
		// pushNewArray (size 2), popIntoTemp 3, push true, push false,
		// storeIntoRemoteTemp (0, temp 3), storeIntoRemoteTemp (1, temp 3), pushTemp 3,
		// returnTop
		CompiledCodeObject code = makeMethod(new byte[] { (byte) 138, (byte) (2), (byte) (104 + 3), 113, 114,
				(byte) 141, 0, 3, (byte) 141, 1, 3, 19, 124 }, literals);
		VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
		try {
			Object result = new SqueakMethodNode(null, code).execute(frame);
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
		Object[] literals = new Object[] { 2097154, image.nil, image.nil }; // header with numTemp=8
		BaseSqueakObject rcvr = image.specialObjectsArray;
		// pushNewArray (size 2), popIntoTemp 3, push true, push false,
		// storeIntoRemoteTemp (0, temp 3), storeIntoRemoteTemp (1, temp 3), pushTemp 3,
		// returnTop
		CompiledCodeObject code = makeMethod(new byte[] { (byte) 138, (byte) (2), (byte) (104 + 3), 113, 114,
				(byte) 142, 0, 3, (byte) 142, 1, 3, 19, 124 }, literals);
		VirtualFrame frame = code.createTestFrame(rcvr, new BaseSqueakObject[4]);
		try {
			Object result = new SqueakMethodNode(null, code).execute(frame);
			assertTrue(result instanceof ListObject);
			ListObject resultList = ((ListObject) result);
			assertEquals(2, resultList.size());
			assertEquals(image.sqFalse, resultList.at0(0));
			assertEquals(image.sqTrue, resultList.at0(1));
		} catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
			assertTrue("broken test", false);
		}
	}

	// TODO: testPushClosure()

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
		assertSame(0, runMethod(rcvr, 113, 168, 0x14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x75,
				0x7C));

		// 17 <72> pushConstant: false
		// 18 <AC 14> jumpFalse: 40
		// ...
		// 40 <75> pushConstant: 0
		// 41 <7C> returnTop
		assertSame(0, runMethod(rcvr, 114, 172, 0x14, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0x75,
				0x7C));
	}

	// TODO: testSendSelector()
	// TODO: testSend()

	private static Object getTempValue(int index, CompiledCodeObject code, VirtualFrame frame) {
		return FrameSlotReadNode.create(code.getTempSlot(index)).executeRead(frame);
	}

	private Object[] getTestObjects(int n) {
		List<Object> list = new ArrayList<>();
		while (list.size() < n) {
			list.add(getTestObject());
		}
		return list.toArray();
	}

	private PointersObject getTestObject() {
		return new PointersObject(image, image.arrayClass,
				new Object[] { image.nil, image.sqFalse, image.sqTrue, image.characterClass, image.metaclass,
						image.schedulerAssociation, image.smallIntegerClass, image.smalltalk,
						image.specialObjectsArray });
	}
}
