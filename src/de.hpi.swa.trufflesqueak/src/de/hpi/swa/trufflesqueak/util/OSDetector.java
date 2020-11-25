/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;

public final class OSDetector {
    public static final OSDetector SINGLETON = new OSDetector();

    public enum OSType {
        Windows,
        MacOS,
        Linux,
        Other
    }

    private final OSType currentOS;

    private OSDetector() {
        final String os = System.getProperty("os.name", "generic").toLowerCase();
        if (os.contains("mac") || os.contains("darwin")) {
            currentOS = OSType.MacOS;
        } else if (os.contains("win")) {
            currentOS = OSType.Windows;
        } else if (os.contains("nux")) {
            currentOS = OSType.Linux;
        } else {
            currentOS = OSType.Other;
        }
    }

    public String getSqOSName() {
        switch (currentOS) {
            case MacOS:
                return "Mac OS";
            case Windows:
                return "Win32";
            case Linux:
                return "unix";
            default:
                return "unknown";
        }
    }

    public boolean isMacOS() {
        return currentOS == OSType.MacOS;
    }

    public boolean isWindows() {
        return currentOS == OSType.Windows;
    }

    public boolean isLinux() {
        return currentOS == OSType.Linux;
    }

    public String getFFIExtension() {
        switch (currentOS) {
            case MacOS:
                return ".dylib";
            case Windows:
                return ".dll";
            case Linux:
                return ".so";
            default:
                throw SqueakException.create("Unsupported Platform.");
        }
    }
}
