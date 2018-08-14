package de.hpi.swa.graal.squeak.launcher;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Source.Builder;

import com.oracle.truffle.api.TruffleOptions;

import de.hpi.swa.graal.squeak.config.SqueakConfig;

public final class GraalSqueakLauncher extends AbstractLanguageLauncher {
    private SqueakConfig config;

    public static void main(final String[] arguments) throws RuntimeException {
        final String[] argumentsForLauncher;
        if (arguments.length >= 1) {
            argumentsForLauncher = arguments;
        } else {
            if (TruffleOptions.AOT) {
                argumentsForLauncher = new String[]{"--help"};
            } else {
                final String image = FileChooser.run();
                if (image != null) {
                    argumentsForLauncher = new String[]{image};
                } else {
                    argumentsForLauncher = new String[]{"--help"};
                }
            }
        }
        new GraalSqueakLauncher().launch(argumentsForLauncher);
    }

    @Override
    protected List<String> preprocessArguments(final List<String> arguments, final Map<String, String> polyglotOptions) {
        config = new SqueakConfig(arguments.toArray(new String[arguments.size()]));
        return config.getUnrecognized();
    }

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        contextBuilder.arguments(getLanguageId(), config.toStringArgs());
        try (Context ctx = contextBuilder.allowAllAccess(true).build()) {
            final Builder sourceBuilder = Source.newBuilder(getLanguageId(), new File(config.getImagePath()));
            sourceBuilder.interactive(true);
            ctx.eval(sourceBuilder.build());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getLanguageId() {
        return SqueakConfig.ID;
    }

    @Override
    protected String getMainClass() {
        return GraalSqueakLauncher.class.getName();
    }

    @Override
    protected void printHelp(final OptionCategory maxCategory) {
        // Checkstyle: stop
        System.out.println("usage: graalsqueak <image> [optional arguments]");
        System.out.println();
        System.out.println("optional arguments:");
        System.out.println("  --help                show this help message and exit");
        System.out.println("  --args                Squeak image arguments");
        System.out.println("  -d, --disable-interrupts");
        System.out.println("                        disable interrupt handler");
        System.out.println("  -m METHOD, --method METHOD");
        System.out.println("                        method selector when receiver is provided");
        System.out.println("  -r RECEIVER, --receiver RECEIVER");
        System.out.println("                        SmallInteger to be used as receiver");
        System.out.println("  -t, --trace           trace Squeak process switches, ...");
        System.out.println("  -v, --verbose         enable verbose output");
        // Checkstyle: resume
    }

    @Override
    protected void collectArguments(final Set<String> options) {
        options.addAll(Arrays.asList(
                        "-r",
                        "-m",
                        "--testing",
                        "--trace",
                        "--verbose",
                        "--args",
                        "--help"));
    }
}
