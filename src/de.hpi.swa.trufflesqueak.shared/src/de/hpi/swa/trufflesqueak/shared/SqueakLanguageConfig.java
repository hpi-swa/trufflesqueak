/*
 * Copyright (c) 2017-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
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
    public static final String VERSION = "24.0.2";
    public static final String WEBSITE = "https://github.com/hpi-swa/trufflesqueak";

    public static final String[][] SUPPORTED_IMAGES = {
                    {"TruffleSqueak image (24.0.0) (recommended)", "https://github.com/hpi-swa/trufflesqueak/releases/download/24.0.2/TruffleSqueakImage-24.0.0.zip"},
                    {"Squeak/Smalltalk (6.0-22104)", "https://files.squeak.org/6.0/Squeak6.0-22104-64bit/Squeak6.0-22104-64bit.zip"},
                    {"TruffleSqueak test image (6.0-22104)", "https://github.com/hpi-swa/trufflesqueak/releases/download/22.3.0/TruffleSqueakTestImage-6.0-22104-64bit.zip"},
                    {"Cuis-Smalltalk test image (6.0-5053)", "https://github.com/hpi-swa/trufflesqueak/releases/download/21.3.0/CuisTestImage-6.0-5053.zip"}};

    private SqueakLanguageConfig() {
    }
}
