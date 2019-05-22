package de.hpi.swa.graal.squeak.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class SqueakTranscriptForwarder extends OutputStream {
    private static final String TRANSCRIPT_BLOCK_CODE = "[ :s | Transcript nextPutAll: s; flush ]";
    private static final String TRANSCRIPT_BLOCK_CODE_NAME = "<transcript forwarder>";
    @CompilationFinal private static Value transcriptBlock;

    private final OutputStream delegate;

    public SqueakTranscriptForwarder(final OutputStream defaultTarget) {
        delegate = defaultTarget;
    }

    public static void setUp(final Context context) throws IOException {
        CompilerDirectives.transferToInterpreterAndInvalidate();
        transcriptBlock = context.eval(Source.newBuilder(SqueakLanguageConfig.ID, TRANSCRIPT_BLOCK_CODE, TRANSCRIPT_BLOCK_CODE_NAME).build());
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
