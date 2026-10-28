/*
 * Copyright (c) 2026-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

@SuppressWarnings("static-method")
public final class LargeIntegersTest extends AbstractSqueakTestCaseWithDummyImage {
    private static final BigInteger WORD_BASE = BigInteger.ONE.shiftLeft(Integer.SIZE);

    @Test
    public void testAnyBitFromToRegressions() {
        final long[][] cases = {{4, 3, 3}, {513, 2, 9}, {769, 2, 9}, {0x8001, 2, 15}, {0x800001, 2, 23}, {0x0180, 8, 9}};
        final Primitive2 longs = newAnyBitFromToNode();
        final Primitive2 smallNative = newAnyBitFromToNode();
        final Primitive2 largeNative = newAnyBitFromToNode();
        for (final long[] values : cases) {
            final BigInteger value = BigInteger.valueOf(values[0]);
            assertAnyBitFromTo(longs, value, values[0], values[1], values[2]);
            assertAnyBitFromTo(smallNative, value, operand(value, true), values[1], values[2]);
            final BigInteger largeValue = value.setBit(80);
            assertAnyBitFromTo(largeNative, largeValue, operand(largeValue, true), values[1], values[2]);
        }
    }

    @Test
    public void testAnyBitFromToAllByteRanges() {
        final Primitive2 longs = newAnyBitFromToNode();
        final Primitive2 smallNative = newAnyBitFromToNode();
        final Primitive2 largeNative = newAnyBitFromToNode();
        for (int digit = 0; digit <= 0xFF; digit++) {
            final BigInteger value = BigInteger.valueOf(digit);
            final BigInteger smallValue = value.setBit(40);
            final BigInteger largeValue = value.setBit(80);
            final Object smallReceiver = operand(smallValue, true);
            final Object largeReceiver = operand(largeValue, true);
            for (long start = 1; start <= Byte.SIZE; start++) {
                for (long stop = 1; stop <= Byte.SIZE; stop++) {
                    assertAnyBitFromTo(longs, value, (long) digit, start, stop);
                    assertAnyBitFromTo(smallNative, smallValue, smallReceiver, start, stop);
                    assertAnyBitFromTo(largeNative, largeValue, largeReceiver, start, stop);
                }
            }
        }
    }

    @Test
    public void testAnyBitFromToRandomRanges() {
        final Random random = new Random(44);
        final Primitive2 longs = newAnyBitFromToNode();
        final Primitive2 smallNative = newAnyBitFromToNode();
        final Primitive2 largeNative = newAnyBitFromToNode();
        for (int i = 0; i < 1000; i++) {
            final BigInteger value = BigInteger.valueOf(random.nextLong(1, Long.MAX_VALUE));
            final BigInteger largeValue = new BigInteger(256, random).setBit(255);
            final long start = random.nextLong(1, 301);
            final long stop = random.nextLong(start, 302);
            assertAnyBitFromTo(longs, value, value.longValue(), start, stop);
            assertAnyBitFromTo(smallNative, value, operand(value, true), start, stop);
            assertAnyBitFromTo(largeNative, largeValue, operand(largeValue, true), start, stop);
        }
    }

    @Test
    public void testAnyBitFromToBoundsAndRespecialization() {
        final Primitive2 primitive = newAnyBitFromToNode();
        for (final BigInteger value : new BigInteger[]{BigInteger.valueOf(4), BigInteger.ONE.shiftLeft(63), BigInteger.ONE.shiftLeft(80).add(BigInteger.valueOf(4)), BigInteger.valueOf(4)}) {
            final Object receiver = operand(value, true);
            for (final long[] range : new long[][]{{3, 3}, {1, Long.MAX_VALUE}, {63, 64}, {80, 81}, {9, 8}, {Long.MAX_VALUE, Long.MAX_VALUE}}) {
                assertAnyBitFromTo(primitive, value, receiver, range[0], range[1]);
            }
        }
        final Object zero = NativeObject.newNativeBytes(image.largePositiveIntegerClass, new byte[Long.BYTES]);
        assertAnyBitFromTo(primitive, BigInteger.ZERO, zero, 1, Long.MAX_VALUE);
        assertAnyBitFromTo(primitive, BigInteger.ZERO, 0L, 1, Long.MAX_VALUE);
        assertAnyBitFromTo(primitive, BigInteger.valueOf(Long.MAX_VALUE), Long.MAX_VALUE, 63, Long.MAX_VALUE);
    }

    @Test
    public void testMontgomeryUnsignedWords() {
        final long[][] cases = {
                        {1477878324L, 2882936584L, 4158861195L},
                        {4000000000L, 4000000000L, 4294967291L},
                        {1034063264L, 1143376560L, 3021740703L},
                        {4294967294L, 4294967294L, 4294967295L}};
        for (final long[] values : cases) {
            final BigInteger first = BigInteger.valueOf(values[0]);
            final BigInteger second = BigInteger.valueOf(values[1]);
            final BigInteger modulus = BigInteger.valueOf(values[2]);
            for (int nativeMask = 0; nativeMask < 8; nativeMask++) {
                assertMontgomery(newMontgomeryNode(), first, second, modulus, operand(first, (nativeMask & 1) != 0), operand(second, (nativeMask & 2) != 0),
                                operand(modulus, (nativeMask & 4) != 0));
            }
        }
    }

    @Test
    public void testMontgomeryRandomWords() {
        final Random random = new Random(42);
        final Primitive3 quick = newMontgomeryNode();
        final Primitive3 generic = newMontgomeryNode();
        for (int i = 0; i < 1000; i++) {
            final BigInteger modulus = BigInteger.valueOf(random.nextLong(3, 1L << Integer.SIZE) | 1);
            final BigInteger first = BigInteger.valueOf(random.nextLong(modulus.longValue()));
            final BigInteger second = BigInteger.valueOf(random.nextLong(modulus.longValue()));
            assertMontgomery(quick, first, second, modulus, first.longValue(), second.longValue(), modulus.longValue());
            assertMontgomery(generic, first, second, modulus, operand(first, true), operand(second, true), operand(modulus, true));
        }
    }

    @Test
    public void testMontgomeryMultipleWords() {
        final Random random = new Random(43);
        final Primitive3 primitive = newMontgomeryNode();
        for (final int bits : new int[]{33, 63, 64, 65, 96, 127, 128, 256}) {
            for (int i = 0; i < 100; i++) {
                final BigInteger modulus = new BigInteger(bits, random).setBit(bits - 1).setBit(0);
                final BigInteger first = new BigInteger(bits, random).mod(modulus);
                final BigInteger second = new BigInteger(bits, random).mod(modulus);
                assertMontgomery(primitive, first, second, modulus, operand(first, true), operand(second, true), operand(modulus, true));
            }
        }
    }

    @Test
    public void testMontgomeryZeroAndShortOperands() {
        final Primitive3 primitive = newMontgomeryNode();
        final Primitive3 longs = newMontgomeryNode();
        for (final BigInteger modulus : new BigInteger[]{BigInteger.ONE, BigInteger.valueOf(3), WORD_BASE.subtract(BigInteger.ONE), WORD_BASE.pow(4).subtract(BigInteger.ONE)}) {
            for (final BigInteger first : new BigInteger[]{BigInteger.ZERO, BigInteger.ONE.mod(modulus), modulus.subtract(BigInteger.ONE)}) {
                for (final BigInteger second : new BigInteger[]{BigInteger.ZERO, BigInteger.ONE.mod(modulus), modulus.subtract(BigInteger.ONE)}) {
                    assertMontgomery(primitive, first, second, modulus, operand(first, true), operand(second, true), operand(modulus, true));
                    assertMontgomery(longs, first, second, modulus, operand(first, false), operand(second, false), operand(modulus, false));
                }
            }
        }
    }

    @Test
    public void testMontgomeryAfterRespecialization() {
        final Primitive3 primitive = newMontgomeryNode();
        final BigInteger modulus = BigInteger.valueOf(4158861195L);
        final BigInteger first = BigInteger.valueOf(1477878324L);
        final BigInteger second = BigInteger.valueOf(2882936584L);
        assertMontgomery(primitive, first, second, modulus, first.longValue(), second.longValue(), modulus.longValue());
        final BigInteger largeModulus = WORD_BASE.pow(4).subtract(BigInteger.ONE);
        assertMontgomery(primitive, first, second, largeModulus, operand(first, true), operand(second, true), operand(largeModulus, true));
        assertMontgomery(primitive, first, second, modulus, first.longValue(), second.longValue(), modulus.longValue());
    }

    private static Primitive3 newMontgomeryNode() {
        return (Primitive3) newPrimitive("primMontgomeryTimesModulo");
    }

    private static Primitive2 newAnyBitFromToNode() {
        return (Primitive2) newPrimitive("primAnyBitFromTo");
    }

    private static AbstractPrimitiveNode newPrimitive(final String name) {
        return new LargeIntegers().getFactories().stream().filter(factory -> Arrays.asList(factory.getNodeClass().getAnnotation(SqueakPrimitive.class).names()).contains(
                        name)).findFirst().orElseThrow().createNode();
    }

    private static Object operand(final BigInteger value, final boolean nativeObject) {
        return nativeObject ? LargeIntegers.toNativeObject(image, value) : LargeIntegers.normalize(image, value);
    }

    private static void assertAnyBitFromTo(final Primitive2 primitive, final BigInteger value, final Object receiver, final long start, final long stop) {
        boolean expected = false;
        for (long bit = start; bit <= Math.min(stop, value.bitLength()); bit++) {
            if (value.testBit((int) bit - 1)) {
                expected = true;
                break;
            }
        }
        assertEquals("Any bit of " + value + " from " + start + " to " + stop, expected, primitive.execute(null, receiver, start, stop));
    }

    private static void assertMontgomery(final Primitive3 primitive, final BigInteger first, final BigInteger second, final BigInteger modulus,
                    final Object receiver, final Object argument, final Object modulusArgument) {
        final BigInteger radix = BigInteger.ONE.shiftLeft(Math.ceilDiv(modulus.bitLength(), Integer.SIZE) * Integer.SIZE);
        final long inverse = modulus.modInverse(WORD_BASE).negate().mod(WORD_BASE).longValueExact();
        final BigInteger expected = first.multiply(second).multiply(radix.modInverse(modulus)).mod(modulus);
        final Object result = primitive.execute(null, receiver, argument, modulusArgument, inverse);
        final BigInteger actual = result instanceof final NativeObject nativeResult ? LargeIntegers.toBigInteger(image, nativeResult) : BigInteger.valueOf((long) result);
        assertEquals("Montgomery product of " + first + " and " + second + " modulo " + modulus, expected, actual);
    }
}
