/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public final class CroquetPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGatherEntropy")
    protected abstract static class PrimGatherEntropyNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @Specialization(guards = "byteArray.isByteType()")
        protected static final boolean doGather(@SuppressWarnings("unused") final Object receiver, final NativeObject byteArray) {
            ArrayUtils.fillRandomly(byteArray.getByteStorage());
            return BooleanObject.TRUE;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveMD5Transform")
    protected abstract static class PrimMD5TransformNode extends AbstractPrimitiveNode implements Primitive2WithFallback {
        @TruffleBoundary
        @Specialization(guards = {"buffer.isIntType()", "hash.isIntType()", "buffer.getIntLength() == 16", "hash.getIntLength() == 4"})
        protected static final NativeObject doMD5(@SuppressWarnings("unused") final Object receiver, final NativeObject buffer, final NativeObject hash) {
            final int[] in = buffer.getIntStorage();
            final int[] hashInts = hash.getIntStorage();

            int a = hashInts[0];
            int b = hashInts[1];
            int c = hashInts[2];
            int d = hashInts[3];

            a = step1(a, b, c, d, in[0], 0xD76AA478, 7);
            d = step1(d, a, b, c, in[1], 0xE8C7B756, 12);
            c = step1(c, d, a, b, in[2], 0x242070DB, 17);
            b = step1(b, c, d, a, in[3], 0xC1BDCEEE, 22);
            a = step1(a, b, c, d, in[4], 0xF57C0FAF, 7);
            d = step1(d, a, b, c, in[5], 0x4787C62A, 12);
            c = step1(c, d, a, b, in[6], 0xA8304613, 17);
            b = step1(b, c, d, a, in[7], 0xFD469501, 22);
            a = step1(a, b, c, d, in[8], 0x698098D8, 7);
            d = step1(d, a, b, c, in[9], 0x8B44F7AF, 12);
            c = step1(c, d, a, b, in[10], 0xFFFF5BB1, 17);
            b = step1(b, c, d, a, in[11], 0x895CD7BE, 22);
            a = step1(a, b, c, d, in[12], 0x6B901122, 7);
            d = step1(d, a, b, c, in[13], 0xFD987193, 12);
            c = step1(c, d, a, b, in[14], 0xA679438E, 17);
            b = step1(b, c, d, a, in[15], 0x49B40821, 22);

            a = step2(a, b, c, d, in[1], 0xF61E2562, 5);
            d = step2(d, a, b, c, in[6], 0xC040B340, 9);
            c = step2(c, d, a, b, in[11], 0x265E5A51, 14);
            b = step2(b, c, d, a, in[0], 0xE9B6C7AA, 20);
            a = step2(a, b, c, d, in[5], 0xD62F105D, 5);
            d = step2(d, a, b, c, in[10], 0x02441453, 9);
            c = step2(c, d, a, b, in[15], 0xD8A1E681, 14);
            b = step2(b, c, d, a, in[4], 0xE7D3FBC8, 20);
            a = step2(a, b, c, d, in[9], 0x21E1CDE6, 5);
            d = step2(d, a, b, c, in[14], 0xC33707D6, 9);
            c = step2(c, d, a, b, in[3], 0xF4D50D87, 14);
            b = step2(b, c, d, a, in[8], 0x455A14ED, 20);
            a = step2(a, b, c, d, in[13], 0xA9E3E905, 5);
            d = step2(d, a, b, c, in[2], 0xFCEFA3F8, 9);
            c = step2(c, d, a, b, in[7], 0x676F02D9, 14);
            b = step2(b, c, d, a, in[12], 0x8D2A4C8A, 20);

            a = step3(a, b, c, d, in[5], 0xFFFA3942, 4);
            d = step3(d, a, b, c, in[8], 0x8771F681, 11);
            c = step3(c, d, a, b, in[11], 0x6D9D6122, 16);
            b = step3(b, c, d, a, in[14], 0xFDE5380C, 23);
            a = step3(a, b, c, d, in[1], 0xA4BEEA44, 4);
            d = step3(d, a, b, c, in[4], 0x4BDECFA9, 11);
            c = step3(c, d, a, b, in[7], 0xF6BB4B60, 16);
            b = step3(b, c, d, a, in[10], 0xBEBFBC70, 23);
            a = step3(a, b, c, d, in[13], 0x289B7EC6, 4);
            d = step3(d, a, b, c, in[0], 0xEAA127FA, 11);
            c = step3(c, d, a, b, in[3], 0xD4EF3085, 16);
            b = step3(b, c, d, a, in[6], 0x04881D05, 23);
            a = step3(a, b, c, d, in[9], 0xD9D4D039, 4);
            d = step3(d, a, b, c, in[12], 0xE6DB99E5, 11);
            c = step3(c, d, a, b, in[15], 0x1FA27CF8, 16);
            b = step3(b, c, d, a, in[2], 0xC4AC5665, 23);

            a = step4(a, b, c, d, in[0], 0xF4292244, 6);
            d = step4(d, a, b, c, in[7], 0x432AFF97, 10);
            c = step4(c, d, a, b, in[14], 0xAB9423A7, 15);
            b = step4(b, c, d, a, in[5], 0xFC93A039, 21);
            a = step4(a, b, c, d, in[12], 0x655B59C3, 6);
            d = step4(d, a, b, c, in[3], 0x8F0CCC92, 10);
            c = step4(c, d, a, b, in[10], 0xFFEFF47D, 15);
            b = step4(b, c, d, a, in[1], 0x85845DD1, 21);
            a = step4(a, b, c, d, in[8], 0x6FA87E4F, 6);
            d = step4(d, a, b, c, in[15], 0xFE2CE6E0, 10);
            c = step4(c, d, a, b, in[6], 0xA3014314, 15);
            b = step4(b, c, d, a, in[13], 0x4E0811A1, 21);
            a = step4(a, b, c, d, in[4], 0xF7537E82, 6);
            d = step4(d, a, b, c, in[11], 0xBD3AF235, 10);
            c = step4(c, d, a, b, in[2], 0x2AD7D2BB, 15);
            b = step4(b, c, d, a, in[9], 0xEB86D391, 21);

            hashInts[0] = a + hashInts[0];
            hashInts[1] = b + hashInts[1];
            hashInts[2] = c + hashInts[2];
            hashInts[3] = d + hashInts[3];

            return hash;
        }

        private static int step1(final int w, final int x, final int y, final int z, final int data, final int add, final int s) {
            return Integer.rotateLeft(w + (z ^ x & (y ^ z)) + data + add, s) + x;
        }

        private static int step2(final int w, final int x, final int y, final int z, final int data, final int add, final int s) {
            return Integer.rotateLeft(w + (y ^ z & (x ^ y)) + data + add, s) + x;
        }

        private static int step3(final int w, final int x, final int y, final int z, final int data, final int add, final int s) {
            return Integer.rotateLeft(w + (x ^ y ^ z) + data + add, s) + x;
        }

        private static int step4(final int w, final int x, final int y, final int z, final int data, final int add, final int s) {
            return Integer.rotateLeft(w + (y ^ (x | ~z)) + data + add, s) + x;
        }
    }

    // TODO: implement other primitives?

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return CroquetPluginFactory.getFactories();
    }
}
