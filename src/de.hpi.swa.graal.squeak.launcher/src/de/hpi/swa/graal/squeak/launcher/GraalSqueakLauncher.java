package de.hpi.swa.graal.squeak.launcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;

import com.oracle.truffle.api.TruffleOptions;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class GraalSqueakLauncher extends AbstractLanguageLauncher {
    private String imagePath;
    private List<String> remainingArgs;
    private String receiver = "1";
    private String selector = null;

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
        List<String> unrecognized = arguments;
        for (int i = 0; i < arguments.size(); i++) {
            final String arg = arguments.get(i);
            if (Files.exists(Paths.get(arg))) {
                unrecognized = arguments.subList(0, i);
                imagePath = Paths.get(arg).toAbsolutePath().toString();
                remainingArgs = arguments.subList(i + 1, arguments.size());
                break;
            }
            if ("-r".equals(arg) || "--receiver".equals(arg)) {
                arguments.remove(i);
                receiver = arguments.get(i);
                arguments.remove(i);
                i--;
            }
            if ("-m".equals(arg) || "--method".equals(arg)) {
                arguments.remove(i);
                selector = arguments.get(i);
                arguments.remove(i);
                i--;
            }
        }
        return unrecognized;
    }

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        contextBuilder.option(SqueakLanguageConfig.ID + ".ImagePath", imagePath);
        contextBuilder.arguments(getLanguageId(), remainingArgs.toArray(new String[remainingArgs.size()]));
        try (Context ctx = contextBuilder.allowAllAccess(true).build()) {
            if (selector != null) {
                ctx.eval(Source.create(getLanguageId(), receiver + ">>#" + selector));
            } else {
                ctx.eval(Source.newBuilder(getLanguageId(), "", "<interactive eval>").interactive(true).build());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected String getLanguageId() {
        return SqueakLanguageConfig.ID;
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
        System.out.println("  -m METHOD, --method METHOD");
        System.out.println("                        method selector when receiver is provided");
        System.out.println("  -r RECEIVER, --receiver RECEIVER");
        System.out.println("                        SmallInteger to be used as receiver");
        // Checkstyle: resume
    }

    @Override
    protected void collectArguments(final Set<String> options) {
        options.addAll(Arrays.asList(
                        "-r", "--receiver",
                        "-m", "--method"));
    }
}
