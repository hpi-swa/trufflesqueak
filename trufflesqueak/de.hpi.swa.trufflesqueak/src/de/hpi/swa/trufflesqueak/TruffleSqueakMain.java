package de.hpi.swa.trufflesqueak;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import javax.swing.JFileChooser;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;

public class TruffleSqueakMain {
    private static void executeImage(String filename, InputStream in, PrintStream out) throws IOException, RuntimeException {
        out.println("== running on " + Truffle.getRuntime().getName());

        Source source = Source.newBuilder(new File(filename)).mimeType(SqueakLanguage.MIME_TYPE).build();
        PolyglotEngine engine = PolyglotEngine.newBuilder().setIn(in).setOut(out).build();
        assert engine.getLanguages().containsKey(SqueakLanguage.MIME_TYPE);
        engine.eval(source);
    }

    public static void main(String[] args) throws IOException, RuntimeException {
        if (args.length == 1) {
            executeImage(args[0], System.in, System.out);
        } else {
            JFileChooser squeakImageChooser = new JFileChooser();
            int result = squeakImageChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                executeImage(squeakImageChooser.getSelectedFile().getAbsolutePath(),
                                System.in, System.out);
            }
        }
    }
}
