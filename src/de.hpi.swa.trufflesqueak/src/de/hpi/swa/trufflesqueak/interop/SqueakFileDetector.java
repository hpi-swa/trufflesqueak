/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;

import com.oracle.truffle.api.TruffleFile;

import de.hpi.swa.trufflesqueak.image.SqueakImageConstants;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

public final class SqueakFileDetector implements TruffleFile.FileTypeDetector {

    @Override
    public String findMimeType(final TruffleFile file) {
        final String fileName = file.getName();
        if (fileName == null) {
            return null;
        } else if (fileName.endsWith(".image")) {
            return isSupportedImage(file) ? SqueakLanguageConfig.MIME_TYPE : null;
        } else if (fileName.endsWith(".st")) {
            return SqueakLanguageConfig.ST_MIME_TYPE;
        } else {
            return null;
        }
    }

    /* Other formats use the .image extension, too (e.g., disk images), so check the image format word. */
    private static boolean isSupportedImage(final TruffleFile file) {
        try (InputStream stream = file.newInputStream()) {
            final byte[] bytes = stream.readNBytes(Integer.BYTES);
            if (bytes.length < Integer.BYTES) {
                return false;
            }
            final int format = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).getInt();
            return ArrayUtils.contains(SqueakImageConstants.SUPPORTED_IMAGE_FORMATS, format);
        } catch (final IOException | SecurityException | UnsupportedOperationException e) {
            return false;
        }
    }

    @Override
    public Charset findEncoding(final TruffleFile file) {
        return null;
    }
}
