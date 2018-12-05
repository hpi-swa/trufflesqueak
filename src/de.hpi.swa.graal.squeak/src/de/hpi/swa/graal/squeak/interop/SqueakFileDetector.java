package de.hpi.swa.graal.squeak.interop;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.spi.FileTypeDetector;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public class SqueakFileDetector extends FileTypeDetector {
    @Override
    public String probeContentType(final Path path) throws IOException {
        final String pathString = path.toString();
        if (pathString.endsWith(".image")) {
            return SqueakLanguageConfig.MIME_TYPE;
        } else if (pathString.endsWith(".st")) {
            return SqueakLanguageConfig.ST_MIME_TYPE;
        }
        return null;
    }
}
