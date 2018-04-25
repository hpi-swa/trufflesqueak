package de.hpi.swa.graal.squeak.nodes.primitives;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface SqueakPrimitive {
    int index() default -1;

    int[] indices() default {};

    String name() default "";

    String[] names() default {};

    int numArguments() default 1;
}
