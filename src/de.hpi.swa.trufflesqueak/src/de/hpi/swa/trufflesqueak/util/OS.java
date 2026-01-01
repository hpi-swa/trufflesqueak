/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
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
        return switch (THE_OS) {
            case macOS -> "Mac OS";
            case Windows -> "Win32";
            case Linux -> "unix";
        };
    }

    public static String findWindowSystemName() {
        return switch (THE_OS) {
            case macOS -> "Aqua";
            case Windows -> "Windows";
            case Linux -> "X11";
        };
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
