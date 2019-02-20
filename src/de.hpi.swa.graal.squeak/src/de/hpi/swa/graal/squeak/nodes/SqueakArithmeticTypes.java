package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeCheck;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;

@TypeSystem
public class SqueakArithmeticTypes {
    @TypeCheck(LargeIntegerObject.class)
    public static boolean isLargeInteger(final Object object) {
        return object instanceof LargeIntegerObject && ((LargeIntegerObject) object).isLargeInteger();
    }

    @ImplicitCast
    public static double fromFloatObject(final FloatObject object) {
        return object.getValue();
    }
}
