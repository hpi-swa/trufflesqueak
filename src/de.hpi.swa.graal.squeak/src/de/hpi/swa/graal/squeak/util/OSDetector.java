package de.hpi.swa.graal.squeak.util;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;

public class OSDetector {

    public enum OSType {
        Windows,
        MacOS,
        Linux,
        Other
    }

    private final OSType currentOS;

    public OSDetector() {
        final String os = System.getProperty("os.name", "generic").toLowerCase();
        if (os.indexOf("mac") >= 0 || os.indexOf("darwin") >= 0) {
            currentOS = OSType.MacOS;
        } else if (os.indexOf("win") >= 0) {
            currentOS = OSType.Windows;
        } else if (os.indexOf("nux") >= 0) {
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
