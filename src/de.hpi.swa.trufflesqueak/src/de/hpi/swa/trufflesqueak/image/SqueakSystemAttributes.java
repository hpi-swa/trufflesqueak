/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.MiscUtils;
import de.hpi.swa.trufflesqueak.util.OS;
import io.github.humbleui.jwm.App;
import io.github.humbleui.types.IRect;

public final class SqueakSystemAttributes {
    private final SqueakImageContext image;
    private final NativeObject vmPath;
    @CompilationFinal private NativeObject imagePath;
    @CompilationFinal(dimensions = 1) private NativeObject[] cmdArguments;
    private final NativeObject platformName;
    private final NativeObject operatingSystemVersion;
    private final NativeObject platformProcessorType;
    private final NativeObject vmVersion;
    private final NativeObject windowSystemName;
    private final NativeObject vmBuildId;
    private final NativeObject interpreterClass;
    private final NativeObject systemProperties;
    private final NativeObject vmInformation;
    private final NativeObject maxFilenameLength;
    private final NativeObject fileLastError;
    private final NativeObject hardwareDetails;
    private final NativeObject operatingSystemDetails;

    public SqueakSystemAttributes(final SqueakImageContext image) {
        this.image = image;

        final String osName = System.getProperty("os.name", "unknown os.name");
        final String osVersion = System.getProperty("os.version", "unknown os.version");
        final String osArch = System.getProperty("os.arch", "unknown os.arch");

        final String separator = File.separator;
        vmPath = asByteString(System.getProperty("java.home") + separator + "bin" + separator + "java");

        platformName = asByteString(OS.findSqueakOSName());
        operatingSystemVersion = asByteString(determineOperatingSystemVersion(osVersion));
        platformProcessorType = asByteString(determinePlatformProcessorType(osArch));

        /*
         * Start with "Croquet" to let `LanguageEnvironment win32VMUsesUnicode` return `true`. Add
         * fake VMMaker info to make `Smalltalk vmVMMakerVersion` work.
         */
        vmVersion = asByteString("Croquet " + SqueakLanguageConfig.IMPLEMENTATION_NAME + " " + SqueakLanguageConfig.VERSION + " VMMaker.fn.9999");

        windowSystemName = asByteString(OS.findWindowSystemName());

        final String date = new SimpleDateFormat("MMM dd yyyy HH:mm:ss zzz", Locale.US).format(new Date(MiscUtils.getStartTime()));
        vmBuildId = asByteString(String.format("%s %s (%s) built on %s", osName, osVersion, osArch, date));

        /*
         * For SmalltalkImage>>#interpreterVMMakerVersion (see
         * https://lists.squeakfoundation.org/pipermail/squeak-dev/2022-March/219464.html).
         */
        interpreterClass = asByteString(String.format("TruffleSqueak Interpreter VMMaker.oscog-fn.3184 (%s)", MiscUtils.getGraalVMInformation()));

        systemProperties = asByteString(MiscUtils.getSystemProperties());
        vmInformation = asByteString(MiscUtils.getVMInformation());
        maxFilenameLength = asByteString("255");
        fileLastError = asByteString("0");
        hardwareDetails = asByteString("Hardware information: not supported");
        operatingSystemDetails = asByteString(String.format("Operating System: %s (%s, %s)", osName, osVersion, osArch));
    }

    /** See SmalltalkImage>>#getSystemAttribute:. */
    public AbstractSqueakObject getSystemAttribute(final int index) {
        return switch (index) {
            case 0 -> getVMPath();
            case 1 -> getImagePath();
            case 1001 -> getPlatformName();
            case 1002 -> getOperatingSystemVersion();
            case 1003 -> getPlatformProcessorType();
            case 1004 -> getVMVersion();
            case 1005 -> getWindowSystemName();
            case 1006 -> getVmBuildId();
            case 1007 -> getInterpreterClass();
            case 1008 -> getSystemProperties();
            case 1009 -> getVMInformation();
            case 1201 -> getMaxFilenameLength();
            case 1202 -> getFileLastError();
            case 10001 -> getHardwareDetails();
            case 10002 -> getOperatingSystemDetails();
            case 10003 -> getGraphicsHardwareDetails();
            default -> {
                if (index >= 2 && index <= 1000) {
                    yield getCMDArgument(index - 2);
                } else {
                    yield NilObject.SINGLETON;
                }
            }
        };
    }

    /** Attribute #0. */
    private NativeObject getVMPath() {
        return vmPath.shallowCopyBytes();
    }

    /** Attribute #1. */
    private NativeObject getImagePath() {
        if (imagePath == null) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            imagePath = asByteString(image.getImagePath());
        }
        return imagePath.shallowCopyBytes();
    }

    /** Attributes #2 to #1000. */
    private AbstractSqueakObject getCMDArgument(final int index) {
        if (cmdArguments == null) {
            final String[] imageArguments = image.getImageArguments();
            cmdArguments = new NativeObject[imageArguments.length];
            for (int i = 0; i < cmdArguments.length; i++) {
                cmdArguments[i] = asByteString(imageArguments[i]);
            }
        }
        if (index < cmdArguments.length) {
            return cmdArguments[index].shallowCopyBytes();
        } else {
            return NilObject.SINGLETON;
        }
    }

    /** Attribute #1001. */
    private NativeObject getPlatformName() {
        return platformName.shallowCopyBytes();
    }

    /** Attribute #1002. */
    private NativeObject getOperatingSystemVersion() {
        return operatingSystemVersion.shallowCopyBytes();
    }

    /** Attribute #1003. */
    private NativeObject getPlatformProcessorType() {
        return platformProcessorType.shallowCopyBytes();
    }

    /** Attribute #1004. */
    private NativeObject getVMVersion() {
        return vmVersion.shallowCopyBytes();
    }

    /** Attribute #1005. */
    private NativeObject getWindowSystemName() {
        return windowSystemName.shallowCopyBytes();
    }

    /** Attribute #1006. */
    private NativeObject getVmBuildId() {
        return vmBuildId.shallowCopyBytes();
    }

    /** Attribute #1007: "Interpreter class (Cog VM only)". */
    private NativeObject getInterpreterClass() {
        return interpreterClass.shallowCopyBytes();
    }

    /** Attribute #1008: "Cogit class (Cog VM only)". */
    private NativeObject getSystemProperties() {
        return systemProperties.shallowCopyBytes();
    }

    /** Attribute #1009: "Platform source version (Cog VM only?)". */
    private NativeObject getVMInformation() {
        return vmInformation.shallowCopyBytes();
    }

    /** Attribute #1201. */
    private NativeObject getMaxFilenameLength() {
        return maxFilenameLength.shallowCopyBytes();
    }

    /** Attribute #1202. */
    private NativeObject getFileLastError() {
        return fileLastError.shallowCopyBytes();
    }

    /** Attribute #10001. */
    private NativeObject getHardwareDetails() {
        return hardwareDetails.shallowCopyBytes();
    }

    /** Attribute #10002. */
    private NativeObject getOperatingSystemDetails() {
        return operatingSystemDetails.shallowCopyBytes();
    }

    /** Attribute #10003. */
    @TruffleBoundary
    private NativeObject getGraphicsHardwareDetails() {
        int width = 0;
        int height = 0;
        if (!image.options.isHeadless()) {
            final IRect bounds = App.getPrimaryScreen().getBounds();
            width = bounds.getWidth();
            height = bounds.getHeight();
        } /* Report 0 x 0 in headless mode. */
        return asByteString(String.format("Display Information: \n\tPrimary monitor resolution: %s x %s\n", width, height));
    }

    /**
     * The image expects things like 1095, so convert 10.10.5 into 1010.5 (e.g., see
     * #systemConverterClass).
     */
    private static String determineOperatingSystemVersion(final String osVersion) {
        if (OS.isMacOS()) {
            String major = "10";
            String minor = "16";
            String patch = "0";
            final String[] osVersionParts = osVersion.split("\\.");
            if (osVersionParts.length > 0) {
                major = osVersionParts[0];
                if (osVersionParts.length > 1) {
                    minor = osVersionParts[1];
                    minor = minor.length() == 1 ? "0" + minor : minor;
                    if (osVersionParts.length > 2) {
                        patch = osVersionParts[2];
                    }
                }
            }
            return String.format("%s%s.%s", major, minor, patch);
        } else {
            return osVersion;
        }
    }

    private static String determinePlatformProcessorType(final String osArch) {
        if (osArch.equals("aarch64")) {
            /* Requires one of #('aarch64' 'arm64') for 'FFIPlatformDescription>>#abi'. */
            /* Begins with "arm" for `SmalltalkImage>>#isLowerPerformance`. */
            return OS.isMacOS() ? "aarch64" : "arm64";
        } else {
            return "x64"; /* For users of `Smalltalk os platformSubtype`. */
        }
    }

    private NativeObject asByteString(final String value) {
        return image.asByteString(value);
    }
}
