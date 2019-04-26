package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.graal.squeak.model.FloatObject;

@TypeSystem
public class SqueakArithmeticTypes {
    @ImplicitCast
    public static final double fromFloatObject(final FloatObject object) {
        return object.getValue();
    }
}
