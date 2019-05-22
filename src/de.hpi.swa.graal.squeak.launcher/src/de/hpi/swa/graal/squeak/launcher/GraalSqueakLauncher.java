package de.hpi.swa.graal.squeak.launcher;

import java.io.File;
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
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.TruffleOptions;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

public final class GraalSqueakLauncher extends AbstractLanguageLauncher {
    private static final String POLYGLOT_FLAG = "--polyglot";
    private String[] remainingArguments;
    private String imagePath = "Squeak.image";
    private String sourceCode = null;

    public static void main(final String[] arguments) throws RuntimeException {
        final String[] argumentsForLauncher;
        if (arguments.length > 1 || arguments.length == 1 && !POLYGLOT_FLAG.equals(arguments[0])) {
            argumentsForLauncher = arguments;
        } else {
            if (TruffleOptions.AOT) {
                argumentsForLauncher = new String[]{"--help"};
            } else {
                final String image = FileChooser.run();
                if (image != null) {
                    argumentsForLauncher = new String[]{POLYGLOT_FLAG, image};
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
                final List<String> remainingArgumentsList = arguments.subList(i + 1, arguments.size());
                remainingArguments = remainingArgumentsList.toArray(new String[remainingArgumentsList.size()]);
                break;
            }
            if ("-c".equals(arg) || "--code".equals(arg)) {
                arguments.remove(i);
                sourceCode = arguments.get(i);
                arguments.remove(i);
                i--;
            }
        }
        return unrecognized;
    }

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        System.exit(execute(contextBuilder));
    }

    protected int execute(final Context.Builder contextBuilder) {
        contextBuilder.option(SqueakLanguageConfig.ID + ".ImagePath", imagePath);
        if (sourceCode != null) {
            contextBuilder.option(SqueakLanguageConfig.ID + ".Headless", "true");
        }
        contextBuilder.arguments(getLanguageId(), remainingArguments);
        contextBuilder.allowAllAccess(true);
        contextBuilder.out(new SqueakTranscriptForwarder(System.out));
        contextBuilder.err(new SqueakTranscriptForwarder(System.err));
        try (Context context = contextBuilder.build()) {
            println("[graalsqueak] Running %s on %s...", SqueakLanguageConfig.NAME, getRuntimeName());
            if (sourceCode != null) {
                final Object result = context.eval(
                                Source.newBuilder(getLanguageId(), sourceCode, "Compiler>>#evaluate:").internal(true).cached(false).mimeType(SqueakLanguageConfig.ST_MIME_TYPE).build());
                println("[graalsqueak] Result: %s", result);
                return 0;
            } else {
                final Value image = context.eval(Source.newBuilder(getLanguageId(), new File(imagePath)).internal(true).cached(false).mimeType(SqueakLanguageConfig.MIME_TYPE).build());
                SqueakTranscriptForwarder.setUp(context);
                image.execute();
                throw abort("A Squeak/Smalltalk image cannot return a result, it can only exit.");
            }
        } catch (final IllegalArgumentException e) {
            if (e.getMessage().contains("Could not find option")) {
                final String thisPackageName = getClass().getPackage().getName();
                final String parentPackageName = thisPackageName.substring(0, thisPackageName.lastIndexOf("."));
                throw abort(String.format("Failed to load GraalSqueak. Please ensure '%s' is on the Java class path.", parentPackageName));
            } else {
                throw e;
            }
        } catch (final PolyglotException e) {
            if (e.isExit()) {
                return e.getExitStatus();
            } else if (!e.isInternalError()) {
                e.printStackTrace();
                return -1;
            } else {
                throw e;
            }
        } catch (final IOException e) {
            throw abort(String.format("Error loading file '%s' (%s)", imagePath, e.getMessage()));
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
        println("usage: graalsqueak <image> [optional arguments]\n");
        println("optional arguments:");
        println("  -c CODE, --code CODE");
        println("                        Smalltalk code to be executed in headless mode");
    }

    @Override
    protected void collectArguments(final Set<String> options) {
        options.addAll(Arrays.asList("-c", "--code"));
    }

    private static void println(final String string, final Object... arguments) {
        // Checkstyle: stop
        System.out.println(String.format(string, arguments));
        // Checkstyle: resume
    }

    private static String getRuntimeName() {
        final String vmName = System.getProperty("java.vm.name", "unknown");
        final String mode = Truffle.getRuntime().getName().equals("Interpreted") ? "interpreted" : "Graal-compiled";
        return String.format("%s (%s)", vmName, mode);
    }
}
