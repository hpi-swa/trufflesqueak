/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
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
    private static final String OPTION_IMAGE_PATH = SqueakLanguageConfig.ID + ".ImagePath";
    private static final String OPTION_HEADLESS = SqueakLanguageConfig.ID + ".Headless";
    private static final String OPTION_TRANSCRIPT_FORWARDING_FLAG = "--enable-transcript-forwarding";
    private static final String POLYGLOT_FLAG = "--polyglot";
    private String[] remainingArguments;
    private String imagePath = "Squeak.image";
    private String sourceCode = null;
    private boolean enableTranscriptForwarding = false;

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
            if (fileExists(arg)) {
                unrecognized = new ArrayList<>(arguments.subList(0, i));
                imagePath = Paths.get(arg).toAbsolutePath().toString();
                final List<String> remainingArgumentsList = arguments.subList(i + 1, arguments.size());
                remainingArguments = remainingArgumentsList.toArray(new String[remainingArgumentsList.size()]);
                break;
            } else if ("-c".equals(arg) || "--code".equals(arg)) {
                arguments.remove(i);
                sourceCode = arguments.get(i);
                arguments.remove(i);
                i--;
            } else if (OPTION_TRANSCRIPT_FORWARDING_FLAG.equals(arg)) {
                arguments.remove(i);
                enableTranscriptForwarding = true;
            }
        }
        return unrecognized;
    }

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        System.exit(execute(contextBuilder));
    }

    protected int execute(final Context.Builder contextBuilder) {
        contextBuilder.option(OPTION_IMAGE_PATH, imagePath);
        if (sourceCode != null) {
            contextBuilder.option(OPTION_HEADLESS, "true");
        }
        contextBuilder.arguments(getLanguageId(), remainingArguments);
        contextBuilder.allowAllAccess(true);
        final SqueakTranscriptForwarder out;
        final SqueakTranscriptForwarder err;
        if (enableTranscriptForwarding) {
            out = new SqueakTranscriptForwarder(System.out, true);
            contextBuilder.out(out);
            err = new SqueakTranscriptForwarder(System.err, true);
            contextBuilder.err(err);
        } else {
            out = null;
            err = null;
        }
        try (Context context = contextBuilder.build()) {
            println("[graalsqueak] Running %s on %s...", SqueakLanguageConfig.NAME, getRuntimeName());
            if (sourceCode != null) {
                final Object result = context.eval(
                                Source.newBuilder(getLanguageId(), sourceCode, "Compiler>>#evaluate:").internal(true).cached(false).mimeType(SqueakLanguageConfig.ST_MIME_TYPE).build());
                println("[graalsqueak] Result: %s", result);
                return 0;
            } else {
                final Value image = context.eval(Source.newBuilder(getLanguageId(), new File(imagePath)).internal(true).cached(false).mimeType(SqueakLanguageConfig.MIME_TYPE).build());
                if (out != null || err != null) {
                    out.setUp(context);
                    err.setUp(context);
                }
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

    @Override
    protected String[] getDefaultLanguages() {
        return new String[0]; // Allow all languages (similar to `--polyglot`)
    }

    @Override
    protected VMType getDefaultVMType() {
        return VMType.JVM;
    }

    private static boolean fileExists(final String path) {
        try {
            return Files.exists(Paths.get(path));
        } catch (final Exception e) {
            return false;
        }
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
