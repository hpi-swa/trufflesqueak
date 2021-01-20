/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;

public enum OS {
    Linux,
    macOS,
    Windows;

    public enum OSType {
        Windows,
        MacOS,
        Linux,
        Other
    }

    private static final OS THE_OS = findOS();
    private static final String SQUEAK_PLATFORM_NAME = findSqueakOSName();
    private static final String FFI_EXTENSION = findFFIExtension();

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

    public static String findFFIExtension() {
        switch (THE_OS) {
            case macOS:
                return ".dylib";
            case Windows:
                return ".dll";
            case Linux:
                return ".so";
            default:
                throw SqueakException.create("Unsupported Platform.");
        }
    }

    public static OS getOS() {
        return THE_OS;
    }

    public static String getSqueakPlatformName() {
        return SQUEAK_PLATFORM_NAME;
    }

    public static String getFFIExtension() {
        return FFI_EXTENSION;
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
