/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.trufflesqueak.model.PointersObject;

public final class SqueakProfiles {

    public abstract static class SqueakProfile {
        public static SqueakProfile createLiteralProfile(final Object literal) {
            CompilerAsserts.neverPartOfCompilation();
            if (!isProfilingEnabled()) {
                return Disabled.INSTANCE;
            } else if (literal instanceof PointersObject && "ClassBinding".equals(((PointersObject) literal).getSqueakClass().getClassName())) {
                /*
                 * Use a renewable identity profile for ClassBindings objects. Values of
                 * ClassBindings only change when classes are recompiled. Deoptimization is allowed
                 * in such cases.
                 */
                return new RenewableIdentityProfile();
            } else {
                return new IdentityProfile();
            }
        }

        private static boolean isProfilingEnabled() {
            return Truffle.getRuntime().isProfilingEnabled();
        }

        public abstract <T> T profile(T value);
    }

    /** Standard one-shot identity profile (see {@link ValueProfile}). */
    private static final class IdentityProfile extends SqueakProfile {
        private static final Object UNINITIALIZED = new Object();
        private static final Object GENERIC = new Object();

        @CompilationFinal protected Object cachedValue = UNINITIALIZED;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T profile(final T newValue) {
            // Field needs to be cached in local variable for thread safety and startup speed.
            final Object cached = cachedValue;
            if (cached != GENERIC) {
                if (cached == newValue) {
                    return (T) cached;
                } else {
                    CompilerDirectives.transferToInterpreterAndInvalidate();
                    if (cachedValue == UNINITIALIZED) {
                        cachedValue = newValue;
                    } else {
                        cachedValue = GENERIC;
                    }
                }
            }
            return newValue;
        }
    }

    private static final class RenewableIdentityProfile extends SqueakProfile {
        @CompilationFinal protected Object cachedValue;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T profile(final T newValue) {
            // Field needs to be cached in local variable for thread safety and startup speed.
            final Object cached = cachedValue;
            if (cached == newValue) {
                return (T) cached;
            } else {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedValue = newValue;
                return newValue;
            }
        }
    }

    private static final class Disabled extends SqueakProfile {
        private static final SqueakProfile INSTANCE = new Disabled();

        @Override
        protected Object clone() {
            return INSTANCE;
        }

        @Override
        public <T> T profile(final T value) {
            return value;
        }
    }

    private SqueakProfiles() {
    }
}
