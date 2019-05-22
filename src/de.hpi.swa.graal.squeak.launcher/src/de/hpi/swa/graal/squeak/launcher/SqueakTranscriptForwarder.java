package de.hpi.swa.graal.squeak.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakTranscriptForwarder extends OutputStream {
    @CompilationFinal private static Value transcriptBlock;

    private final OutputStream delegate;

    public static void setTranscriptBlock(final Value transcriptBlockObject) {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        transcriptBlock = transcriptBlockObject;
    }

    public SqueakTranscriptForwarder(final OutputStream defaultTarget) {
        delegate = defaultTarget;
    }

    @Override
    public void write(final byte[] b) throws IOException {
        try {
            if (transcriptBlock != null) {
                transcriptBlock.execute(new String(b));
            }
        } finally {
            delegate.write(b);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) throws IOException {
        try {
            if (transcriptBlock != null) {
                transcriptBlock.execute(new String(Arrays.copyOfRange(b, off, off + len)));
            }
        } finally {
            delegate.write(b);
        }
    }

    @Override
    public void write(final int b) throws IOException {
        delegate.write(b); // Must be implemented, but ignored for now.
    }
}
