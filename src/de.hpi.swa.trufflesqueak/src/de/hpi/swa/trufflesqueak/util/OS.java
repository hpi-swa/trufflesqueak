/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;

public enum OS {
    Linux,
    macOS,
    Windows;

    private static final OS THE_OS = findOS();
    private static final String SQUEAK_PLATFORM_NAME = findSqueakOSName();

    private static OS findOS() {
        final String name = System.getProperty("os.name");
        if (name.equals("Linux")) {
            return Linux;
        }
        if (name.equals("Mac OS X") || name.equals("Darwin")) {
            return macOS;
        }
        if (name.startsWith("Windows")) {
            return Windows;
        }
        throw SqueakException.create("Unsupported Platform: " + name);
    }

    public static String findSqueakOSName() {
        switch (THE_OS) {
            case macOS:
                return "Mac OS";
            case Windows:
                return "Win32";
            case Linux:
                return "unix";
            default:
                throw SqueakException.create("Unsupported Platform.");
        }
    }

    public static String getSqueakPlatformName() {
        return SQUEAK_PLATFORM_NAME;
    }

    public static boolean isLinux() {
        return THE_OS == Linux;
    }

    public static boolean isMacOS() {
        return THE_OS == macOS;
    }

    public static boolean isWindows() {
        return THE_OS == Windows;
    }
}
