package de.hpi.swa.graal.squeak.util;

/*
Copyright (c) 2015-2016 The Huldra Project.
See the LICENSE file for too long unnecessary boring license bullshit that otherwise would be written here.
Tl;dr: Use this possibly broken code however you like.

Representation:
Base is 2^32.
Magnitude as array in little endian order.
The len first ints count as the number.
Sign is represented by a sign int (-1 or 1).
Internally zero is allowed to have either sign. (Otherwise one would have to remember to check for sign-swap for div/mul etc...)
Zero has length 1 and dig[0]==0.

Principle: No Exceptions.
If a programmer divides by zero he has only himself to blame. It is OK to have undefined behavior.

Beware:
Nothing should be assumed about a position of the internal array that is not part of the number, e.g. that it is 0.
Beware of signed extensions!

Organization: Locality of reference
Stuff that has something in common should generally be close to oneanother in the code.
So stuff regarding multiplication is bunched together.

Coding style: Klein / As long as it looks good
Generally brackets on new line, but exception can be made for small special cases, then they may be aligned on the same line.
Never space after for or if or akin, it looks ugly.
Bracketless loops may be on one line. For nested bracketless loops each should be indented on a new line.
*/

import java.util.Arrays;
//import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * <p>
 * A class for arbitrary-precision integer arithmetic purely written in Java.
 * </p>
 * <p>
 * This class does what {@link java.math.BigInteger} doesn't.<br />
 * It is <b>faster</b>, and it is <b>mutable</b>!<br />
 * It supports <b>ints</b> and <b>longs</b> as parameters!<br />
 * It has a way faster {@link #toString()} method!<br />
 * It utilizes a faster multiplication algorithm for those nasty big numbers!
 * </p>
 *
 * <p>
 * Get it today! Because performance matters (and we like Java).
 * </p>
 *
 * @author Simon Klein
 * @version 0.7
 */
public class BigInt extends Number implements Comparable<BigInt> {
    /**
     *
     */
    private static final long serialVersionUID = -8909233872251836898L;

    /**
     * Used to cast a (base 2^32) digit to a long without getting unwanted sign extension.
     */
    private static final long mask = (1L << 32) - 1;

    /**
     * The sign of this number. 1 for positive numbers and -1 for negative numbers. Zero can have
     * either sign.
     */
    private int sign;
    /**
     * The number of digits of the number (in base 2^32).
     */
    private int len;
    /**
     * The digits of the number, i.e., the magnitude array.
     */
    private int[] dig;

    private int squeaksize = -1;

    /*** <Constructors> ***/
    /**
     * Creates a BigInt from the given parameters. The input-array will be used as is and not be
     * copied.
     *
     * @param sign The sign of the number.
     * @param v The magnitude of the number, the first position gives the least significant 32 bits.
     * @param len The (first) number of entries of v that are considered part of the number.
     * @complexity O(1)
     */
    public BigInt(final int sign, final int[] v, final int len) {
        assign(sign, v, len);
    }

    /**
     * Creates a BigInt from the given parameters. The contents of the input-array will be copied.
     *
     * @param sign The sign of the number.
     * @param v The magnitude of the number, the first position gives the least significant 8 bits.
     * @param fvlen The (first) number of entries of v that are considered part of the number.
     * @complexity O(n)
     */
    public BigInt(final int sign, final byte[] v, final int fvlen) {
        int vlen = fvlen;
        while (vlen > 1 && v[vlen - 1] == 0) {
            --vlen;
        }
        dig = new int[(vlen + 3) / 4];
        assign(sign, v, vlen);
    }

    /**
     * Creates a BigInt from the given parameters. The input-value will be interpreted as unsigned.
     *
     * @param sign The sign of the number.
     * @param val The magnitude of the number.
     * @complexity O(1)
     */
    public BigInt(final int sign, final int val) {
        dig = new int[1];
        uassign(sign, val);
    }

    /**
     * Creates a BigInt from the given parameters. The input-value will be interpreted as unsigned.
     *
     * @param sign The sign of the number.
     * @param val The magnitude of the number.
     * @complexity O(1)
     */
    public BigInt(final int sign, final long val) {
        dig = new int[2];
        uassign(sign, val);
    }

    /**
     * Creates a BigInt from the given int. The input-value will be interpreted a signed value.
     *
     * @param val The value of the number.
     * @complexity O(1)
     */
    public BigInt(final int val) {
        dig = new int[1];
        assign(val);
    }

    /**
     * Creates a BigInt from the given long. The input-value will be interpreted a signed value.
     *
     * @param val The value of the number.
     * @complexity O(1)
     */
    public BigInt(final long val) {
        dig = new int[2];
        assign(val);
    }

    /**
     * Creates a BigInt from the given string.
     *
     * @param s A string representing the number in decimal.
     * @complexity O(n^2)
     */
    public BigInt(final String s) {
        assign(s);
    }

    /**
     * Creates a BigInt from the given char-array.
     *
     * @param s A char array representing the number in decimal.
     * @complexity O(n^2)
     */
    public BigInt(final char[] s) {
        assign(s);
    }

    /*** </Constructors> ***/

    /*** <General Helper> ***/
    /**
     * Parses a part of a char array as an unsigned decimal number.
     *
     * @param s A char array representing the number in decimal.
     * @param ffrom The index (inclusive) where we start parsing.
     * @param to The index (exclusive) where we stop parsing.
     * @return The parsed number.
     * @complexity O(n)
     */
    private static int parse(final char[] s, final int ffrom, final int to) {
        int from = ffrom;
        int res = s[from] - '0';
        while (++from < to) {
            res = res * 10 + s[from] - '0';
        }
        return res;
    }

    /**
     * Multiplies this number and then adds something to it. I.e. sets this = this*mul + add.
     *
     * @param mul The value we multiply our number with, mul < 2^31.
     * @param add The value we add to our number, add < 2^31.
     * @complexity O(n)
     */
    private void mulAdd(final int mul, final int add) {
        long carry = 0;
        for (int i = 0; i < len; i++) {
            carry = mul * (dig[i] & mask) + carry;
            dig[i] = (int) carry;
            carry >>>= 32;
        }
        if (carry != 0) {
            dig[len++] = (int) carry;
        }
        carry = (dig[0] & mask) + add;
        dig[0] = (int) carry;
        if (carry >>> 32 != 0) {
            int i = 1;
            for (; i < len && ++dig[i] == 0; ++i) {
            }
            if (i == len) {
                dig[len++] = 1; // Todo: realloc() for general case?
            }
        }
    }

    /**
     * Reallocates the magnitude array to one twice its size.
     *
     * @complexity O(n)
     */
    private void realloc() {
        final int[] res = new int[dig.length * 2];
        System.arraycopy(dig, 0, res, 0, len);
        dig = res;
    }

    /**
     * Reallocates the magnitude array to one of the given size.
     *
     * @param newLen The new size of the magnitude array.
     * @complexity O(n)
     */
    private void realloc(final int newLen) {
        final int[] res = new int[newLen];
        System.arraycopy(dig, 0, res, 0, len);
        dig = res;
        squeaksize = -1; // Reset explicit size for squeak
    }

    /*** </General Helper> ***/

    /*** <General functions> ***/
    /**
     * Creates a copy of this number.
     *
     * @return The BigInt copy.
     * @complexity O(n)
     */
    public BigInt copy() {
        return new BigInt(sign, Arrays.copyOf(dig, len), len);
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param other BigInt to copy/assign to this BigInt.
     * @complexity O(n)
     */
    public void assign(final BigInt other) {
        sign = other.sign;
        assign(other.dig, other.len);
    }

    /**
     * Assigns the content of the given magnitude array and the length to this number. The contents
     * of the input will be copied.
     *
     * @param v The new magnitude array content.
     * @param vlen The length of the content, vlen > 0.
     * @complexity O(n)
     */
    private void assign(final int[] v, final int vlen) { // Todo: Better and more consistent naming.
        if (vlen > dig.length) {
            dig = new int[vlen + 2];
        }
        System.arraycopy(v, 0, dig, 0, len = vlen);
    }

    /**
     * Assigns the given BigInt parameter to this number. The input magnitude array will be used as
     * is and not copied.
     *
     * @param fsign The sign of the number.
     * @param v The magnitude of the number.
     * @param flen The length of the magnitude array to be used.
     * @complexity O(1)
     */
    public void assign(final int fsign, final int[] v, final int flen) {
        this.sign = fsign;
        this.len = flen;
        dig = v;
    }

    /**
     * Assigns the given BigInt parameter to this number. Assumes no leading zeroes of the
     * input-array, i.e. that v[vlen-1]!=0, except for the case when vlen==1.
     *
     * @param fsign The sign of the number.
     * @param v The magnitude of the number.
     * @param vlen The length of the magnitude array to be used.
     * @complexity O(n)
     */
    public void assign(final int fsign, final byte[] v, final int vlen) {
        len = (vlen + 3) / 4;
        if (len > dig.length) {
            dig = new int[len + 2];
        }
        this.sign = fsign;
        int tmp = vlen / 4, j = 0;
        for (int i = 0; i < tmp; i++, j += 4) {
            dig[i] = v[j + 3] << 24 | (v[j + 2] & 0xFF) << 16 | (v[j + 1] & 0xFF) << 8 | v[j] & 0xFF;
        }
        if (tmp != len) {
            tmp = v[j++] & 0xFF;
            if (j < vlen) {
                tmp |= (v[j++] & 0xFF) << 8;
                if (j < vlen) {
                    tmp |= (v[j] & 0xFF) << 16;
                }
            }
            dig[len - 1] = tmp;
        }
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param s A string representing the number in decimal.
     * @complexity O(n^2)
     */
    public void assign(final String s) {
        assign(s.toCharArray());
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param s A char array representing the number in decimal.
     * @complexity O(n^2)
     */
    public void assign(final char[] s) {
        sign = s[0] == '-' ? -1 : 1;

        len = s.length + (sign - 1 >> 1);
        final int alloc = len < 10 ? 1 : (int) (len * 3402L >>> 10) + 32 >>> 5; // 3402 = bits per
                                                                                // digit * 1024
        if (dig == null || alloc > dig.length) {
            dig = new int[alloc];
        }

        int j = len % 9;
        if (j == 0) {
            j = 9;
        }
        j -= sign - 1 >> 1;

        dig[0] = parse(s, 0 - (sign - 1 >> 1), j);
        for (len = 1; j < s.length;) {
            mulAdd(1_000_000_000, parse(s, j, j += 9));
        }
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param s The sign of the number.
     * @param val The magnitude of the number (will be intepreted as unsigned).
     * @complexity O(1)
     */
    public void uassign(final int s, final int val) {
        sign = s;
        len = 1;
        dig[0] = val;
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param s The sign of the number.
     * @param val The magnitude of the number (will be intepreted as unsigned).
     * @complexity O(1)
     */
    public void uassign(final int s, final long val) {
        sign = s;
        len = 2;
        if (dig.length < 2) {
            realloc(2);
        }
        dig[0] = (int) (val & mask);
        dig[1] = (int) (val >>> 32);
        if (dig[1] == 0) {
            --len;
        }
    }

    /**
     * Assigns the given non-negative number to this BigInt object.
     *
     * @param val The number interpreted as unsigned.
     * @complexity O(1)
     */
    public void uassign(final int val) {
        uassign(1, val);
    }

    /**
     * Assigns the given non-negative number to this BigInt object.
     *
     * @param val The number interpreted as unsigned.
     * @complexity O(1)
     */
    public void uassign(final long val) {
        uassign(1, val);
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param val The number to be assigned.
     * @complexity O(1)
     */
    public void assign(final int val) {
        uassign(val < 0 ? -1 : 1, val < 0 ? -val : val);
    }

    /**
     * Assigns the given number to this BigInt object.
     *
     * @param val The number to be assigned.
     * @complexity O(1)
     */
    public void assign(final long val) {
        uassign(val < 0 ? -1 : 1, val < 0 ? -val : val);
    }

    /**
     * Tells whether this number is zero or not.
     *
     * @return true if this number is zero, false otherwise
     * @complexity O(1)
     */
    public boolean isZero() {
        return dig.length == 0 || len == 1 && dig[0] == 0;
    }

    /**
     * Sets this number to zero.
     *
     * @complexity O(1)
     */
    private void setToZero() {
        dig[0] = 0;
        len = 1;
        sign = 1; // Remove?
    }

    /**
     * Compares the absolute value of this and the given number.
     *
     * @param a The number to be compared with.
     * @return -1 if the absolute value of this number is less, 0 if it's equal, 1 if it's greater.
     * @complexity O(n)
     */
    public int compareAbsTo(final BigInt a) {
        if (len > a.len) {
            return 1;
        }
        if (len < a.len) {
            return -1;
        }
        for (int i = len - 1; i >= 0; i--) {
            if (dig[i] != a.dig[i]) {
                if ((dig[i] & mask) > (a.dig[i] & mask)) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }
        return 0;
    }

    /**
     * Compares the value of this and the given number.
     *
     * @param a The number to be compared with.
     * @return -1 if the value of this number is less, 0 if it's equal, 1 if it's greater.
     * @complexity O(n)
     */
    @Override
    public int compareTo(final BigInt a) {
        if (sign < 0) {
            if (a.sign < 0 || a.isZero()) {
                return -compareAbsTo(a);
            }
            return -1;
        }
        if (a.sign > 0 || a.isZero()) {
            return compareAbsTo(a);
        }
        return 1;
    }

    /**
     * Tests equality of this number and the given one.
     *
     * @param a The number to be compared with.
     * @return true if the two numbers are equal, false otherwise.
     * @complexity O(n)
     */
    public boolean equals(final BigInt a) {
        if (len != a.len) {
            return false;
        }
        if (isZero() && a.isZero()) {
            return true;
        }
        if ((sign ^ a.sign) < 0) {
            return false; // In case definition of sign would change...
        }
        for (int i = 0; i < len; i++) {
            if (dig[i] != a.dig[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object o) { // Todo: Equality on other Number objects?

        if (o instanceof BigInt) {
            return equals((BigInt) o);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hash = 0; // Todo: Opt and improve.
        for (int i = 0; i < len; i++) {
            hash = (int) (31 * hash + (dig[i] & mask));
        }
        return sign * hash; // relies on 0 --> hash==0.
    }

    /*** </General functions> ***/

    /*** <Number Override> ***/
    /**
     * {@inheritDoc} Returns this BigInt as a {@code byte}.
     *
     * @return {@code sign * (this & 0x7F)}
     */
    @Override
    public byte byteValue() {
        if (dig.length == 0) {
            return 0;
        }
        return (byte) (sign * (dig[0] & 0x7F));
    }

    /**
     * {@inheritDoc} Returns this BigInt as a {@code short}.
     *
     * @return {@code sign * (this & 0x7FFF)}
     */
    @Override
    public short shortValue() {
        if (dig.length == 0) {
            return 0;
        }
        return (short) (sign * (dig[0] & 0x7FFF));
    }

    /**
     * {@inheritDoc} Returns this BigInt as an {@code int}.
     *
     * @return {@code sign * (this & 0x7FFFFFFF)}
     */
    @Override
    public int intValue() {
        if (dig.length == 0) {
            return 0;
        }
        return sign * (dig[0] & 0x7FFFFFFF); // relies on that sign always is either 1/-1.
    }

    /**
     * {@inheritDoc} Returns this BigInt as a {@code long}.
     *
     * @return {@code sign * (this & 0x7FFFFFFFFFFFFFFF)}
     */
    @Override
    public long longValue() {
        if (dig.length == 0) {
            return 0L;
        }
        // TODO: Submit Huldra issue: -9223372036854775808.longValue()
        if (sign < 0) {
            return len == 1 ? sign * (dig[0] & mask) : sign * ((dig[1] & 0xFFFFFFFFL) << 32 | dig[0] & mask);
        } else {
            return len == 1 ? sign * (dig[0] & mask) : sign * ((dig[1] & 0x7FFFFFFFL) << 32 | dig[0] & mask);
        }
    }

    /**
     * {@inheritDoc} Returns this BigInt as a {@code float}.
     *
     * @return the most significant 24 bits in the mantissa (the highest order bit obviously being
     *         implicit), the exponent value which will be consistent for {@code BigInt}s up to 128
     *         bits (should it not fit it'll be calculated modulo 256), and the sign bit set if this
     *         number is negative.
     */
    @Override
    public float floatValue() {
        final int s = Integer.numberOfLeadingZeros(dig[len - 1]);
        if (len == 1 && s >= 8) {
            return sign * dig[0];
        }

        int bits = dig[len - 1]; // Mask out the 24 MSBits.
        if (s <= 8) {
            bits >>>= 8 - s;
        } else {
            bits = bits << s - 8 | dig[len - 2] >>> 32 - (s - 8); // s-8==additional bits we need.
        }
        bits ^= 1L << 23; // The leading bit is implicit, cancel it out.

        final int exp = (int) (32 - s + 32L * (len - 1) - 1 + 127 & 0xFF);
        bits |= exp << 23; // Add exponent.
        bits |= sign & 1 << 31; // Add sign-bit.

        return Float.intBitsToFloat(bits);
    }

    /**
     * {@inheritDoc} Returns this BigInt as a {@code double}.
     *
     * @return the most significant 53 bits in the mantissa (the highest order bit obviously being
     *         implicit), the exponent value which will be consistent for {@code BigInt}s up to 1024
     *         bits (should it not fit it'll be calculated modulo 2048), and the sign bit set if
     *         this number is negative.
     */
    @Override
    public double doubleValue() {
        if (len == 1) {
            return sign * (dig[0] & mask);
        }

        final int s = Integer.numberOfLeadingZeros(dig[len - 1]);
        if (len == 2 && 32 - s + 32 <= 53) {
            return sign * ((long) dig[1] << 32 | dig[0] & mask);
        }

        long bits = (long) dig[len - 1] << 32 | dig[len - 2] & mask; // Mask out the 53 MSBits.
        if (s <= 11) {
            bits >>>= 11 - s;
        } else {
            bits = bits << s - 11 | dig[len - 3] >>> 32 - (s - 11); // s-11==additional bits we
        }
        // need.
        bits ^= 1L << 52; // The leading bit is implicit, cancel it out.

        final long exp = 32 - s + 32L * (len - 1) - 1 + 1023 & 0x7FF;
        bits |= exp << 52; // Add exponent.
        bits |= sign & 1L << 63; // Add sign-bit.

        return Double.longBitsToDouble(bits);
    }

    /*** </Number Override> ***/

    /*** <Unsigned Int Num> ***/
    /**
     * Increases the magnitude of this number.
     *
     * @param a The amount of the increase (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    private void uaddMag(final int a) {
        final long tmp = (dig[0] & mask) + (a & mask);
        dig[0] = (int) tmp;
        if (tmp >>> 32 != 0) {
            int i = 1;
            for (; i < len && ++dig[i] == 0; i++) {
            }
            if (i == len) {
                if (len == dig.length) {
                    realloc();
                }
                dig[len++] = 1;
            }
        }
    }

    /**
     * Decreases the magnitude of this number. If s > this behaviour is undefined.
     *
     * @param s The amount of the decrease (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    private void usubMag(final int s) {
        final long dif = (dig[0] & mask) - (s & mask);
        dig[0] = (int) dif;
        if (dif >> 32 != 0) {
            int i = 1;
            for (; dig[i] == 0; i++) {
                --dig[i];
            }
            if (--dig[i] == 0 && i + 1 == len) {
                --len;
            }
        }
    }

    /**
     * Adds an unsigned int to this number.
     *
     * @param a The amount to add (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    public void uadd(final int a) {
        if (sign < 0) {
            if (len > 1 || (dig[0] & mask) > (a & mask)) {
                usubMag(a);
                return;
            }
            sign = 1;
            dig[0] = a - dig[0];
            return;
        }

        uaddMag(a);
    }

    /**
     * Subtracts an unsigned int from this number.
     *
     * @param s The amount to subtract (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    public void usub(final int s) {
        if (sign < 0) {
            uaddMag(s);
            return;
        }
        if (len == 1 && (dig[0] & mask) < (s & mask)) {
            sign = -1;
            dig[0] = s - dig[0];
            return;
        }

        usubMag(s);
    }

    /**
     * Multiplies this number with an unsigned int.
     *
     * @param mul The amount to multiply (treated as unsigned).
     * @complexity O(n)
     */
    public void umul(final int mul) { // mul is interpreted as unsigned
        if (mul == 0) {
            setToZero();
            return;
        } // To be removed?

        long carry = 0;
        final long m = mul & mask;
        for (int i = 0; i < len; i++) {
            carry = (dig[i] & mask) * m + carry;
            dig[i] = (int) carry;
            carry >>>= 32;
        }
        if (carry != 0) {
            if (len == dig.length) {
                realloc();
            }
            dig[len++] = (int) carry;
        }
    }

    /**
     * Divides this number with an unsigned int and returns the remainder.
     *
     * @param div The amount to divide with (treated as unsigned).
     * @return The absolute value of the remainder as an unsigned int.
     * @complexity O(n)
     */
    public int udiv(final int div) { // returns the unsigned remainder!

        if (div < 0) {
            return safeUdiv(div);
        } else {
            return unsafeUdiv(div);
        }
    }

    // Assumes div > 0.
    private int unsafeUdiv(final int div) {
        final long d = div & mask;
        long rem = 0;
        for (int i = len - 1; i >= 0; i--) {
            rem <<= 32;
            rem = rem + (dig[i] & mask);
            dig[i] = (int) (rem / d); // Todo: opt?
            rem = rem % d;
        }
        if (dig[len - 1] == 0 && len > 1) {
            --len;
        }
        if (len == 1 && dig[0] == 0) {
            sign = 1;
        }
        return (int) rem;
    }

    // Assumes div < 0.
    private int safeUdiv(final int div) {
        final long d = div & mask, hbit = Long.MIN_VALUE;
        long hq = (hbit - 1) / d;
        if (hq * d + d == hbit) {
            ++hq;
        }
        final long hrem = hbit - hq * d;
        long rem = 0;
        for (int i = len - 1; i >= 0; i--) {
            rem = (rem << 32) + (dig[i] & mask);
            final long q = (hq & rem >> 63) + ((rem & hbit - 1) + (hrem & rem >> 63)) / d;
            rem = rem - q * d;
            dig[i] = (int) q;
        }
        if (dig[len - 1] == 0 && len > 1) {
            --len;
        }
        if (len == 1 && dig[0] == 0) {
            sign = 1;
        }
        return (int) rem;
    }

    /**
     * Modulos this number with an unsigned int. I.e. sets this number to this % mod.
     *
     * @param mod The amount to modulo with (treated as unsigned).
     * @complexity O(n)
     */
    public void urem(final int mod) {
        if (mod < 0) {
            safeUrem(mod);
        } else {
            unsafeUrem(mod);
        }
    }

    // Assumes mod > 0.
    private void unsafeUrem(final int mod) {
        long rem = 0;
        final long d = mod & mask;
        for (int i = len - 1; i >= 0; i--) {
            rem <<= 32;
            rem = (rem + (dig[i] & mask)) % d;
        }
        len = 1;
        dig[0] = (int) rem;
        if (dig[0] == 0) {
            sign = 1;
        }
    }

    // Assumes mod < 0.
    private void safeUrem(final int mod) {
        final long d = mod & mask, hbit = Long.MIN_VALUE;
        // Precompute hrem = (1<<63) % d
        // I.e. the remainder caused by the highest bit.
        long hrem = (hbit - 1) % d;
        if (++hrem == d) {
            hrem = 0;
        }
        long rem = 0;
        for (int i = len - 1; i >= 0; i--) {
            rem = (rem << 32) + (dig[i] & mask);
            // Calculate rem %= d.
            // Do this by calculating the lower 63 bits and highest bit separately.
            // The highest bit remainder only gets added if it's set.
            rem = ((rem & hbit - 1) + (hrem & rem >> 63)) % d;
            // The addition is safe and cannot overflow.
            // Because hrem < 2^32 and there's at least one zero bit in [62,32] if bit 63 is set.
        }
        len = 1;
        dig[0] = (int) rem;
        if (dig[0] == 0) {
            sign = 1;
        }
    }

    /*** </Unsigned Int Num> ***/

    /*** <Unsigned Long Num> ***/
    /**
     * Increases the magnitude of this number.
     *
     * @param a The amount of the increase (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    private void uaddMag(final long a) {
        if (dig.length <= 2) {
            realloc(3);
            len = 2;
        }

        final long ah = a >>> 32, al = a & mask;
        long carry = (dig[0] & mask) + al;
        dig[0] = (int) carry;
        carry >>>= 32;
        carry = (dig[1] & mask) + ah + carry;
        dig[1] = (int) carry;
        if (carry >> 32 != 0) {
            int i = 2;
            for (; i < len && ++dig[i] == 0; i++) {
            }
            if (i == len) {
                if (len == dig.length) {
                    realloc();
                }
                dig[len++] = 1;
            }
        } else if (len == 2 && dig[1] == 0) {
            --len;
        }
    }

    /**
     * Decreases the magnitude of this number. If s > this behaviour is undefined.
     *
     * @param s The amount of the decrease (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    private void usubMag(final long s) {
        final long sh = s >>> 32, sl = s & mask;
        long dif = (dig[0] & mask) - sl;
        dig[0] = (int) dif;
        dif >>= 32;
        dif = (dig[1] & mask) - sh + dif;
        dig[1] = (int) dif;
        if (dif >> 32 != 0) {
            int i = 2;
            for (; dig[i] == 0; i++) {
                --dig[i];
            }
            if (--dig[i] == 0 && i + 1 == len) {
                --len;
            }
        }
        if (len == 2 && dig[1] == 0) {
            --len;
        }
    }

    /**
     * Adds an unsigned long to this number.
     *
     * @param a The amount to add (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    public void uadd(final long a) { // Refactor? Similar to usub.

        if (sign < 0) {
            final long ah = a >>> 32, al = a & mask;
            if (len > 2 || len == 2 && ((dig[1] & mask) > ah || (dig[1] & mask) == ah && (dig[0] & mask) >= al) || ah == 0 && (dig[0] & mask) >= al) {
                usubMag(a);
                return;
            }
            if (dig.length == 1) {
                realloc(2);
            }
            if (len == 1) {
                dig[len++] = 0;
            }
            long dif = al - (dig[0] & mask);
            dig[0] = (int) dif;
            dif >>= 32;
            dif = ah - (dig[1] & mask) + dif;
            dig[1] = (int) dif;
            // dif>>32 != 0 should be impossible
            if (dif == 0) {
                --len;
            }
            sign = 1;
        } else {
            uaddMag(a);
        }
    }

    /**
     * Subtracts an unsigned long from this number.
     *
     * @param a The amount to subtract (treated as unsigned).
     * @complexity O(n)
     * @amortized O(1)
     */
    public void usub(final long a) { // Fix parameter name

        if (sign > 0) {
            final long ah = a >>> 32, al = a & mask;
            if (len > 2 || len == 2 && ((dig[1] & mask) > ah || (dig[1] & mask) == ah && (dig[0] & mask) >= al) || ah == 0 && (dig[0] & mask) >= al) {
                usubMag(a);
                return;
            }
            if (dig.length == 1) {
                realloc(2);
            }
            if (len == 1) {
                dig[len++] = 0;
            }
            long dif = al - (dig[0] & mask);
            dig[0] = (int) dif;
            dif >>= 32;
            dif = ah - (dig[1] & mask) + dif;
            dig[1] = (int) dif;
            // dif>>32 != 0 should be impossible
            if (dif == 0) {
                --len;
            }
            sign = -1;
        } else {
            uaddMag(a);
        }
    }

    /**
     * Multiplies this number with an unsigned long.
     *
     * @param mul The amount to multiply (treated as unsigned).
     * @complexity O(n)
     */
    public void umul(final long mul) {
        if (mul == 0) {
            setToZero();
            return;
        }
        if (len + 2 >= dig.length) {
            realloc(2 * len + 1);
        }

        final long mh = mul >>> 32, ml = mul & mask;
        long carry = 0, next = 0, tmp;
        for (int i = 0; i < len; i++) {
            carry = carry + next; // Could this overflow?
            tmp = (dig[i] & mask) * ml;
            next = (dig[i] & mask) * mh;
            dig[i] = (int) (tmp + carry);
            carry = (tmp >>> 32) + (carry >>> 32) + ((tmp & mask) + (carry & mask) >>> 32);
        }
        carry = carry + next;
        dig[len++] = (int) carry;
        dig[len++] = (int) (carry >>> 32);

        while (len > 1 && dig[len - 1] == 0) {
            --len;
        }
    }

    /**
     * Divides this number with an unsigned long and returns the remainder.
     *
     * @param div The amount to divide with (treated as unsigned).
     * @return The absolute value of the remainder as an unsigned long.
     * @complexity O(n)
     */
    public long udiv(final long div) { // Adaption of general div to long.

        if (div == (div & mask)) {
            return udiv((int) div) & mask;
        }
        if (len == 1) {
            final long tmp = dig[0] & mask;
            setToZero();
            return tmp;
        }

        final int s = Integer.numberOfLeadingZeros((int) (div >>> 32));
        final long dh = div >>> 32 - s, dl = div << s & mask, hbit = Long.MIN_VALUE;

        long u2 = 0, u1 = dig[len - 1] >>> 32 - s, u0 = (dig[len - 1] << s | dig[len - 2] >>> 32 - s) & mask;
        if (s == 0) {
            u1 = 0;
            u0 = dig[len - 1] & mask;
        }
        for (int j = len - 2; j >= 0; j--) {
            u2 = u1;
            u1 = u0;
            u0 = s > 0 && j > 0 ? (dig[j] << s | dig[j - 1] >>> 32 - s) & mask : dig[j] << s & mask;

            long k = (u2 << 32) + u1; // Unsigned division is a pain in the ass! ='(
            long qhat = (k >>> 1) / dh << 1;
            long t = k - qhat * dh;
            if (t + hbit >= dh + hbit) {
                qhat++; // qhat = (u[j+n]*b + u[j+n-1])/v[n-1];
            }
            long rhat = k - qhat * dh;

            while (qhat + hbit >= (1L << 32) + hbit || qhat * dl + hbit > (rhat << 32) + u0 + hbit) { // Unsigned
                                                                                                      // comparison.

                --qhat;
                rhat = rhat + dh;
                if (rhat + hbit >= (1L << 32) + hbit) {
                    break;
                }
            }

            // Multiply and subtract. Unfolded loop.
            long p = qhat * dl;
            t = u0 - (p & mask);
            u0 = t & mask;
            k = (p >>> 32) - (t >> 32);
            p = qhat * dh;
            t = u1 - k - (p & mask);
            u1 = t & mask;
            k = (p >>> 32) - (t >> 32);
            t = u2 - k;
            u2 = t & mask;

            dig[j] = (int) qhat; // Store quotient digit. If we subtracted too much, add back.
            if (t < 0) {
                --dig[j]; // Unfolded loop.
                t = u0 + dl;
                u0 = t & mask;
                t >>>= 32;
                t = u1 + dh + t;
                u1 = t & mask;
                t >>>= 32;
                u2 += t & mask;
            }
        }

        --len;
        dig[len] = 0;
        if (len > 1 && dig[len - 1] == 0) {
            --len;
        }

        final long tmp = u1 << 32 - s | u0 >>> s;
        return s == 0 ? tmp : u2 << 64 - s | tmp;
    }

    /**
     * Modulos this number with an unsigned long. I.e. sets this number to this % mod.
     *
     * @param mod The amount to modulo with (treated as unsigned).
     * @complexity O(n)
     */
    public void urem(final long mod) {
        final long rem = udiv(mod); // todo: opt?
        len = 2;
        dig[0] = (int) rem;
        if (rem == (rem & mask)) {
            --len;
            return;
        } // if(dig[0]==0) sign = 1;
        dig[1] = (int) (rem >>> 32);
    }

    /*** </Unsigned Long Num> ***/

    /*** <Signed Small Num> ***/

    /**
     * Returns the sign of this.
     *
     */
    public int signum() {
        assert sign == -1 || sign == 1;
        return sign;
    }

    /**
     * Adds an int to this number.
     *
     * @param add The amount to add.
     * @complexity O(n)
     */
    public void add(final int add) { // Has not amortized O(1) due to the risk of alternating +1 -1
                                     // on
                                     // continuous sequence of 1-set bits.

        if (add < 0) {
            usub(-add);
        } else {
            uadd(add);
        }
    }

    /**
     * Subtracts an int from this number.
     *
     * @param sub The amount to subtract.
     * @complexity O(n)
     */
    public void sub(final int sub) {
        if (sub < 0) {
            uadd(-sub);
        } else {
            usub(sub);
        }
    }

    /**
     * Multiplies this number with an int.
     *
     * @param mul The amount to multiply with.
     * @complexity O(n)
     */
    public void mul(final int mul) {
        if (isZero()) {
            return; // Remove?
        }
        if (mul < 0) {
            sign = -sign;
            umul(-mul);
        } else {
            umul(mul);
        }
    }

    /**
     * Divides this number with an int.
     *
     * @param div The amount to divide with.
     * @complexity O(n)
     * @return the signed remainder.
     */
    public int div(final int div) {
        if (isZero()) {
            return 0; // Remove?
        }
        if (div < 0) {
            sign = -sign;
            return -sign * udiv(-div);
        }
        return sign * udiv(div);
    }

    // --- Long SubSection ---
    /**
     * Adds a long to this number.
     *
     * @param add The amount to add.
     * @complexity O(n)
     */
    public void add(final long add) {
        if (add < 0) {
            usub(-add);
        } else {
            uadd(add);
        }
    }

    /**
     * Subtracts a long from this number.
     *
     * @param sub The amount to subtract.
     * @complexity O(n)
     */
    public void sub(final long sub) {
        if (sub < 0) {
            uadd(-sub);
        } else {
            usub(sub);
        }
    }

    /**
     * Multiplies this number with a long.
     *
     * @param mul The amount to multiply with.
     * @complexity O(n)
     */
    public void mul(final long mul) {
        if (isZero()) {
            return; // remove?
        }
        if (mul < 0) {
            sign = -sign;
            umul(-mul);
        } else {
            umul(mul);
        }
    }

    /**
     * Divides this number with a {@code long}.
     *
     * @param div The amount to divide with.
     * @complexity O(n)
     * @return the signed remainder.
     */
    public long div(final long div) {
        if (isZero()) {
            return 0; // Remove?
        }
        if (div < 0) {
            sign = -sign;
            return -sign * udiv(-div);
        }
        return sign * udiv(div);
    }

    /*** </Signed Small Num> ***/

    /*** <Big Num Helper> ***/
    /**
     * Increases the magnitude of this number by the given magnitude array.
     *
     * @param fv The magnitude array of the increase.
     * @param fvlen The length (number of digits) of the increase.
     * @complexity O(n)
     */
    private void addMag(final int[] fv, final int fvlen) {
        int[] v = fv;
        int vlen = fvlen;
        int ulen = len;
        int[] u = dig; // ulen <= vlen
        if (vlen < ulen) {
            u = v;
            v = dig;
            ulen = vlen;
            vlen = len;
        }
        if (vlen > dig.length) {
            realloc(vlen + 1);
        }

        long carry = 0;
        int i = 0;
        for (; i < ulen; i++) {
            carry = (u[i] & mask) + (v[i] & mask) + carry;
            dig[i] = (int) carry;
            carry >>>= 32;
        }
        if (vlen > len) {
            System.arraycopy(v, len, dig, len, vlen - len);
            len = vlen;
        }
        if (carry != 0) { // carry==1
            for (; i < len && ++dig[i] == 0; i++) {
            }
            if (i == len) { // vlen==len

                if (len == dig.length) {
                    realloc();
                }
                dig[len++] = 1;
            }
        }
    }

    /**
     * Decreases the magnitude of this number by the given magnitude array. Behaviour is undefined
     * if u > |this|.
     *
     * @param u The magnitude array of the decrease.
     * @param ulen The length (number of digits) of the decrease.
     * @complexity O(n)
     */
    private void subMag(final int[] u, final int ulen) {
        final int[] v = dig; // ulen <= vlen

        // Assumes vlen=len and v=dig
        long dif = 0;
        int i = 0;
        for (; i < ulen; i++) {
            dif = (v[i] & mask) - (u[i] & mask) + dif;
            dig[i] = (int) dif;
            dif >>= 32;
        }
        if (dif != 0) {
            for (; dig[i] == 0; i++) {
                --dig[i];
            }
            if (--dig[i] == 0 && i + 1 == len) {
                len -= 1; // TODO: Submit Huldra issue: 1<<64 - 1 1<<96 -1
            }
        }
        while (len > 1 && dig[len - 1] == 0) {
            --len;
        }
    }

    /*** </Big Num Helper> ***/

    /*** <Big Num> ***/
    /**
     * Adds a BigInt to this number.
     *
     * @param a The number to add.
     * @complexity O(n)
     */
    public void add(final BigInt a) {
        if (sign == a.sign) {
            addMag(a.dig, a.len);
            return;
        }
        if (compareAbsTo(a) >= 0) {
            subMag(a.dig, a.len);
            // if(len==1 && dig[0]==0) sign = 1;
            return;
        }

        final int[] v = a.dig;
        final int vlen = a.len;
        if (dig.length < vlen) {
            realloc(vlen + 1);
        }

        sign = -sign;
        long dif = 0;
        int i = 0;
        for (; i < len; i++) {
            dif = (v[i] & mask) - (dig[i] & mask) + dif;
            dig[i] = (int) dif;
            dif >>= 32;
        }
        if (vlen > len) {
            System.arraycopy(v, len, dig, len, vlen - len);
            len = vlen;
        }
        if (dif != 0) {
            for (; i < vlen && dig[i] == 0; i++) {
                --dig[i];
            }
            if (--dig[i] == 0 && i + 1 == len) {
                --len;
            }
        }
        // if(i==vlen) should be impossible
    }

    /**
     * Subtracts a BigInt from this number.
     *
     * @param a The number to subtract.
     * @complexity O(n)
     */
    public void sub(final BigInt a) { // Fix naming.

        if (sign != a.sign) {
            addMag(a.dig, a.len);
            return;
        }
        if (compareAbsTo(a) >= 0) {
            subMag(a.dig, a.len);
            // if(len==1 && dig[0]==0) sign = 1;
            return;
        }

        final int[] v = a.dig;
        final int vlen = a.len;
        if (dig.length < vlen) {
            realloc(vlen + 1);
        }

        sign = -sign;
        long dif = 0;
        int i = 0;
        for (; i < len; i++) {
            dif = (v[i] & mask) - (dig[i] & mask) + dif;
            dig[i] = (int) dif;
            dif >>= 32;
        }
        if (vlen > len) {
            System.arraycopy(v, len, dig, len, vlen - len);
            len = vlen;
        }
        if (dif != 0) {
            for (; i < vlen && dig[i] == 0; i++) {
                --dig[i];
            }
            if (--dig[i] == 0 && i + 1 == len) {
                --len;
            }
        }
        // if(i==vlen) should be impossible
    }

    // --- Multiplication SubSection ---
    /**
     * Multiplies this number by the given BigInt. Chooses the appropriate algorithm with regards to
     * the size of the numbers.
     *
     * @param mul The number to multiply with.
     * @complexity O(n^2) - O(n log n)
     */
    public void mul(final BigInt mul) {
        if (isZero()) {
            return;
        } else if (mul.isZero()) {
            setToZero();
        } else if (mul.len <= 2 || len <= 2) {
            sign *= mul.sign;
            if (mul.len == 1) {
                umul(mul.dig[0]);
            } else if (len == 1) {
                final int tmp = dig[0];
                assign(mul.dig, mul.len);
                umul(tmp);
            } else if (mul.len == 2) {
                umul((long) mul.dig[1] << 32 | mul.dig[0] & mask);
            } else {
                final long tmp = (long) dig[1] << 32 | dig[0] & mask;
                assign(mul.dig, mul.len);
                umul(tmp);
            }
        } else if (len < 128 || mul.len < 128 || (long) len * mul.len < 1_000_000) {
            smallMul(mul); // Remove overhead?
        } else if (Math.max(len, mul.len) < 20000) {
            karatsuba(mul, false); // Tune thresholds and remove hardcode.
        } else {
            karatsuba(mul, true);
        }
    }

    /**
     * Multiplies this number by the given (suitably small) BigInt. Uses a quadratic algorithm which
     * is often suitable for smaller numbers.
     *
     * @param mul The number to multiply with.
     * @complexity O(n^2)
     */
    public void smallMul(final BigInt mul) {
        if (isZero()) {
            return; // Remove?
        }
        if (mul.isZero()) {
            setToZero();
            return;
        }

        sign *= mul.sign;

        int ulen = len, vlen = mul.len;
        int[] u = dig, v = mul.dig; // ulen <= vlen
        if (vlen < ulen) {
            u = v;
            v = dig;
            ulen = vlen;
            vlen = len;
        }

        final int[] res = naiveMul(u, ulen, v, vlen); // Todo remove function overhead.

        dig = res;
        len = res.length;
        if (res[len - 1] == 0) {
            --len;
        }
    }

    /**
     * Multiplies this number by the given BigInt using the Karatsuba algorithm.
     *
     * @param mul The number to multiply with.
     * @throws Exception
     * @complexity O(n^1.585)
     */
    public void karatsuba(final BigInt mul) throws Exception { // Fix naming?

        karatsuba(mul, false);
    }

    /**
     * Multiplies this number by the given BigInt using the Karatsuba algorithm. The caller can
     * choose to use a parallel version which is more suitable for larger numbers.
     *
     * @param mul The number to multiply with.
     * @param parallel true if we should try to parallelize the algorithm, false if we shouldn't.
     * @complexity O(n^1.585)
     */
    public void karatsuba(final BigInt mul, final boolean parallel) { // Not fully
                                                                      // tested on
                                                                      // small
        // numbers... Fix naming?

        if (mul.dig.length < len) {
            mul.realloc(len);
        } else if (dig.length < mul.len) {
            realloc(mul.len);
        }

        if (mul.len < len) {
            for (int i = mul.len; i < len; i++) {
                mul.dig[i] = 0;
            }
        }
        if (len < mul.len) {
            for (int i = len; i < mul.len; i++) {
                dig[i] = 0;
            }
        }

        final int mlen = Math.max(len, mul.len);
        int[] res = null;
        if (!parallel) {
            res = kmul(dig, mul.dig, 0, mlen);
        } else {
            final ExecutorService pool = Executors.newFixedThreadPool(12);
            try {
                res = pmul(dig, mul.dig, 0, mlen, 1, pool);
            } catch (final Exception e) {
                assert false; // This should never happen
            }
            pool.shutdown();
        }

        len = len + mul.len;
        while (res[len - 1] == 0) {
            --len;
        }
        dig = res;
        sign *= mul.sign;
    }

    /*** <Mul Helper> ***/
    /**
     * Multiplies two magnitude arrays and returns the result.
     *
     * @param u The first magnitude array.
     * @param ulen The length of the first array.
     * @param v The second magnitude array.
     * @param vlen The length of the second array.
     * @return A ulen+vlen length array containing the result.
     * @complexity O(n^2)
     */
    private static int[] naiveMul(final int[] u, final int ulen, final int[] v, final int vlen) {
        final int[] res = new int[ulen + vlen];
        long carry = 0, tmp, ui = u[0] & mask;
        for (int j = 0; j < vlen; j++) {
            tmp = ui * (v[j] & mask) + carry;
            res[j] = (int) tmp;
            carry = tmp >>> 32;
        }
        res[vlen] = (int) carry;
        for (int i = 1; i < ulen; i++) {
            ui = u[i] & mask;
            carry = 0;
            for (int j = 0; j < vlen; j++) {
                tmp = ui * (v[j] & mask) + (res[i + j] & mask) + carry;
                res[i + j] = (int) tmp;
                carry = tmp >>> 32;
            }
            res[i + vlen] = (int) carry;
        }
        return res;
    }

    /**
     * Multiplies partial magnitude arrays x[off..off+n) and y[off...off+n) and returns the result.
     * Algorithm: Karatsuba
     *
     * @param x The first magnitude array.
     * @param y The second magnitude array.
     * @param off The offset, where the first element is residing.
     * @param n The length of each of the two partial arrays.
     * @complexity O(n^1.585)
     */
    private static int[] kmul(final int[] x, final int[] y, final int off, final int n) {
        // x = x1*B^m + x0
        // y = y1*B^m + y0
        // xy = z2*B^2m + z1*B^m + z0
        // z2 = x1*y1, z0 = x0*y0, z1 = (x1+x0)(y1+y0)-z2-z0
        if (n <= 32) { // Basecase

            final int[] z = new int[2 * n];
            long carry = 0, tmp, xi = x[off] & mask;
            for (int j = 0; j < n; j++) {
                tmp = xi * (y[off + j] & mask) + carry;
                z[j] = (int) tmp;
                carry = tmp >>> 32;
            }
            z[n] = (int) carry;
            for (int i = 1; i < n; i++) {
                xi = x[off + i] & mask;
                carry = 0;
                for (int j = 0; j < n; j++) {
                    tmp = xi * (y[off + j] & mask) + (z[i + j] & mask) + carry;
                    z[i + j] = (int) tmp;
                    carry = tmp >>> 32;
                }
                z[i + n] = (int) carry;
            }
            return z;
        }

        final int b = n >>> 1;
        final int[] z2 = kmul(x, y, off + b, n - b);
        final int[] z0 = kmul(x, y, off, b);

        final int[] x2 = new int[n - b + 1], y2 = new int[n - b + 1];
        long carry = 0;
        for (int i = 0; i < b; i++) {
            carry = (x[off + b + i] & mask) + (x[off + i] & mask) + carry;
            x2[i] = (int) carry;
            carry >>>= 32;
        }
        if ((n & 1) != 0) {
            x2[b] = x[off + b + b];
        }
        if (carry != 0) {
            if (++x2[b] == 0) {
                ++x2[b + 1];
            }
        }
        carry = 0;
        for (int i = 0; i < b; i++) {
            carry = (y[off + b + i] & mask) + (y[off + i] & mask) + carry;
            y2[i] = (int) carry;
            carry >>>= 32;
        }
        if ((n & 1) != 0) {
            y2[b] = y[off + b + b];
        }
        if (carry != 0) {
            if (++y2[b] == 0) {
                ++y2[b + 1];
            }
        }

        final int[] z1 = kmul(x2, y2, 0, n - b + (x2[n - b] != 0 || y2[n - b] != 0 ? 1 : 0));

        final int[] z = new int[2 * n];
        System.arraycopy(z0, 0, z, 0, 2 * b); // Add z0
        System.arraycopy(z2, 0, z, b + b, 2 * (n - b)); // Add z2

        // Add z1
        carry = 0;
        int i = 0;
        for (; i < 2 * b; i++) {
            carry = (z[i + b] & mask) + (z1[i] & mask) - (z2[i] & mask) - (z0[i] & mask) + carry;
            z[i + b] = (int) carry;
            carry >>= 32;
        }
        for (; i < 2 * (n - b); i++) {
            carry = (z[i + b] & mask) + (z1[i] & mask) - (z2[i] & mask) + carry;
            z[i + b] = (int) carry;
            carry >>= 32;
        }
        for (; i < z1.length; i++) {
            carry = (z[i + b] & mask) + (z1[i] & mask) + carry;
            z[i + b] = (int) carry;
            carry >>= 32;
        }
        if (carry != 0) {
            while (++z[i + b] == 0) {
                ++i;
            }
        }

        return z;
    }

    /**
     * Multiplies partial magnitude arrays x[off..off+n) and y[off...off+n) and returns the result.
     * Algorithm: Parallell Karatsuba
     *
     * @param x The first magnitude array.
     * @param y The second magnitude array.
     * @param off The offset, where the first element is residing.
     * @param n The length of each of the two partial arrays.
     * @param lim The recursion depth up until which we will spawn new threads.
     * @param pool Where spawn threads will be added and executed.
     * @throws Exception - Various thread related exceptions.
     * @complexity O(n^1.585)
     */
    private static int[] pmul(final int[] x, final int[] y, final int off, final int n, final int lim, final ExecutorService pool) throws Exception {
        final int b = n >>> 1;

        final Future<int[]> left = pool.submit(new Callable<int[]>() {
            @Override
            public int[] call() throws Exception {
                return lim == 0 ? kmul(x, y, off, b) : pmul(x, y, off, b, lim - 1, pool);
            }
        });

        final Future<int[]> right = pool.submit(new Callable<int[]>() {
            @Override
            public int[] call() throws Exception {
                return lim == 0 ? kmul(x, y, off + b, n - b) : pmul(x, y, off + b, n - b, lim - 1, pool);
            }
        });

        final int[] x2 = new int[n - b + 1], y2 = new int[n - b + 1];
        long carry = 0;
        for (int i = 0; i < b; i++) {
            carry = (x[off + b + i] & mask) + (x[off + i] & mask) + carry;
            x2[i] = (int) carry;
            carry >>>= 32;
        }
        if ((n & 1) != 0) {
            x2[b] = x[off + b + b];
        }
        if (carry != 0) {
            if (++x2[b] == 0) {
                ++x2[b + 1];
            }
        }
        carry = 0;
        for (int i = 0; i < b; i++) {
            carry = (y[off + b + i] & mask) + (y[off + i] & mask) + carry;
            y2[i] = (int) carry;
            carry >>>= 32;
        }
        if ((n & 1) != 0) {
            y2[b] = y[off + b + b];
        }
        if (carry != 0) {
            if (++y2[b] == 0) {
                ++y2[b + 1];
            }
        }

        final Future<int[]> mid = pool.submit(new Callable<int[]>() {
            @Override
            public int[] call() throws Exception {
                return lim == 0 ? kmul(x2, y2, 0, n - b + (x2[n - b] != 0 || y2[n - b] != 0 ? 1 : 0)) : pmul(x2, y2, 0, n - b + (x2[n - b] != 0 || y2[n - b] != 0 ? 1 : 0), lim - 1, pool);
            }
        });

        final int[] z = new int[2 * n];

        final int[] z0 = left.get();
        System.arraycopy(z0, 0, z, 0, 2 * b);
        final int[] z2 = right.get();
        System.arraycopy(z2, 0, z, b + b, 2 * (n - b));

        final int[] z1 = mid.get();

        carry = 0;
        int i = 0;
        for (; i < 2 * b; i++) {
            carry = (z[i + b] & mask) + (z1[i] & mask) - (z2[i] & mask) - (z0[i] & mask) + carry;
            z[i + b] = (int) carry;
            carry >>= 32;
        }
        for (; i < 2 * (n - b); i++) {
            carry = (z[i + b] & mask) + (z1[i] & mask) - (z2[i] & mask) + carry;
            z[i + b] = (int) carry;
            carry >>= 32;
        }
        for (; i < z1.length; i++) {
            carry = (z[i + b] & mask) + (z1[i] & mask) + carry;
            z[i + b] = (int) carry;
            carry >>= 32;
        }
        if (carry != 0) {
            while (++z[i + b] == 0) {
                ++i;
            }
        }
        return z;
    }
    /*** </Mul Helper> ***/

    // --- Division and Remainder SubSection ---
    /**
     * Divides this number by the given BigInt. Division by zero is undefined.
     *
     * @param div The number to divide with.
     * @complexity O(n^2)
     */
    public void div(final BigInt div) {
        if (div.len == 1) {
            sign *= div.sign;
            udiv(div.dig[0]);
            return;
        }

        final int tmp = compareAbsTo(div);
        if (tmp < 0) {
            setToZero();
            return;
        }
        if (tmp == 0) {
            // TODO: Submit Hulra issue: 987654321098765432109876 / -987654321098765432109876
            uassign(sign * div.sign, 1);
            return;
        }

        final int[] q = new int[len - div.len + 1];
        if (len == dig.length) {
            realloc(len + 1); // We need an extra slot.
        }
        div(dig, div.dig, len, div.len, q);

        dig = q;
        for (len = q.length; len > 1 && dig[len - 1] == 0; --len) {
        }
        sign *= div.sign;
    }

    /**
     * Sets this number to the remainder r satisfying q*div + r = this, where q = floor(this/div).
     *
     * @param div The number to use in the division causing the remainder.
     * @complexity O(n^2)
     */
    public void rem(final BigInt div) {
        // -7/-3 = 2, 2*-3 + -1
        // -7/3 = -2, -2*3 + -1
        // 7/-3 = -2, -2*-3 + 1
        // 7/3 = 2, 2*3 + 1
        if (div.len == 1) {
            urem(div.dig[0]);
            return;
        }

        final int tmp = compareAbsTo(div);
        if (tmp < 0) {
            return;
        }
        if (tmp == 0) {
            setToZero();
            return;
        }

        final int[] q = new int[len - div.len + 1];
        if (len == dig.length) {
            realloc(len + 1); // We need an extra slot.
        }
        div(dig, div.dig, len, div.len, q);

        for (len = div.len; len > 1 && dig[len - 1] == 0; --len) {
        } // TODO: Submit Huldra issue for edge case i%j==0
    }

    /**
     * Divides this number by the given BigInt and returns the remainder. Division by zero is
     * undefined.
     *
     * @param div The number to divide with.
     * @return The remainder.
     * @complexity O(n^2)
     */
    public BigInt divRem(final BigInt div) {
        int tmp = sign;
        if (div.len == 1) {
            sign *= div.sign;
            return new BigInt(tmp, udiv(div.dig[0]));
        }

        tmp = compareAbsTo(div);
        if (tmp < 0) {
            final BigInt cpy = new BigInt(sign, dig, len);
            dig = new int[2];
            len = 1; // setToZero()
            return cpy;
        }
        if (tmp == 0) {
            uassign(1, sign *= div.sign);
            return new BigInt(1, 0);
        }

        final int[] q = new int[len - div.len + 1];
        if (len == dig.length) {
            realloc(len + 1); // We need an extra slot.
        }
        div(dig, div.dig, len, div.len, q);

        final int[] r = dig;
        dig = q;
        for (len = q.length; len > 1 && dig[len - 1] == 0; --len) {
        }

        tmp = div.len;
        while (tmp > 1 && r[tmp - 1] == 0) {
            --tmp;
        }
        sign *= div.sign;
        return new BigInt(sign / div.sign, r, tmp);
    }

    /**
     * Divides the first magnitude u[0..m) by v[0..n) and stores the resulting quotient in q. The
     * remainder will be stored in u, so u will be destroyed. u[] must have room for an additional
     * element, i.e. u[m] is a legal access.
     *
     * @param u The first magnitude array, the dividend.
     * @param v The second magnitude array, the divisor.
     * @param m The length of the first array.
     * @param n The length of the second array.
     * @param q An array of length at least n-m+1 where the quotient will be stored.
     * @complexity O(m*n)
     */
    private static void div(final int[] u, final int[] v, final int m, final int n, final int[] q) { // Hacker'sDelight's
        // implementation
        // of
        // Knuth's
        // Algorithm
        // D
        final long b = 1L << 32;    // Number base (32 bits).
        long qhat;               // Estimated quotient digit.
        long rhat;               // A remainder.
        long p;                // Product of two digits.

        final int s;
        int i, j;
        long t, k;

        // Normalize by shifting v left just enough so that
        // its high-order bit is on, and shift u left the
        // same amount. We may have to append a high-order
        // digit on the dividend; we do that unconditionally.

        s = Integer.numberOfLeadingZeros(v[n - 1]);
        if (s > 0) { // In Java (x<<32)==(x<<0) so...

            for (i = n - 1; i > 0; i--) {
                v[i] = v[i] << s | v[i - 1] >>> 32 - s;
            }
            v[0] = v[0] << s;

            u[m] = u[m - 1] >>> 32 - s;
            for (i = m - 1; i > 0; i--) {
                u[i] = u[i] << s | u[i - 1] >>> 32 - s;
            }
            u[0] = u[0] << s;
        }

        final long dh = v[n - 1] & mask, dl = v[n - 2] & mask, hbit = Long.MIN_VALUE;

        for (j = m - n; j >= 0; j--) { // Main loop

            // Compute estimate qhat of q[j].
            k = u[j + n] * b + (u[j + n - 1] & mask); // Unsigned division is a pain in the ass! ='(
            qhat = (k >>> 1) / dh << 1;
            t = k - qhat * dh;
            if (t + hbit >= dh + hbit) {
                qhat++; // qhat = (u[j+n]*b + u[j+n-1])/v[n-1];
            }
            rhat = k - qhat * dh;

            while (qhat + hbit >= b + hbit || qhat * dl + hbit > b * rhat + (u[j + n - 2] & mask) + hbit) { // Unsigned
                                                                                                            // comparison.

                qhat = qhat - 1;
                rhat = rhat + dh;
                if (rhat + hbit >= b + hbit) {
                    break;
                }
            }

            // Multiply and subtract.
            k = 0;
            for (i = 0; i < n; i++) {
                p = qhat * (v[i] & mask);
                t = (u[i + j] & mask) - k - (p & mask);
                u[i + j] = (int) t;
                k = (p >>> 32) - (t >> 32);
            }
            t = (u[j + n] & mask) - k;
            u[j + n] = (int) t;

            q[j] = (int) qhat;              // Store quotient digit. If we subtracted too much, add
                                            // back.
            if (t < 0) {
                q[j] = q[j] - 1;
                k = 0;
                for (i = 0; i < n; i++) {
                    t = (u[i + j] & mask) + (v[i] & mask) + k;
                    u[i + j] = (int) t;
                    k = t >>> 32; // >>
                }
                u[j + n] += (int) k;
            }
        }

        if (s > 0) {
            // Unnormalize v[].
            for (i = 0; i < n - 1; i++) {
                v[i] = v[i] >>> s | v[i + 1] << 32 - s;
            }
            v[n - 1] >>>= s;

            // Unnormalize u[].
            for (i = 0; i < m; i++) {
                u[i] = u[i] >>> s | u[i + 1] << 32 - s;
            }
            u[m] >>>= s;
        }
    }

    /*** </Big Num> ***/

    /*** <Output> ***/
    /**
     * Converts this number into a string of radix 10.
     *
     * @return The string representation of this number in decimal.
     * @complexity O(n^2)
     */
    @Override
    public String toString() {
        if (isZero()) {
            return "0";
        }

        int top = len * 10 + 1;
        final char[] buf = new char[top];
        Arrays.fill(buf, '0');
        final int[] cpy = Arrays.copyOf(dig, len);
        while (true) {
            final int j = top;
            for (long tmp = toStringDiv(); tmp > 0; tmp /= 10) {
                buf[--top] += tmp % 10; // TODO: Optimize.
            }
            if (len == 1 && dig[0] == 0) {
                break;
            } else {
                top = j - 13;
            }
        }
        if (sign < 0) {
            buf[--top] = '-';
        }
        System.arraycopy(cpy, 0, dig, 0, len = cpy.length);
        return new String(buf, top, buf.length - top);
    }

    // Divides the number by 10^13 and returns the remainder.
    // Does not change the sign of the number.
    private long toStringDiv() {
        final int pow5 = 1_220_703_125, pow2 = 1 << 13;
        int nextq = 0;
        long rem = 0;
        for (int i = len - 1; i > 0; i--) {
            rem = (rem << 32) + (dig[i] & mask);
            final int q = (int) (rem / pow5);
            rem = rem % pow5;
            dig[i] = nextq | q >>> 13;
            nextq = q << 32 - 13;
        }
        rem = (rem << 32) + (dig[0] & mask);
        final int mod2 = dig[0] & pow2 - 1;
        dig[0] = nextq | (int) (rem / pow5 >>> 13);
        rem = rem % pow5;
        // Applies the Chinese Remainder Theorem.
        // -67*5^13 + 9983778*2^13 = 1
        final long pow10 = (long) pow5 * pow2;
        rem = (rem - pow5 * (mod2 - rem) % pow10 * 67) % pow10;
        if (rem < 0) {
            rem += pow10;
        }
        if (dig[len - 1] == 0 && len > 1) {
            if (dig[--len - 1] == 0 && len > 1) {
                --len;
            }
        }
        return rem;
    }
    /*** </Output> ***/

    /*** <BitOperations> ***/
    // Negative numbers are imagined in their two's complement form with infinite sign extension.
    // This has no effect on bit shifts, but makes implementaion of other bit operations a bit
    // tricky if one wants them to be as efficient as possible.

    /**
     * Shifts this number left by the given amount (less than 32) starting at the given digit, i.e.
     * the first (<len) digits are left untouched.
     *
     * @param shift The amount to shift.
     * @param first The digit to start shifting from.
     * @complexity O(n)
     */
    private void smallShiftLeft(final int shift, final int first) {
        int[] res = dig;
        if (dig[len - 1] << shift >>> shift != dig[len - 1]) {
            if (++len > dig.length) {
                res = new int[len + 1]; // realloc(len+1);
            } else {
                dig[len - 1] = 0;
            }
        }

        int nxt = len > dig.length ? 0 : dig[len - 1];
        for (int i = len - 1; i > first; i--) {
            res[i] = nxt << shift | (nxt = dig[i - 1]) >>> 32 - shift;
        }
        res[first] = nxt << shift;
        dig = res;
    }

    /**
     * Shifts this number right by the given amount (less than 32).
     *
     * @param shift The amount to shift.
     * @complexity O(n)
     */
    private void smallShiftRight(final int shift) {
        int nxt = dig[0];
        for (int i = 0; i < len - 1; i++) {
            dig[i] = nxt >>> shift | (nxt = dig[i + 1]) << 32 - shift;
        }
        if ((dig[len - 1] >>>= shift) == 0 && len > 1) {
            --len;
        }
    }

    /**
     * Shifts this number left by 32*shift, i.e. moves each digit shift positions to the left.
     *
     * @param shift The number of positions to move each digit.
     * @complexity O(n)
     */
    private void bigShiftLeft(final int shift) {
        if (len + shift > dig.length) {
            final int[] res = new int[len + shift + 1];
            System.arraycopy(dig, 0, res, shift, len);
            dig = res;
        } else {
            System.arraycopy(dig, 0, dig, shift, len);
            for (int i = 0; i < shift; i++) {
                dig[i] = 0;
            }
        }
        len += shift;
    }

    /**
     * Shifts this number right by 32*shift, i.e. moves each digit shift positions to the right.
     *
     * @param shift The number of positions to move each digit.
     * @complexity O(n)
     */
    private void bigShiftRight(final int shift) {
        // TODO: Submit Huldra issue: 52339921626855613784307183 << 238 (shift > len)
        if (shift < len) {
            System.arraycopy(dig, shift, dig, 0, len - shift);
            // for(int i = len-shift; i<len; i++) dig[i] = 0; dig[j >= len] are allowed to be
            // anything.
            len -= shift;
        } else {
            for (int i = 0; i < len; i++) {
                dig[i] = 0;
                len = 1;
            }
        }
    }

    /**
     * Shifts this number left by the given amount.
     *
     * @param shift The amount to shift.
     * @complexity O(n)
     */
    public void shiftLeft(final int shift) {
        final int bigShift = shift >>> 5, smallShift = shift & 31;
        if (bigShift > 0) {
            bigShiftLeft(bigShift);
        }
        if (smallShift > 0) {
            smallShiftLeft(smallShift, bigShift);
        }
    }

    /**
     * Shifts this number right by the given amount.
     *
     * @param shift The amount to shift.
     * @complexity O(n)
     */
    public void shiftRight(final int shift) {
        final int bigShift = shift >>> 5, smallShift = shift & 31;
        if (bigShift > 0) {
            bigShiftRight(bigShift);
        }
        if (smallShift > 0) {
            smallShiftRight(smallShift);
        }
    }

    /**
     * Tests if the given bit in the number is set.
     *
     * @param bit The index of the bit to test.
     * @return true if the given bit is one.
     * @complexity O(n)
     */
    public boolean testBit(final int bit) {
        final int bigBit = bit >>> 5, smallBit = bit & 31;
        if (bigBit >= len) {
            return sign < 0;
        }
        if (sign > 0) {
            return (dig[bigBit] & 1 << smallBit) != 0;
        }
        int j = 0;
        for (; j <= bigBit && dig[j] == 0;) {
            ++j;
        }
        if (j > bigBit) {
            return false;
        }
        if (j < bigBit) {
            return (dig[bigBit] & 1 << smallBit) == 0;
        }
        j = -dig[bigBit];
        return (j & 1 << smallBit) != 0;
    }

    /**
     * Sets the given bit in the number.
     *
     * @param bit The index of the bit to set.
     * @complexity O(n)
     */
    public void setBit(final int bit) {
        final int bigBit = bit >>> 5, smallBit = bit & 31;
        if (sign > 0) {
            if (bigBit >= dig.length) {
                realloc(bigBit + 1);
                len = bigBit + 1;
            } else if (bigBit >= len) {
                for (; len <= bigBit; len++) {
                    dig[len] = 0;
                    // len = bigBit+1;
                }
            }
            dig[bigBit] |= 1 << smallBit;
        } else {
            if (bigBit >= len) {
                return;
            }
            int j = 0;
            for (; j <= bigBit && dig[j] == 0;) {
                ++j;
            }
            if (j > bigBit) {
                dig[bigBit] = -1 << smallBit;
                for (; dig[j] == 0; j++) {
                    dig[j] = -1;
                }
                dig[j] = ~-dig[j];
                if (j == len - 1 && dig[len - 1] == 0) {
                    --len;
                }
                return;
            }
            if (j < bigBit) {
                dig[bigBit] &= ~(1 << smallBit);
                while (dig[len - 1] == 0) {
                    --len;
                }
                return;
            }
            j = Integer.lowestOneBit(dig[j]); // more efficient than numberOfTrailingZeros
            final int k = 1 << smallBit;
            if (k - j > 0) {
                dig[bigBit] &= ~k; // Unsigned compare.
            } else {
                dig[bigBit] ^= (j << 1) - 1 ^ k - 1;
                dig[bigBit] |= k;
            }
        }
    }

    /**
     * Clears the given bit in the number.
     *
     * @param bit The index of the bit to clear.
     * @complexity O(n)
     */
    public void clearBit(final int bit) {
        final int bigBit = bit >>> 5, smallBit = bit & 31;
        if (sign > 0) {
            if (bigBit < len) {
                dig[bigBit] &= ~(1 << smallBit);
                while (dig[len - 1] == 0 && len > 1) {
                    --len;
                }
            }
        } else {
            if (bigBit >= dig.length) {
                realloc(bigBit + 1);
                len = bigBit + 1;
                dig[bigBit] |= 1 << smallBit;
                return;
            } else if (bigBit >= len) {
                for (; len <= bigBit; len++) {
                    dig[len] = 0;
                }
                dig[bigBit] |= 1 << smallBit;
                return;
            }
            int j = 0;
            for (; j <= bigBit && dig[j] == 0;) {
                ++j;
            }
            if (j > bigBit) {
                return;
            }
            if (j < bigBit) {
                dig[bigBit] |= 1 << smallBit;
                return;
            }
            j = Integer.lowestOneBit(dig[j]); // more efficient than numberOfTrailingZeros
            final int k = 1 << smallBit;
            if (j - k > 0) {
                return; // Unsigned compare
            }
            if (j - k < 0) {
                dig[bigBit] |= k;
                return;
            }
            j = dig[bigBit];
            if (j == (-1 ^ k - 1)) {
                dig[bigBit] = 0;
                for (j = bigBit + 1; j < len && dig[j] == -1; j++) {
                    dig[j] = 0;
                }
                if (j == dig.length) {
                    realloc(j + 2);
                }
                if (j == len) {
                    dig[len++] = 1;
                    return;
                }
                dig[j] = -~dig[j];
            } else {
                j = Integer.lowestOneBit(j ^ -1 ^ k - 1);
                dig[bigBit] ^= j | j - 1 ^ k - 1;
            }
        }
    }

    /**
     * Flips the given bit in the number.
     *
     * @param bit The index of the bit to flip.
     * @complexity O(n)
     */
    public void flipBit(final int bit) {
        final int bigBit = bit >>> 5, smallBit = bit & 31;
        block: if (bigBit >= dig.length) {
            realloc(bigBit + 1);
            len = bigBit + 1;
            dig[bigBit] ^= 1 << smallBit;
        } else if (bigBit >= len) {
            for (; len <= bigBit; len++) {
                dig[len] = 0;
            }
            dig[bigBit] ^= 1 << smallBit;
        } else if (sign > 0) {
            dig[bigBit] ^= 1 << smallBit;
        } else {
            int j = 0;
            for (; j <= bigBit && dig[j] == 0;) {
                ++j;
            }
            if (j < bigBit) {
                dig[bigBit] ^= 1 << smallBit;
                break block;
            }
            if (j > bigBit) { // TODO: Refactor with setBit?

                dig[bigBit] = -1 << smallBit;
                for (; dig[j] == 0; j++) {
                    dig[j] = -1;
                }
                dig[j] = ~-dig[j];
                if (j == len - 1 && dig[len - 1] == 0) {
                    --len;
                }
                return;
            }
            j = Integer.lowestOneBit(dig[j]); // more efficient than numberOfTrailingZeros
            final int k = 1 << smallBit;
            if (j - k > 0) {
                dig[bigBit] ^= (j << 1) - 1 ^ k - 1;
                return;
            }
            if (j - k < 0) {
                dig[bigBit] ^= k;
                return;
            }
            j = dig[bigBit];
            if (j == (-1 ^ k - 1)) { // TODO: Refactor with clearBit?

                dig[bigBit] = 0;
                for (j = bigBit + 1; j < len && dig[j] == -1; j++) {
                    dig[j] = 0;
                }
                if (j == dig.length) {
                    realloc(j + 2);
                }
                if (j == len) {
                    dig[len++] = 1;
                    return;
                }
                dig[j] = -~dig[j];
            } else {
                j = Integer.lowestOneBit(j ^ -1 ^ k - 1);
                dig[bigBit] ^= j | j - 1 ^ k - 1;
            }
        }
        while (dig[len - 1] == 0 && len > 1) {
            --len;
        }
    }

    /**
     * Bitwise-ands this number with the given number, i.e. this &= mask.
     *
     * @param fmask The number to bitwise-and with.
     * @complexity O(n)
     */
    public void and(final BigInt fmask) {
        if (sign > 0) {
            if (fmask.sign > 0) {
                if (fmask.len < len) {
                    len = fmask.len;
                }
                for (int i = 0; i < len; i++) {
                    dig[i] &= fmask.dig[i];
                }
            } else {
                final int mlen = Math.min(len, fmask.len);
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0 && j < mlen; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    for (dig[j - 1] = 0; j < mlen && fmask.dig[j] == 0; j++) {
                        dig[j] = 0;
                    }
                    if (j < mlen) {
                        dig[j] &= -fmask.dig[j];
                    } else if (j == len) {
                        len = 1;
                    }
                    ++j;
                } else if (a == 0) { // && (b!=0 || j==mlen)

                    while (j < mlen && dig[j] == 0) {
                        j++;
                    }
                } else {
                    dig[j - 1] &= -b;
                }
                for (; j < mlen; j++) {
                    dig[j] &= ~fmask.dig[j];
                }
            }
            while (dig[len - 1] == 0 && len > 1) {
                --len;
            }
        } else {
            final int mlen = Math.min(len, fmask.len);
            if (fmask.sign > 0) {
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0 && j < mlen; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    for (dig[j - 1] = 0; j < mlen && fmask.dig[j] == 0; j++) {
                        dig[j] = 0;
                    }
                } else if (a == 0) { // && (b!=0 || j==mlen)

                    while (j < mlen && dig[j] == 0) {
                        j++;
                    }
                    if (j < mlen) {
                        dig[j] = -dig[j] & fmask.dig[j];
                    }
                    ++j;
                } else {
                    dig[j - 1] = -a & b;
                }
                for (; j < mlen; j++) {
                    dig[j] = ~dig[j] & fmask.dig[j];
                }
                if (fmask.len > len) {
                    if (fmask.len > dig.length) {
                        realloc(fmask.len + 2);
                    }
                    System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
                }
                len = fmask.len;
                sign = 1;
                while (dig[len - 1] == 0 && len > 1) {
                    --len;
                }
            } else {
                if (fmask.len > len) {
                    if (fmask.len > dig.length) {
                        realloc(fmask.len + 2);
                    }
                    System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
                }
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    for (dig[j - 1] = 0; j < mlen && fmask.dig[j] == 0; j++) {
                        dig[j] = 0;
                    }
                    if (j < mlen) {
                        dig[j] = -(~dig[j] & -fmask.dig[j]);
                    }
                    ++j;
                } else if (a == 0) { // && (b!=0 || j==mlen)

                    while (j < mlen && dig[j] == 0) {
                        j++;
                    }
                    if (j < mlen) {
                        dig[j] = -(-dig[j] & ~fmask.dig[j]);
                    }
                    ++j;
                } else {
                    dig[j - 1] = -(-a & -b);
                }
                if (j <= mlen && dig[j - 1] == 0) {
                    if (j < mlen) {
                        for (dig[j] = -~(dig[j] | fmask.dig[j]); ++j < mlen && dig[j - 1] == 0;) {
                            dig[j] = -~(dig[j] | fmask.dig[j]);  // -(~dig[j]&~mask.dig[j])
                        }
                    }
                    if (j == mlen && dig[j - 1] == 0) {
                        final int blen = Math.max(len, fmask.len);
                        while (j < blen && dig[j] == -1) {
                            dig[j++] = 0; // mask.dig[j]==dig[j]
                        }
                        if (j < blen) {
                            dig[j] = -~dig[j];
                        } else {
                            if (blen >= dig.length) {
                                realloc(blen + 2);
                            }
                            dig[blen] = 1;
                            len = blen + 1;
                            return;
                        }
                        ++j;
                    }
                }
                for (; j < mlen; j++) {
                    dig[j] |= fmask.dig[j]; // ~(~dig[j]&~mask.dig[j]);
                }
                if (fmask.len > len) {
                    len = fmask.len;
                }
            }
        }
    }

    /**
     * Bitwise-ors this number with the given number, i.e. this |= mask.
     *
     * @param fmask The number to bitwise-or with.
     * @complexity O(n)
     */
    public void or(final BigInt fmask) {
        if (sign > 0) {
            if (fmask.sign > 0) {
                if (fmask.len > len) {
                    if (fmask.len > dig.length) {
                        realloc(fmask.len + 1);
                    }
                    System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
                    for (int i = 0; i < len; i++) {
                        dig[i] |= fmask.dig[i];
                    }
                    len = fmask.len;
                } else {
                    for (int i = 0; i < fmask.len; i++) {
                        dig[i] |= fmask.dig[i];
                    }
                }
            } else {
                if (fmask.len > dig.length) {
                    realloc(fmask.len + 1);
                }
                if (fmask.len > len) {
                    System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
                }
                final int mLen = Math.min(fmask.len, len);
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0 && j < mLen; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    dig[j - 1] = -a;
                    for (; fmask.dig[j] == 0; j++) {
                        dig[j] ^= -1;
                    }
                    if (j < mLen) {
                        dig[j] = ~(dig[j] | -fmask.dig[j]);
                    } else {
                        dig[j] = ~-dig[j];
                    }
                    ++j;
                } else if (a == 0) { // && (b!=0 || j==mLen)

                    for (dig[j - 1] = b; j < mLen && dig[j] == 0; j++) {
                        dig[j] = fmask.dig[j];
                    }
                } else { // a!=0 && b!=0

                    dig[j - 1] = -(a | -b);
                }
                for (; j < mLen; j++) {
                    dig[j] = ~dig[j] & fmask.dig[j]; // ~(dig[j]|~mask.dig[j])
                }
                sign = -1;
                len = fmask.len;
                while (dig[len - 1] == 0) {
                    --len;
                }
            }
        } else {
            final int mLen = Math.min(fmask.len, len);
            int a = dig[0], b = fmask.dig[0], j = 1;
            for (; (a | b) == 0 && j < mLen; a = dig[j], b = fmask.dig[j], j++) {
            }
            if (fmask.sign > 0) {
                if (a != 0 && b == 0) {
                    for (; j < mLen && fmask.dig[j] == 0; j++) {
                    }
                } else if (a == 0) { // && (b!=0 || j==mLen)

                    dig[j - 1] = -b;
                    for (; j < mLen && dig[j] == 0; j++) {
                        dig[j] = ~fmask.dig[j];
                    }
                    if (j < mLen) {
                        dig[j] = ~(-dig[j] | fmask.dig[j]);
                    } else {
                        for (; dig[j] == 0; j++) {
                            dig[j] = -1;
                        }
                        dig[j] = ~-dig[j];
                    }
                    ++j;
                } else { // a!=0 && b!=0

                    dig[j - 1] = -(-a | b);
                }
                for (; j < mLen; j++) {
                    dig[j] &= ~fmask.dig[j]; // ~(~dig[j]|mask.dig[j])
                }
            } else {
                if (a != 0 && b == 0) {
                    for (; j < mLen && fmask.dig[j] == 0; j++) {
                    }
                    if (j < mLen) {
                        dig[j] = ~(~dig[j] | -fmask.dig[j]);
                    }
                    ++j;
                } else if (a == 0) { // && (b!=0 || j==mLen)

                    for (dig[j - 1] = b; j < mLen && dig[j] == 0; j++) {
                        dig[j] = fmask.dig[j];
                    }
                    if (j < mLen) {
                        dig[j] = ~(-dig[j] | ~fmask.dig[j]);
                    }
                    ++j;
                } else { // a!=0 && b!=0

                    dig[j - 1] = -(-a | -b);
                }
                for (; j < mLen; j++) {
                    dig[j] &= fmask.dig[j]; // ~(~dig[j]|~mask.dig[j])
                }
                len = mLen;
            }
            while (dig[len - 1] == 0) {
                --len;
            }
        }
    }

    /**
     * Bitwise-xors this number with the given number, i.e. this ^= mask.
     *
     * @param fmask The number to bitwise-xor with.
     * @complexity O(n)
     */
    public void xor(final BigInt fmask) {
        if (sign > 0) {
            if (fmask.len > len) {
                if (fmask.len > dig.length) {
                    realloc(fmask.len + 2);
                }
                System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
            }
            final int mlen = Math.min(len, fmask.len);
            if (fmask.sign > 0) {
                for (int i = 0; i < mlen; i++) {
                    dig[i] ^= fmask.dig[i];
                }
            } else {
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0 && j < mlen; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    dig[j - 1] = -a;
                    for (; fmask.dig[j] == 0; ++j) {
                        dig[j] ^= -1;
                    }
                    if (j < len) {
                        dig[j] = ~(dig[j] ^ -fmask.dig[j]);
                    } else {
                        dig[j] = ~-fmask.dig[j];
                    }
                    ++j;
                } else if (a == 0) { // && (b!=0 || j==mLen)

                    dig[j - 1] = b; // -(0^-b)
                } else { // a!=0 && b!=0

                    dig[j - 1] = -(a ^ -b);
                    for (; j < mlen && dig[j - 1] == 0; j++) {
                        dig[j] = -(dig[j] ^ ~fmask.dig[j]);
                    }
                    if (j >= mlen && dig[j - 1] == 0) {
                        final int[] tmp = j < len ? dig : fmask.dig;
                        final int blen = Math.max(len, fmask.len);
                        for (; j < blen && tmp[j] == -1; j++) {
                            dig[j] = 0;
                        }
                        if (blen == dig.length) {
                            realloc(blen + 2); // len==blen
                        }
                        if (j == blen) {
                            dig[blen] = 1;
                            len = blen + 1;
                        } else {
                            dig[j] = -~tmp[j];
                        }
                        ++j;
                    }
                }
                for (; j < mlen; j++) {
                    dig[j] ^= fmask.dig[j]; // ~(dig[j]^~mask.dig[j]);
                }
                sign = -1;
            }
            if (fmask.len > len) {
                len = fmask.len;
            } else {
                while (dig[len - 1] == 0 && len > 1) {
                    --len;
                }
            }
        } else {
            if (fmask.len > len) {
                if (fmask.len > dig.length) {
                    realloc(fmask.len + 2);
                }
                System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
            }
            final int mlen = Math.min(len, fmask.len);
            if (fmask.sign > 0) {
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0 && j < mlen; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    while (j < mlen && fmask.dig[j] == 0) {
                        ++j;
                    }
                } else if (a == 0) { // && (b!=0 || j==mLen)

                    for (dig[j - 1] = -b; j < mlen && dig[j] == 0; j++) {
                        dig[j] = ~fmask.dig[j];
                    }
                    while (j < len && dig[j] == 0) {
                        dig[j++] = -1;
                    }
                    if (j < mlen) {
                        dig[j] = ~(-dig[j] ^ fmask.dig[j]);
                    } else {
                        dig[j] = ~-dig[j];
                    }
                    ++j;
                } else { // a!=0 && b!=0

                    dig[j - 1] = -(-a ^ b);
                }
                for (; j < mlen; j++) {
                    dig[j] ^= fmask.dig[j]; // ~(~dig[j]^mask.dig[j]);
                }
            } else {
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; (a | b) == 0 && j < mlen; a = dig[j], b = fmask.dig[j], j++) {
                }
                if (a != 0 && b == 0) {
                    for (dig[j - 1] = -a; fmask.dig[j] == 0; j++) {
                        dig[j] ^= -1; // ~dig[j]
                    }
                    if (j < len) {
                        dig[j] = ~dig[j] ^ -fmask.dig[j];
                    } else {
                        dig[j] = ~-dig[j]; // dig[j]==mask.dig[j], ~0^-mask.dig[j]
                    }
                    ++j;
                } else if (a == 0) { // && b!=0

                    for (dig[j - 1] = -b; j < fmask.len && dig[j] == 0; j++) {
                        dig[j] = ~fmask.dig[j];
                    }
                    while (dig[j] == 0) {
                        dig[j++] = -1;
                    }
                    if (j < fmask.len) {
                        dig[j] = -dig[j] ^ ~fmask.dig[j];
                    } else {
                        dig[j] = ~-dig[j]; // -dig[j]^~0
                    }
                    ++j;
                } else { // a!=0 && b!=0

                    dig[j - 1] = -a ^ -b;
                }
                for (; j < mlen; j++) {
                    dig[j] ^= fmask.dig[j]; // ~dig[j]^~mask.dig[j]
                }
                sign = 1;
            }
            if (fmask.len > len) {
                len = fmask.len;
            } else {
                while (dig[len - 1] == 0 && len > 1) {
                    --len;
                }
            }
        }
    }

    /**
     * Bitwise-and-nots this number with the given number, i.e. this &= ~mask.
     *
     * @param fmask The number to bitwise-and-not with.
     * @complexity O(n)
     */
    public void andNot(final BigInt fmask) {
        final int mlen = Math.min(len, fmask.len);
        if (sign > 0) {
            if (fmask.sign > 0) {
                for (int i = 0; i < mlen; i++) {
                    dig[i] &= ~fmask.dig[i];
                }
            } else {
                int j = 0;
                while (j < mlen && fmask.dig[j] == 0) {
                    ++j;
                }
                if (j < mlen) {
                    for (dig[j] &= ~-fmask.dig[j]; ++j < mlen;) {
                        dig[j] &= fmask.dig[j]; // ~~mask.dig[j]
                    }
                }
                len = mlen;
            }
        } else {
            if (fmask.len > len) {
                if (fmask.len > dig.length) {
                    realloc(fmask.len + 2);
                }
                System.arraycopy(fmask.dig, len, dig, len, fmask.len - len);
            }
            if (fmask.sign > 0) {
                int j = 0;
                while (dig[j] == 0) {
                    ++j;
                }
                if (j < mlen) {
                    dig[j] = -(-dig[j] & ~fmask.dig[j]);
                    for (; ++j < mlen && dig[j - 1] == 0;) {
                        dig[j] = -~(dig[j] | fmask.dig[j]); // -(~dig[j]&~mask.dig[j])
                    }
                    if (j == mlen && dig[j - 1] == 0) {
                        final int blen = Math.max(len, fmask.len);
                        while (j < blen && dig[j] == -1) {
                            dig[j++] = 0; // mask.dig[j]==dig[j]
                        }
                        if (j < blen) {
                            dig[j] = -~dig[j];
                        } else {
                            if (blen >= dig.length) {
                                realloc(blen + 2);
                            }
                            dig[blen] = 1;
                            len = blen + 1;
                            return;
                        }
                        ++j;
                    }
                    for (; j < mlen; j++) {
                        dig[j] |= fmask.dig[j]; // ~(~dig[j]&~mask.dig[j]);
                    }
                    if (fmask.len > len) {
                        len = fmask.len;
                    }
                }
            } else {
                int a = dig[0], b = fmask.dig[0], j = 1;
                for (; j < mlen && (a | b) == 0; a = dig[j], b = fmask.dig[j], ++j) {
                }
                if (a != 0 && b == 0) {
                    dig[j - 1] = -a;
                    for (; j < fmask.len && fmask.dig[j] == 0; j++) {
                        dig[j] ^= -1;
                    }
                    if (j < len) {
                        dig[j] = ~(dig[j] | -fmask.dig[j]); // ~dig[j]&~-mask.dig[j]);
                    } else {
                        dig[j] = ~-dig[j]; // dig[j]==mask.dig[j]
                    }
                    ++j;
                } else if (a == 0) { // && (b!=0 || j==mlen)

                    for (; j < mlen && dig[j] == 0; j++) {
                    }
                    if (j < mlen) {
                        dig[j] = -dig[j] & fmask.dig[j]; // ~~mask.dig[j]
                    }
                    ++j;
                } else {
                    dig[j - 1] = -a & ~-b;
                }
                for (; j < mlen; j++) {
                    dig[j] = ~dig[j] & fmask.dig[j];
                }
                len = fmask.len;
                sign = 1;
            }
        }
        while (dig[len - 1] == 0 && len > 1) {
            --len;
        }
    }

    /**
     * Inverts sign and all bits of this number, i.e. this = ~this. The identity -this = ~this + 1
     * holds.
     *
     * @complexity O(n)
     */
    public void not() {
        if (sign > 0) {
            sign = -1;
            uaddMag(1);
        } else {
            sign = 1;
            usubMag(1);
        }
    }

    public byte getByteAt(final int index) {
        return (byte) (dig[index / 4] >> 8 * (index % 4) & 0xFF);
    }

    public byte getBitAt(final int index) {
        return (byte) (dig[index / 32] >> 1 * (index + 1 % 32 - 1) & 1);
    }

    public byte[] getBytes() {
        final byte[] result = new byte[dig.length * 4];
        for (int i = 0; i < dig.length; i++) {
            result[i * 4] = (byte) (dig[i] & 0xFF);
            result[i * 4 + 1] = (byte) (dig[i] >> 8 & 0xFF);
            result[i * 4 + 2] = (byte) (dig[i] >> 16 & 0xFF);
            result[i * 4 + 3] = (byte) (dig[i] >> 24 & 0xFF);
        }
        return result;
    }

    public void setByte(final int index, final byte value) {
        if (index / 4 + 1 > len) {
            len++;
        }
        switch (index % 4) {
            case 0:
                dig[index / 4] = dig[index / 4] & 0xFFFFFF00 | value & 0xFF;
                break;
            case 1:
                dig[index / 4] = dig[index / 4] & 0xFFFF00FF | (value & 0xFF) << 8;
                break;
            case 2:
                dig[index / 4] = dig[index / 4] & 0xFF00FFFF | (value & 0xFF) << 16;
                break;
            case 3:
                dig[index / 4] = dig[index / 4] & 0x00FFFFFF | (value & 0xFF) << 24;
                break;
        }
    }

    public int length() {
        if (squeaksize != -1) {
            return squeaksize;
        }
        for (int i = dig.length * 4 - 1; i >= 0; i--) {
            if (getByteAt(i) != 0) {
                return i + 1;
            }
        }
        return 0;
    }

    public int bitLength() {
        if (squeaksize == 0) {
            return 0;
        }
        for (int i = len * 32 - 1; i >= 0; i--) {
            if (getBitAt(i) != 0) {
                return i + 1;
            }
        }
        return 0;
    }

    public void setSize(final int size) {
        if (size == 0) {
            dig = new int[0];
        } else {
            this.realloc((int) Math.ceil(size / 4.0)); // 4 bytes per integer
        }
        squeaksize = size;
    }

    public void setNegative() {
        this.sign = -1;
    }

    public void setPositive() {
        this.sign = 1;
    }

    public void setBytes(final byte[] bytes, final int srcPos, final int destPos, final int length) {
        for (int i = 0; i < length; i++) {
            setByte(destPos + i, bytes[srcPos + i]);
        }
    }

    public void reduceIfPossible() {
        squeaksize = -1;
        for (int i = len - 1; i >= 0; i--) {
            if (dig[i] == 0 && len > 1) {
                len--;
            } else {
                return;
            }
        }
    }

    public void negate() {
        this.sign = this.sign * -1;
    }

    /*** </BitOperations> ***/
}
