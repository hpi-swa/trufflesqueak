package de.hpi.swa.graal.squeak.interop;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

@GenerateUncached
public abstract class WrapToSqueakNode extends AbstractNode {

    public static WrapToSqueakNode getUncached() {
        return WrapToSqueakNodeGen.getUncached();
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
        return (ArrayObject) executeWrap(values);
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
    protected static final NativeObject doString(final String value,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.asByteString(value);
    }

    @Specialization
    protected static final char doChar(final char value) {
        return value;
    }

    @Specialization
    protected final ArrayObject doObjects(final Object[] values,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        return image.asArrayOfObjects(executeObjects(values));
    }

    @Fallback
    protected static final Object doObject(final Object value) {
        return value;
    }
}
