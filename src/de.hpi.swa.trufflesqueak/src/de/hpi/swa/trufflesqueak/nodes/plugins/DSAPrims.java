/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DenyReplace;

import com.oracle.truffle.api.strings.TruffleString;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractSingletonPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.VarHandleUtils;

public final class DSAPrims extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveExpandBlock")
    protected abstract static class PrimExpandBlockNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"buf.isTruffleStringType()", "expanded.isIntType()", "expanded.getIntLength() == 80", "buf.getByteLength() == 64"})
        protected static final Object doExpand(final Object receiver, final NativeObject buf, final NativeObject expanded, @Cached TruffleString.GetInternalByteArrayNode getBytesNode) {
            final byte[] bytes = buf.getTruffleStringAsReadonlyBytes(getBytesNode);
            for (int i = 0; i <= 15; i++) {
                expanded.setInt(i, VarHandleUtils.getIntReversed(bytes, i));
            }
            for (int i = 16; i <= 79; i += 1) {
                final long value = Integer.toUnsignedLong(expanded.getInt(i - 3) ^ expanded.getInt(i - 8) ^ expanded.getInt(i - 14) ^ expanded.getInt(i - 16));
                expanded.setInt(i, (int) (value << 1 | value >> 31)); // leftRotate:by:.
            }
            return receiver;
        }
    }

    @DenyReplace
    @SqueakPrimitive(names = "primitiveHasSecureHashPrimitive")
    public static final class PrimHasSecureHashPrimitiveNode extends AbstractSingletonPrimitiveNode implements Primitive0 {
        @Override
        public Object execute(final VirtualFrame frame, final Object receiver) {
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveHashBlock")
    protected abstract static class PrimHashBlockNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @Specialization(guards = {"buf.isIntType()", "state.isIntType()", "state.getIntLength() == 5", "buf.getIntLength() == 80"})
        protected static final Object doHash(final Object receiver, final NativeObject buf, final NativeObject state) {
            final int[] statePtr = state.getIntStorage();
            final int[] bufPtr = buf.getIntStorage();

            int a = statePtr[0];
            int b = statePtr[1];
            int c = statePtr[2];
            int d = statePtr[3];
            int e = statePtr[4];
            for (int i = 0; i <= 19; i += 1) {
                final int tmp = 1518500249 + (b & c | ~b & d) + leftRotateBy5(a) + e + bufPtr[i];
                e = d;
                d = c;
                c = leftRotateBy30(b);
                b = a;
                a = tmp;
            }
            for (int i = 20; i <= 39; i += 1) {
                final int tmp = 1859775393 + (b ^ c ^ d) + leftRotateBy5(a) + e + bufPtr[i];
                e = d;
                d = c;
                c = leftRotateBy30(b);
                b = a;
                a = tmp;
            }
            for (int i = 40; i <= 59; i += 1) {
                final int tmp = (int) 2400959708L + (b & c | b & d | c & d) + leftRotateBy5(a) + e + bufPtr[i];
                e = d;
                d = c;
                c = leftRotateBy30(b);
                b = a;
                a = tmp;
            }
            for (int i = 60; i <= 79; i += 1) {
                final int tmp = (int) 3395469782L + (b ^ c ^ d) + leftRotateBy5(a) + e + bufPtr[i];
                e = d;
                d = c;
                c = leftRotateBy30(b);
                b = a;
                a = tmp;
            }
            statePtr[0] = statePtr[0] + a;
            statePtr[1] = statePtr[1] + b;
            statePtr[2] = statePtr[2] + c;
            statePtr[3] = statePtr[3] + d;
            statePtr[4] = statePtr[4] + e;
            return receiver;
        }

        private static int leftRotateBy30(final int value) {
            final long unsignedLong = Integer.toUnsignedLong(value);
            return (int) (unsignedLong << 30 | unsignedLong >> 2);
        }

        private static int leftRotateBy5(final int value) {
            final long unsignedLong = Integer.toUnsignedLong(value);
            return (int) (unsignedLong << 5 | unsignedLong >> 27);
        }
    }

    // TODO: implement other primitives?

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return DSAPrimsFactory.getFactories();
    }

    @Override
    public List<? extends AbstractSingletonPrimitiveNode> getSingletonPrimitives() {
        return List.of(new PrimHasSecureHashPrimitiveNode());
    }
}
