package de.hpi.swa.graal.squeak.nodes.primitives;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SqueakPrimitive {
    int[] indices() default {};

    String[] names() default {};
}
