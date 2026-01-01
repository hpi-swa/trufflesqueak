/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import java.nio.charset.Charset;

import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

public final class SqueakFileDetector implements TruffleFile.FileTypeDetector {

    @Override
    public String findMimeType(final TruffleFile file) {
        final String fileName = file.getName();
        if (fileName == null) {
            return null;
        } else if (fileName.endsWith(".image")) {
            return SqueakLanguageConfig.MIME_TYPE;
        } else if (fileName.endsWith(".st")) {
            return SqueakLanguageConfig.ST_MIME_TYPE;
        } else {
            return null;
        }
    }

    @Override
    public Charset findEncoding(final TruffleFile file) {
        return null;
    }
}
