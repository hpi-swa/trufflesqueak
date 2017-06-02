package de.hpi.swa.trufflesqueak;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.Arrays;

import javax.swing.JFileChooser;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;
import com.oracle.truffle.api.vm.PolyglotEngine.Builder;
import com.oracle.truffle.api.vm.PolyglotEngine.Value;

public class TruffleSqueakMain {
    private static void executeImage(String filename, InputStream in, PrintStream out, String... args) throws RuntimeException {
        out.println("== running TruffleSqueak on " + Truffle.getRuntime().getName());
        Source source = Source.newBuilder(filename).mimeType(SqueakLanguage.MIME_TYPE).name(filename).interactive().build();
        Builder builder = PolyglotEngine.newBuilder().setIn(in).setOut(out);
        builder.config(SqueakLanguage.MIME_TYPE, "config", new SqueakConfig(args));
        PolyglotEngine engine = builder.build();
        assert engine.getLanguages().containsKey(SqueakLanguage.MIME_TYPE);
        Value result = engine.eval(source);
        if (result.get() instanceof Integer) {
            System.exit((int) result.get());
        }
    }

    public static void main(String[] args) throws RuntimeException {
        if (args.length >= 1) {
            executeImage(args[0], System.in, System.out, Arrays.copyOfRange(args, 1, args.length));
        } else {
            JFileChooser squeakImageChooser = new JFileChooser();
            int result = squeakImageChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                executeImage(squeakImageChooser.getSelectedFile().getAbsolutePath(),
                                System.in, System.out, args);
            }
        }
    }
}
