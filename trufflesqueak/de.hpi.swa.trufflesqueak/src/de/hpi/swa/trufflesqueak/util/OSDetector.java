package de.hpi.swa.trufflesqueak.util;

public class OSDetector {

    public enum OSType {
        Windows,
        MacOS,
        Linux,
        Other
    }

    private final OSType currentOS;

    public OSDetector() {
        String OS = System.getProperty("os.name", "generic").toLowerCase();
        if ((OS.indexOf("mac") >= 0) || (OS.indexOf("darwin") >= 0)) {
            currentOS = OSType.MacOS;
        } else if (OS.indexOf("win") >= 0) {
            currentOS = OSType.Windows;
        } else if (OS.indexOf("nux") >= 0) {
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
}
