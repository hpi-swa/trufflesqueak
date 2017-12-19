package de.hpi.swa.trufflesqueak.nodes.primitives.impl;

import java.math.BigInteger;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
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
    public static abstract class PrimMousePointNode extends AbstractPrimitiveNode {

        public PrimMousePointNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object replace(@SuppressWarnings("unused") VirtualFrame frame) {
            throw new PrimitiveFailed();
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(index = 105, numArguments = 5)
    public static abstract class PrimReplaceFromToNode extends AbstractPrimitiveNode {
        public PrimReplaceFromToNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        Object replace(LargeInteger rcvr, int start, int stop, LargeInteger repl, int replStart) {
            return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
        }

        @Specialization
        Object replace(LargeInteger rcvr, int start, int stop, NativeObject repl, int replStart) {
            return replaceInLarge(rcvr, start, stop, repl.getBytes(), replStart);
        }

        @Specialization
        Object replace(LargeInteger rcvr, int start, int stop, BigInteger repl, int replStart) {
            return replaceInLarge(rcvr, start, stop, LargeInteger.getSqueakBytes(repl), replStart);
        }

        private static Object replaceInLarge(LargeInteger rcvr, int start, int stop, byte[] replBytes, int replStart) {
            byte[] rcvrBytes = rcvr.getBytes();
            int repOff = replStart - start;
            for (int i = start - 1; i < stop; i++) {
                rcvrBytes[i] = replBytes[repOff + i];
            }
            rcvr.setBytes(rcvrBytes);
            return rcvr;
        }

        @Specialization
        Object replace(NativeObject rcvr, int start, int stop, LargeInteger repl, int replStart) {
            int repOff = replStart - start;
            byte[] replBytes = repl.getBytes();
            for (int i = start - 1; i < stop; i++) {
                rcvr.setNativeAt0(i, replBytes[repOff + i]);
            }
            return rcvr;
        }

        @Specialization
        Object replace(NativeObject rcvr, int start, int stop, NativeObject repl, int replStart) {
            int repOff = replStart - start;
            for (int i = start - 1; i < stop; i++) {
                rcvr.setNativeAt0(i, repl.getNativeAt0(repOff + i));
            }
            return rcvr;
        }

        @Specialization
        Object replace(NativeObject rcvr, int start, int stop, BigInteger repl, int replStart) {
            int repOff = replStart - start;
            byte[] bytes = LargeInteger.getSqueakBytes(repl);
            for (int i = start - 1; i < stop; i++) {
                rcvr.setNativeAt0(i, bytes[repOff + i]);
            }
            return rcvr;
        }

        @Specialization
        Object replace(ListObject rcvr, int start, int stop, ListObject repl, int replStart) {
            int repOff = replStart - start;
            for (int i = start - 1; i < stop; i++) {
                rcvr.atput0(i, repl.at0(repOff + i));
            }
            return rcvr;
        }
    }

}
