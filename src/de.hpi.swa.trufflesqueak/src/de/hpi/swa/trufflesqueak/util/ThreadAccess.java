/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the thread-safety access contract for a field shared across multiple threads.
 * This annotation documents intended synchronization rules and provides guidelines
 * on how shared fields must be safely accessed and mutated.
 * Specify the weakest access mode that guarantees correctness,
 * rather than defaulting to the strongest available mechanism.
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.FIELD)
public @interface ThreadAccess {
    Mode value();

    /**
     * Identifies the synchronization mechanism enforcing ordering or atomicity for this field
     * (e.g. the name of a {@code VarHandle}, {@code AtomicReferenceFieldUpdater}, guarding lock,
     * or VM-level operation).
     */
    String orderedBy() default "";

    enum Mode {
        // TODO: Add more modes if needed.
        /**
         * Updated only by {@code compareAndSet}, typically in a retry loop.
         */
        CAS,
    }
}
