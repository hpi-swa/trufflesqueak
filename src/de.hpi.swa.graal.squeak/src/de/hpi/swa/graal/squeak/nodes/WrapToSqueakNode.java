package de.hpi.swa.graal.squeak.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.io.DisplayPoint;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;

public abstract class WrapToSqueakNode extends AbstractNodeWithImage {

    protected WrapToSqueakNode(final SqueakImageContext image) {
        super(image);
    }

    public static WrapToSqueakNode create(final SqueakImageContext image) {
        return WrapToSqueakNodeGen.create(image);
    }

    public abstract Object executeWrap(Object value);

    public final Object[] executeObjects(final Object... values) {
        final Object[] wrappedElements = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            wrappedElements[i] = executeWrap(values[i]);
        }
        return wrappedElements;
    }

    public final ArrayObject executeList(final Object... values) {
        return image.newArrayOfObjects(executeObjects(values));
    }

    @Specialization(guards = "nullValue == null")
    protected final NilObject doNull(@SuppressWarnings("unused") final Object nullValue) {
        return image.nil;
    }

    @Specialization
    protected static final boolean doBoolean(final boolean value) {
        return value;
    }

    @Specialization
    protected static final long doByte(final byte value) {
        return value;
    }

    @Specialization
    protected static final long doShort(final short value) {
        return value;
    }

    @Specialization
    protected static final long doInteger(final int value) {
        return value;
    }

    @Specialization
    protected static final long doLong(final long value) {
        return value;
    }

    @Specialization
    protected static final double doFloat(final float value) {
        return value;
    }

    @Specialization
    protected static final double doDouble(final double value) {
        return value;
    }

    @Specialization
    protected final Object doBigInteger(final BigInteger value) {
        return image.wrap(value);
    }

    @Specialization
    protected final NativeObject doString(final String value) {
        return image.wrap(value);
    }

    @Specialization
    protected static final char doChar(final char value) {
        return value;
    }

    @Specialization
    protected final NativeObject doBytes(final byte[] value) {
        return image.wrap(value);
    }

    @Specialization
    protected final ArrayObject doObjects(final Object[] value) {
        return executeList(value);
    }

    @Specialization
    protected final PointersObject doDisplayPoint(final DisplayPoint value) {
        return image.wrap(value);
    }

    @Specialization
    protected static final TruffleObject doTruffleObject(final TruffleObject value) {
        return value;
    }
}
