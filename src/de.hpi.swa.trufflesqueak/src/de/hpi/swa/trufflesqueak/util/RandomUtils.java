/*
 * Copyright (c) 2026-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.Security;

import com.oracle.truffle.api.CompilerDirectives;

public final class RandomUtils {
    private static final SecureRandom RANDOM = getSecureRandomInstance();

    private RandomUtils() {
    }

    private static SecureRandom getSecureRandomInstance() {
        if (Security.getAlgorithms("SecureRandom").contains("NATIVEPRNGNONBLOCKING")) {
            try {
                return SecureRandom.getInstance("NATIVEPRNGNONBLOCKING");
            } catch (NoSuchAlgorithmException e) {
                throw CompilerDirectives.shouldNotReachHere(e);
            }
        } else {
            return new SecureRandom();
        }
    }

    public static SecureRandom getSecureRandom() {
        return RANDOM;
    }
}
