/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.image;

import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
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

        platformName = asByteString(OS.getSqueakPlatformName());

        String value;
        if (OS.isMacOS()) {
            /*
             * The image expects things like 1095, so convert 10.10.5 into 1010.5 (e.g., see
             * #systemConverterClass)
             */
            value = "1016.0";
            final String[] osVersionParts = osVersion.split("\\.");
            if (osVersionParts.length == 2) {
                final String major = osVersionParts[0];
                try {
                    final int minor = Integer.parseInt(osVersionParts[1]);
                    value = String.format("%s%02d.0", major, minor);
                } catch (final NumberFormatException e) {
                    // give up
                }
            }
        } else {
            value = osVersion;
        }
        operatingSystemVersion = asByteString(value);

        if (osArch.equals("aarch64")) {
            value = "armv8"; /* For `SmalltalkImage>>#isLowerPerformance`. */
        } else {
            value = "x64"; /* For users of `Smalltalk os platformSubtype`. */
        }
        platformProcessorType = asByteString(value);

        /*
         * Start with "Croquet" to let `LanguageEnvironment win32VMUsesUnicode` return `true`. Add
         * fake VMMaker info to make `Smalltalk vmVMMakerVersion` work.
         */
        vmVersion = asByteString("Croquet " + SqueakLanguageConfig.IMPLEMENTATION_NAME + " " + SqueakLanguageConfig.VERSION + " VMMaker.fn.9999");

        windowSystemName = asByteString("Aqua");

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
        switch (index) {
            case 0:
                return getVMPath();
            case 1:
                return getImagePath();
            case 1001:
                return getPlatformName();
            case 1002:
                return getOperatingSystemVersion();
            case 1003:
                return getPlatformProcessorType();
            case 1004:
                return getVMVersion();
            case 1005:
                return getWindowSystemName();
            case 1006:
                return getVmBuildId();
            case 1007:
                return getInterpreterClass();
            case 1008:
                return getSystemProperties();
            case 1009:
                return getVMInformation();
            case 1201:
                return getMaxFilenameLength();
            case 1202:
                return getFileLastError();
            case 10001:
                return getHardwareDetails();
            case 10002:
                return getOperatingSystemDetails();
            case 10003:
                return getGraphicsHardwareDetails();
            default:
                if (index >= 2 && index <= 1000) {
                    return getCMDArgument(index - 2);
                } else {
                    return NilObject.SINGLETON;
                }
        }
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
        try {
            final DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
            width = dm.getWidth();
            height = dm.getHeight();
        } catch (final HeadlessException e) {
            /* Report 0 x 0 in headless mode. */
        }
        return asByteString(String.format("Display Information: \n\tPrimary monitor resolution: %s x %s\n", width, height));
    }

    private NativeObject asByteString(final String value) {
        return image.asByteString(value);
    }
}
