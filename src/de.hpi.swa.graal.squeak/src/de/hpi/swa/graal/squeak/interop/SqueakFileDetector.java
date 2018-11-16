package de.hpi.swa.graal.squeak.interop;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public class SqueakFileDetector extends FileTypeDetector {
    @Override
    public String probeContentType(final Path path) throws IOException {
        if (path.toString().endsWith(".image")) {
            return SqueakLanguageConfig.MIME_TYPE;
        } else {
            return SqueakLanguageConfig.ST_MIME_TYPE;
        }
    }
}
