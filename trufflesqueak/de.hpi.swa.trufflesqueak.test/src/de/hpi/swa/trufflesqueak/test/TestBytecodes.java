package de.hpi.swa.trufflesqueak.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMethodNode;

public class TestBytecodes extends TestSqueak {
    @Test
    public void testPushReceiverVariables() {
        Object[] expectedResults = getTestObjects(16);
        BaseSqueakObject rcvr = new PointersObject(image, image.arrayClass, expectedResults);
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, i, 124));
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
    public void testPopIntoReceiverVariables() {

    }

    @Test
    public void testPopIntoTemporaryVariables() {

    }

    @Test
    public void testPushConstants() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        Object[] expectedResults = {rcvr, true, false, null, -1, 0, 1, 2};
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 112 + i, 124));
        }
    }

    @Test
    public void testReturnConstants() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        Object[] expectedResults = {rcvr, true, false, null};
        for (int i = 0; i < expectedResults.length; i++) {
            assertSame(expectedResults[i], runMethod(rcvr, 120 + i));
        }
    }

    @Test
    public void testPop() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // push true, push false, pop, return top
        assertSame(runMethod(rcvr, 113, 114, 135, 124), image.sqTrue);
    }

    @Test
    public void testDup() {
        BaseSqueakObject rcvr = image.specialObjectsArray;
        // push 1, dup, primAdd, return top
        assertSame(runMethod(rcvr, 118, 136, 176, 124), 2);
    }

    @Test
    public void testPrimAdd() {
        BaseSqueakObject rcvr = image.wrap(1);
        // push 1, push 1, primAdd, return top
        assertSame(runMethod(rcvr, 118, 118, 176, 124), 2);
    }

    private Object[] getTestObjects(int n) {
        List<Object> list = new ArrayList<>();
        while (list.size() < n) {
            list.add(getTestObject());
        }
        return list.toArray();
    }

    private Object getTestObject() {
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
