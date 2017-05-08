package de.hpi.swa.trufflesqueak.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.ImmediateCharacter;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.TrueObject;

@TypeSystem({boolean.class,
                int.class,
                long.class,
                BigInteger.class,
                String.class,
                NilObject.class,
                FalseObject.class,
                TrueObject.class,
                SmallInteger.class,
                ImmediateCharacter.class,
                ClassObject.class,
                ListObject.class,
                PointersObject.class,
                BlockClosure.class,
                NativeObject.class,
                CompiledMethodObject.class,
                BaseSqueakObject.class})
public abstract class SqueakTypes {
    @ImplicitCast
    public static int castInt(SmallInteger obj) {
        return obj.getValue();
    }

    @ImplicitCast
    public static long castLong(int obj) {
        return obj;
    }

    @ImplicitCast
    public static long castLong(SmallInteger obj) {
        return obj.getValue();
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
    @TruffleBoundary
    public static BigInteger castBigInteger(SmallInteger value) {
        return BigInteger.valueOf(value.getValue());
    }
}
