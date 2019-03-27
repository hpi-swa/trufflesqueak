package de.hpi.swa.graal.squeak.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectToObjectArrayNode;

public abstract class UnwrapFromSqueakNode extends AbstractNodeWithImage {

    protected UnwrapFromSqueakNode(final SqueakImageContext image) {
        super(image);
    }

    public static UnwrapFromSqueakNode create(final SqueakImageContext image) {
        return UnwrapFromSqueakNodeGen.create(image);
    }

    public final Object[] executeList(final Object... values) {
        final Object[] unwrappedElements = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            unwrappedElements[i] = executeUnwrap(values[i]);
        }
        return unwrappedElements;
    }

    public abstract Object executeUnwrap(Object value);

    @Specialization(guards = "nullValue == null")
    protected final NilObject doNull(@SuppressWarnings("unused") final Object nullValue) {
        return image.nil;
    }

    @Specialization
    protected static final boolean doBoolean(final boolean value) {
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
    protected static final double doInteger(final double value) {
        return value;
    }

    @Specialization
    protected static final BigInteger doBigInteger(final LargeIntegerObject value) {
        return value.getBigInteger();
    }

    @Specialization
    protected static final char doChar(final char value) {
        return value;
    }

    @Specialization(guards = "value.isString()")
    protected static final String doString(final NativeObject value) {
        return value.asString();
    }

    @Specialization(guards = {"!value.isString()", "value.isByteType()"})
    protected static final byte[] doBytes(final NativeObject value) {
        return value.getByteStorage();
    }

    @Specialization
    protected final Object[] doObjects(final ArrayObject value,
                    @Cached("create()") final ArrayObjectToObjectArrayNode toObjectArrayNode) {
        return executeList(toObjectArrayNode.execute(value));
    }

    @Specialization
    protected static final TruffleObject doTruffleObject(final TruffleObject value) {
        return value;
    }
}
