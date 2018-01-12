package de.hpi.swa.trufflesqueak;

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
import com.oracle.truffle.api.TruffleRuntime;

public class TruffleSqueakMain extends AbstractLanguageLauncher {
    private static void executeImage(String... args) throws RuntimeException {
        TruffleRuntime runtime = Truffle.getRuntime();
        System.out.println(String.format("== Running %s on %s ==", SqueakLanguage.NAME, runtime.getName()));
        new TruffleSqueakMain().launch(args);
    }

    public static void main(String[] args) throws RuntimeException {
        if (args.length >= 1) {
            executeImage(args);
        } else {
            JFileChooser squeakImageChooser = new JFileChooser();
            int result = squeakImageChooser.showOpenDialog(null);
            if (result == JFileChooser.APPROVE_OPTION) {
                executeImage(squeakImageChooser.getSelectedFile().getAbsolutePath());
            }
        }
    }

    private SqueakConfig config;

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        config = new SqueakConfig(arguments.toArray(new String[0]));
        return config.getUnrecognized();
    }

    @Override
    protected void launch(Context.Builder contextBuilder) {
        contextBuilder.arguments(getLanguageId(), config.toStringArgs());
        try (Context ctx = contextBuilder.build()) {
            Builder sourceBuilder = Source.newBuilder(getLanguageId(), new File(config.getImagePath()));
            sourceBuilder.interactive(true);
            ctx.eval(sourceBuilder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getLanguageId() {
        return SqueakLanguage.NAME.toLowerCase();
    }

    @Override
    protected void printHelp(OptionCategory maxCategory) {
        System.out.println("squeak <image> [-r <receiver>] [-m <method>] [-t|--trace] [-v|--verbose] [--|--args <image args>]");
    }

    @Override
    protected void collectArguments(Set<String> options) {
        options.add("-r");
        options.add("-m");
        options.add("--trace");
        options.add("--verbose");
        options.add("--args");
    }
}
