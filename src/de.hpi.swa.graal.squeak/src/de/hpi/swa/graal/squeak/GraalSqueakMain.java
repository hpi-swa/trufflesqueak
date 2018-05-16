package de.hpi.swa.graal.squeak;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Source.Builder;

import com.oracle.truffle.api.Truffle;

public class GraalSqueakMain extends AbstractLanguageLauncher {
    private SqueakConfig config;

    public static void main(final String[] args) throws RuntimeException {
        if (args.length >= 1) {
            executeImage(args);
        } else {
            final JFileChooser squeakImageChooser = new JFileChooser();
            final long result = squeakImageChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                executeImage(squeakImageChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private static void executeImage(final String... args) throws RuntimeException {
        // Checkstyle: stop
        System.out.println("== Running " + SqueakLanguage.NAME + " on " + Truffle.getRuntime().getName() + " ==");
        // Checkstyle: resume
        new GraalSqueakMain().launch(args);
    }

    @Override
    protected List<String> preprocessArguments(final List<String> arguments, final Map<String, String> polyglotOptions) {
        config = new SqueakConfig(arguments.toArray(new String[0]));
        return config.getUnrecognized();
    }

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        contextBuilder.arguments(getLanguageId(), config.toStringArgs());
        try (Context ctx = contextBuilder.build()) {
            final Builder sourceBuilder = Source.newBuilder(getLanguageId(), new File(config.getImagePath()));
            sourceBuilder.interactive(true);
            ctx.eval(sourceBuilder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getLanguageId() {
        return SqueakLanguage.ID;
    }

    @Override
    protected void printHelp(final OptionCategory maxCategory) {
        // Checkstyle: stop
        System.out.println("squeak <image> [-r <receiver>] [-m <method>] [-t|--trace] [-v|--verbose] [--|--args <image args>]");
        // Checkstyle: resume
    }

    @Override
    protected void collectArguments(final Set<String> options) {
        options.add("-r");
        options.add("-m");
        options.add("--trace");
        options.add("--verbose");
        options.add("--args");
    }
}
