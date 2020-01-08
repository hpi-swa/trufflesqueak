/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.interop;

import java.io.IOException;
import java.nio.charset.Charset;

import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public class SqueakFileDetector implements TruffleFile.FileTypeDetector {

    @Override
    public String findMimeType(final TruffleFile file) throws IOException {
        final String fileName = file.getName().toString();
        if (fileName.endsWith(".image")) {
            return SqueakLanguageConfig.MIME_TYPE;
        } else if (fileName.endsWith(".st")) {
            return SqueakLanguageConfig.ST_MIME_TYPE;
        }
        return null;
    }

    @Override
    public Charset findEncoding(final TruffleFile file) throws IOException {
        return null;
    }
}
