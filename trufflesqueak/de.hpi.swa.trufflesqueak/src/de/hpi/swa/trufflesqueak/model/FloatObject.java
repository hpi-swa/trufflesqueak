package de.hpi.swa.trufflesqueak.model;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.util.SqueakImageChunk;

public class FloatObject extends SqueakObject {
    @CompilationFinal private double value;

    public FloatObject(SqueakImageContext image) {
        super(image, image.floatClass);
    }

    public FloatObject(FloatObject original) {
        this(original.image);
        this.value = original.value;
    }

    public FloatObject(SqueakImageContext image, double value) {
        this(image);
        this.value = value;
    }

    @Override
    public void fillin(SqueakImageChunk chunk) {
        throw new SqueakException("Not implemented by FloatObject");
    }

    @Override
    public Object at0(long index) {
        Long bits = Double.doubleToLongBits(value);
        if (index == 0) {
            return (bits >> 32) & 0xffffffff;
        } else if (index == 1) {
            return bits & 0xffffffff;
        } else {
            throw new SqueakException("FloatObject only has two slots");
        }
    }

    @Override
    public void atput0(long index, Object object) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        Long bits = Double.doubleToLongBits(value);
        long objectMasked = (long) object & 0xffffffff;
        if (index == 0) {
            value = Double.longBitsToDouble(objectMasked << 32 | bits & 0xffffffff);
        } else if (index == 1) {
            value = Double.longBitsToDouble((bits >> 32) << 32 | objectMasked);
        } else {
            throw new SqueakException("FloatObject only has two slots");
        }
    }

    @Override
    public final int size() {
        return 2;
    }

    @Override
    public final int instsize() {
        return 0;
    }

    public double getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "" + value;
    }

    @Override
    public boolean equals(Object b) {
        if (b instanceof FloatObject) {
            return value == value;
        } else {
            return super.equals(b);
        }
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public BaseSqueakObject shallowCopy() {
        return new FloatObject(this);
    }

    public static double bytesAsFloatObject(byte[] someBytes) {
        ByteBuffer buf = ByteBuffer.allocate(8); // 2 * 32 bit
        buf.order(ByteOrder.nativeOrder());
        buf.put(someBytes);
        buf.rewind();
        long low = Integer.toUnsignedLong(buf.asIntBuffer().get(0));
        long high = Integer.toUnsignedLong(buf.asIntBuffer().get(1));
        return Double.longBitsToDouble(high << 32 | low);
    }
}
