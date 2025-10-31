/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

public final class SqueakLanguageConfig {
    public static final String ID = "smalltalk";
    public static final String IMAGE_SOURCE_NAME = "<smalltalk image>";
    public static final String IMPLEMENTATION_NAME = "TruffleSqueak";
    public static final String MIME_TYPE = "application/x-smalltalk";
    public static final String NAME = "Squeak/Smalltalk";
    public static final String ST_MIME_TYPE = "text/x-smalltalk";
    public static final String VERSION = "25.0.1"; // sync with Truffle import
    public static final String WEBSITE = "https://github.com/hpi-swa/trufflesqueak";
    private static final String IMAGE_VERSION = VERSION; // on release: `VERSION;`

    public static final String[][] SUPPORTED_IMAGES = {
                    {"TruffleSqueak image (" + IMAGE_VERSION + ") (recommended)",
                                    "https://github.com/hpi-swa/trufflesqueak/releases/download/" + IMAGE_VERSION + "/TruffleSqueakImage-" + IMAGE_VERSION + ".zip"},
                    {"Squeak/Smalltalk (6.0-22148)", "https://files.squeak.org/6.0/Squeak6.0-22148-64bit/Squeak6.0-22148-64bit.zip"},
                    {"TruffleSqueak test image (6.0-22104)", "https://github.com/hpi-swa/trufflesqueak/releases/download/22.3.0/TruffleSqueakTestImage-6.0-22104-64bit.zip"},
                    {"Cuis Smalltalk test image (7.3-7036)", "https://github.com/hpi-swa/trufflesqueak/releases/download/24.1.2/CuisTestImage-7.3-7036.zip"}};

    private SqueakLanguageConfig() {
    }
}
