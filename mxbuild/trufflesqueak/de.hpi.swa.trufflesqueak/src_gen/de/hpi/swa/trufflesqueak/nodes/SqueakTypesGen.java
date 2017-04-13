// CheckStyle: start generated
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.GeneratedBy;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import de.hpi.swa.trufflesqueak.model.NilObject;
import java.math.BigInteger;

@GeneratedBy(SqueakTypes.class)
public final class SqueakTypesGen extends SqueakTypes {

    @Deprecated public static final SqueakTypesGen SQUEAKTYPES = new SqueakTypesGen();

    protected SqueakTypesGen() {
    }

    public static boolean isBoolean(Object value) {
        return value instanceof Boolean;
    }

    public static boolean asBoolean(Object value) {
        assert value instanceof Boolean : "SqueakTypesGen.asBoolean: boolean expected";
        return (boolean) value;
    }

    public static boolean expectBoolean(Object value) throws UnexpectedResultException {
        if (value instanceof Boolean) {
            return (boolean) value;
        }
        throw new UnexpectedResultException(value);
    }

    public static boolean isLong(Object value) {
        return value instanceof Long;
    }

    public static long asLong(Object value) {
        assert value instanceof Long : "SqueakTypesGen.asLong: long expected";
        return (long) value;
    }

    public static long expectLong(Object value) throws UnexpectedResultException {
        if (value instanceof Long) {
            return (long) value;
        }
        throw new UnexpectedResultException(value);
    }

    public static boolean isBigInteger(Object value) {
        return value instanceof BigInteger;
    }

    public static BigInteger asBigInteger(Object value) {
        assert value instanceof BigInteger : "SqueakTypesGen.asBigInteger: BigInteger expected";
        return (BigInteger) value;
    }

    public static BigInteger expectBigInteger(Object value) throws UnexpectedResultException {
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        throw new UnexpectedResultException(value);
    }

    public static boolean isString(Object value) {
        return value instanceof String;
    }

    public static String asString(Object value) {
        assert value instanceof String : "SqueakTypesGen.asString: String expected";
        return (String) value;
    }

    public static String expectString(Object value) throws UnexpectedResultException {
        if (value instanceof String) {
            return (String) value;
        }
        throw new UnexpectedResultException(value);
    }

    public static boolean isNilObject(Object value) {
        return value instanceof NilObject;
    }

    public static NilObject asNilObject(Object value) {
        assert value instanceof NilObject : "SqueakTypesGen.asNilObject: NilObject expected";
        return (NilObject) value;
    }

    public static NilObject expectNilObject(Object value) throws UnexpectedResultException {
        if (value instanceof NilObject) {
            return (NilObject) value;
        }
        throw new UnexpectedResultException(value);
    }

    public static BigInteger expectImplicitBigInteger(int state, Object value) throws UnexpectedResultException {
        if ((state & 0b1) != 0 && value instanceof Long) {
            return castBigInteger((long) value);
        } else if ((state & 0b10) != 0 && value instanceof BigInteger) {
            return (BigInteger) value;
        } else {
            throw new UnexpectedResultException(value);
        }
    }

    public static boolean isImplicitBigInteger(int state, Object value) {
        return ((state & 0b1) != 0 && value instanceof Long)
             || ((state & 0b10) != 0 && value instanceof BigInteger);
    }

    public static BigInteger asImplicitBigInteger(int state, Object value) {
        if ((state & 0b1) != 0 && value instanceof Long) {
            return castBigInteger((long) value);
        } else if ((state & 0b10) != 0 && value instanceof BigInteger) {
            return (BigInteger) value;
        } else {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            throw new IllegalArgumentException("Illegal type ");
        }
    }

    public static int specializeImplicitBigInteger(Object value) {
        if (value instanceof Long) {
            return 0b1;
        } else if (value instanceof BigInteger) {
            return 0b10;
        } else {
            return 0;
        }
    }

}
