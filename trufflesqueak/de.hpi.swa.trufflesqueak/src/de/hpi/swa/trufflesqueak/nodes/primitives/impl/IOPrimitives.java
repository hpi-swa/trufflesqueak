package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class IOPrimitives extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return IOPrimitivesFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 90)
    protected static abstract class PrimMousePointNode extends AbstractPrimitiveNode {

        protected PrimMousePointNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object mousePoint(@SuppressWarnings("unused") VirtualFrame frame) {
            return code.image.wrap(code.image.display.getMousePosition());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 101, variableArguments = true)
    protected static abstract class PrimBeCursorNode extends AbstractPrimitiveNode {

        protected PrimBeCursorNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object beCursor(Object[] rcvrAndArgs) {
            // TODO: display the cursor, mask is optional argument
            return rcvrAndArgs[0];
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 102)
    protected static abstract class PrimBeDisplayNode extends AbstractPrimitiveNode {

        protected PrimBeDisplayNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected boolean beDisplay(PointersObject receiver) {
            if (receiver.size() < 4) {
                throw new PrimitiveFailed();
            }
            code.image.display.open();
            code.image.specialObjectsArray.atput0(SPECIAL_OBJECT_INDEX.TheDisplay, receiver);
            return true;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 105, numArguments = 5)
    protected static abstract class PrimReplaceFromToNode extends AbstractPrimitiveNode {
        protected PrimReplaceFromToNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object replace(LargeIntegerObject rcvr, int start, int stop, LargeIntegerObject repl, int replStart) {
            return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
        }

        @Specialization
        protected Object replace(LargeIntegerObject rcvr, int start, int stop, NativeObject repl, int replStart) {
            return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
        }

        @Specialization
        protected Object replace(LargeIntegerObject rcvr, int start, int stop, BigInteger repl, int replStart) {
            return replaceInLarge(rcvr, start, stop, LargeIntegerObject.getSqueakBytes(repl), replStart);
        }

        private static Object replaceInLarge(LargeIntegerObject rcvr, int start, int stop, byte[] replBytes, int replStart) {
            byte[] rcvrBytes = rcvr.getBytes();
            int repOff = replStart - start;
            for (int i = start - 1; i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization
        protected Object replace(NativeObject rcvr, int start, int stop, LargeIntegerObject repl, int replStart) {
            int repOff = replStart - start;
            byte[] replBytes = repl.getBytes();
            for (int i = start - 1; i < stop; i++) {
                rcvr.setNativeAt0(i, replBytes[repOff + i]);
            }
            return rcvr;
        }

        @Specialization
        protected Object replace(NativeObject rcvr, int start, int stop, NativeObject repl, int replStart) {
            int repOff = replStart - start;
            for (int i = start - 1; i < stop; i++) {
                rcvr.setNativeAt0(i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @Specialization
        protected Object replace(NativeObject rcvr, int start, int stop, BigInteger repl, int replStart) {
            int repOff = replStart - start;
            byte[] bytes = LargeIntegerObject.getSqueakBytes(repl);
            for (int i = start - 1; i < stop; i++) {
                rcvr.setNativeAt0(i, bytes[repOff + i]);
            }
            return rcvr;
        }

        @Specialization
        protected Object replace(ListObject rcvr, int start, int stop, ListObject repl, int replStart) {
            int repOff = replStart - start;
            for (int i = start - 1; i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 106)
    protected static abstract class PrimScreenSizeNode extends AbstractPrimitiveNode {

        protected PrimScreenSizeNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            DisplayMode displayMode = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
            return code.image.newPoint(displayMode.getWidth(), displayMode.getHeight());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 107)
    protected static abstract class PrimMouseButtonsNode extends AbstractPrimitiveNode {

        protected PrimMouseButtonsNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.display.getButtons());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 108)
    protected static abstract class PrimKeyboardNextNode extends AbstractPrimitiveNode {

        protected PrimKeyboardNextNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.display.nextKey());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 109)
    protected static abstract class PrimKeyboardPeekNode extends AbstractPrimitiveNode {

        protected PrimKeyboardPeekNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected Object get(@SuppressWarnings("unused") BaseSqueakObject receiver) {
            return code.image.wrap(code.image.display.peekKey());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 127, numArguments = 5)
    protected static abstract class PrimDrawRectNode extends AbstractPrimitiveNode {

        protected PrimDrawRectNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject get(BaseSqueakObject receiver, int left, int right, int top, int bottom) {
            if (receiver != code.image.specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.TheDisplay)) {
                return code.image.nil;
            }
            if (!((left <= right) && (top <= bottom))) {
                return code.image.nil;
            }
            code.image.display.drawRect(left, right, top, bottom);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 133)
    protected static abstract class PrimSetInterruptKeyNode extends AbstractPrimitiveNode {

        protected PrimSetInterruptKeyNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BaseSqueakObject set(BaseSqueakObject receiver) {
            // TODO: interrupt key is obsolete in image, but maybe still needed in the vm?
            return receiver;
        }
    }
}
