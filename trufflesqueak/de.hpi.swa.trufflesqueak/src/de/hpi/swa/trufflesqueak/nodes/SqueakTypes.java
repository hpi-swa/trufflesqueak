package de.hpi.swa.trufflesqueak.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.ReadOnlyContextObject;

@TypeSystem({boolean.class,
                char.class,
                int.class,
                long.class,
                BigInteger.class,
                double.class,
                String.class,
                LargeIntegerObject.class,
                ClassObject.class,
                ReadOnlyContextObject.class,
                ListObject.class,
                PointersObject.class,
                BlockClosureObject.class,
                NativeObject.class,
                CompiledBlockObject.class,
                CompiledMethodObject.class,
                BaseSqueakObject.class})
public abstract class SqueakTypes {
    @ImplicitCast
    public static long castLong(int obj) {
        return obj;
    }

    @ImplicitCast
    @TruffleBoundary
    public static BigInteger castBigInteger(int value) {
        return BigInteger.valueOf(value);
    }

    @ImplicitCast
    @TruffleBoundary
    public static BigInteger castBigInteger(long value) {
        return BigInteger.valueOf(value);
    }

    @ImplicitCast
    public static BigInteger castBigInteger(LargeIntegerObject value) {
        return value.getValue();
    }

    @ImplicitCast
    public static double castDouble(int obj) {
        return obj;
    }

    @ImplicitCast
    public static double castDouble(long obj) {
        return obj;
    }

    @ImplicitCast
    public static double castDouble(BigInteger obj) {
        return obj.doubleValue();
    }

    @ImplicitCast
    public static double castDouble(LargeIntegerObject obj) {
        return obj.getValue().doubleValue();
    }
}
