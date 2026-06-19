/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

import java.net.URI;

public final class SqueakLanguageConfig {
    public static final String ID = "smalltalk";
    public static final String IMAGE_SOURCE_NAME = "<smalltalk image>";
    public static final String IMPLEMENTATION_NAME = "TruffleSqueak";
    public static final String MIME_TYPE = "application/x-smalltalk";
    public static final String NAME = "Squeak/Smalltalk";
    public static final String ST_MIME_TYPE = "text/x-smalltalk";
    public static final String VERSION = "25.0.2"; // sync with Truffle import
    public static final String WEBSITE = "https://github.com/hpi-swa/trufflesqueak";
    private static final String IMAGE_VERSION = "25.0.1"; // on release: `VERSION;`

    public record SupportedImage(String id, String name, String url) {
        public static SupportedImage url(final String url) {
            final String name = extractName(url);
            return new SupportedImage(name, name, url);
        }

        private static String extractName(final String url) {
            final String path = URI.create(url).getPath();
            final int lastSlash = path.lastIndexOf('/') + 1;
            final String fileName = path.substring(lastSlash);
            final int lastDot = fileName.lastIndexOf('.');
            return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        }
    }

    public static final SupportedImage[] SUPPORTED_IMAGES = {
                    new SupportedImage("trufflesqueak", "TruffleSqueak image (" + IMAGE_VERSION + ") (recommended)",
                                    "https://github.com/hpi-swa/trufflesqueak/releases/download/" + IMAGE_VERSION + "/TruffleSqueakImage-" + IMAGE_VERSION + ".zip"),
                    new SupportedImage("squeak-6.0", "Squeak/Smalltalk (6.0-22148)", "https://files.squeak.org/6.0/Squeak6.0-22148-64bit/Squeak6.0-22148-64bit.zip"),
                    new SupportedImage("trufflesqueak-test", "TruffleSqueak test image (6.0-22104)",
                                    "https://github.com/hpi-swa/trufflesqueak/releases/download/22.3.0/TruffleSqueakTestImage-6.0-22104-64bit.zip"),
                    new SupportedImage("cuis-7.3-test", "Cuis Smalltalk test image (7.3-7036)", "https://github.com/hpi-swa/trufflesqueak/releases/download/24.1.2/CuisTestImage-7.3-7036.zip")};

    private SqueakLanguageConfig() {
    }
}
