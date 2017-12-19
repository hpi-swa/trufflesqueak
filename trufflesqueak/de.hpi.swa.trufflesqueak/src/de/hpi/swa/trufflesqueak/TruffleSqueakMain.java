package de.hpi.swa.trufflesqueak;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFileChooser;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;

import com.oracle.truffle.api.Truffle;

public class TruffleSqueakMain extends AbstractLanguageLauncher {
    private static void executeImage(String... args) throws RuntimeException {
        System.out.println(String.format("== Running TruffleSqueak on %s ==", Truffle.getRuntime().getName()));
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
    private String imagepath;

    @Override
    protected List<String> preprocessArguments(List<String> arguments, Map<String, String> polyglotOptions) {
        imagepath = arguments.get(0);
        config = new SqueakConfig(arguments.subList(1, arguments.size()).toArray(new String[0]));
        return config.getUnrecognized();
    }

    @Override
    protected void launch(org.graalvm.polyglot.Context.Builder contextBuilder) {
        contextBuilder.arguments("squeaksmalltalk", config.toStringArgs());
        try (org.graalvm.polyglot.Context ctx = contextBuilder.build()) {
            Object result = ctx.eval(org.graalvm.polyglot.Source.newBuilder("squeaksmalltalk", new File(imagepath)).build());
            System.out.println(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getLanguageId() {
        return "squeaksmalltalk";
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
