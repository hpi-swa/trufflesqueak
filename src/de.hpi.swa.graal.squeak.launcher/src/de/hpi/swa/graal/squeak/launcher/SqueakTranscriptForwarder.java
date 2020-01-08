/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class SqueakTranscriptForwarder extends PrintStream {
    private static final String TRANSCRIPT_BLOCK_CODE = "[ :s | Transcript nextPutAll: s; flush ]";
    private static final String TRANSCRIPT_BLOCK_CODE_NAME = "<transcript forwarder>";

    private Value transcriptBlock;

    public SqueakTranscriptForwarder(final OutputStream out, final boolean autoFlush) {
        super(out, autoFlush);
    }

    public void setUp(final Context context) throws IOException {
        transcriptBlock = context.eval(Source.newBuilder(SqueakLanguageConfig.ID, TRANSCRIPT_BLOCK_CODE, TRANSCRIPT_BLOCK_CODE_NAME).build());
    }

    @Override
    public void write(final byte[] b) throws IOException {
        try {
            if (transcriptBlock != null) {
                transcriptBlock.execute(new String(b));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            super.write(b);
        }
    }

    @Override
    public void write(final byte[] b, final int off, final int len) {
        try {
            if (transcriptBlock != null) {
                transcriptBlock.execute(new String(Arrays.copyOfRange(b, off, off + len)));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            super.write(b, off, len);
        }
    }
}
