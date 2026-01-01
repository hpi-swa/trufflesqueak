/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.launcher;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

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
                transcriptBlock.execute(bytesToString(b));
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
                transcriptBlock.execute(bytesToString(Arrays.copyOfRange(b, off, off + len)));
            }
        } catch (final Exception e) {
            e.printStackTrace();
        } finally {
            super.write(b, off, len);
        }
    }

    private static String bytesToString(final byte[] b) {
        return StandardCharsets.UTF_8.decode(ByteBuffer.wrap(b)).toString();
    }
}
