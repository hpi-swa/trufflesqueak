package de.hpi.swa.trufflesqueak.nodes;

import java.math.BigInteger;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeCast;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
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
                long.class,
                BigInteger.class,
                String.class,
                NilObject.class,
                FalseObject.class,
                TrueObject.class,
                SmallInteger.class,
                ImmediateCharacter.class,
                ListObject.class,
                PointersObject.class,
                NativeObject.class,
                CompiledMethodObject.class,
                BaseSqueakObject.class})
public abstract class SqueakTypes {

    @ImplicitCast
    public static long castLong(SmallInteger obj) {
        return obj.getValue();
    }

    @ImplicitCast
    public static int castInt(SmallInteger obj) {
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

    @ImplicitCast
    public static String castString(NativeObject obj) {
        return obj.toString();
    }

    @ImplicitCast
    public static boolean castBoolean(@SuppressWarnings("unused") TrueObject obj) {
        return true;
    }

    @ImplicitCast
    public static boolean castBoolean(@SuppressWarnings("unused") FalseObject obj) {
        return false;
    }

    @ImplicitCast
    public static BaseSqueakObject asBaseSqueakObject(int value) {
        return new SmallInteger(value);
    }

    @ImplicitCast
    public static BaseSqueakObject asBaseSqueakObject(char value) {
        return new ImmediateCharacter(value);
    }

    @ImplicitCast
    public static BaseSqueakObject asBaseSqueakObject(boolean value) {
        if (value) {
            return new TrueObject();
        } else {
            return new FalseObject();
        }
    }
}
