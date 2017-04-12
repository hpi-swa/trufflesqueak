package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;
import de.hpi.swa.trufflesqueak.model.NilObject;

import java.math.BigInteger;

@TypeSystem({boolean.class,
                long.class,
                BigInteger.class,
                String.class,
                NilObject.class})
public abstract class Types {

    @TypeCheck(NilObject.class)
    public static boolean isNil(Object value) {
        return value == NilObject.SINGLETON;
    }

    @ImplicitCast
    @TruffleBoundary
    public static BigInteger castBigInteger(long value) {
        return BigInteger.valueOf(value);
    }
}
