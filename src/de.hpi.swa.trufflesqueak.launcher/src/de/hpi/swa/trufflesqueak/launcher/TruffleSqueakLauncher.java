/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.launcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.humbleui.jwm.App;

import org.graalvm.launcher.AbstractLanguageLauncher;
import org.graalvm.maven.downloader.Main;
import org.graalvm.options.OptionCategory;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

import de.hpi.swa.trufflesqueak.shared.SqueakImageLocator;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageOptions;

public final class TruffleSqueakLauncher extends AbstractLanguageLauncher {
    private static final String ENGINE_MODE_OPTION = "engine.Mode";
    private static final String ENGINE_MODE_LATENCY = "latency";

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private boolean headless;
    private boolean printImagePath;
    private boolean quiet;
    private String[] imageArguments = new String[0];
    private String imagePath;
    private String sourceCode;
    private boolean enableTranscriptForwarding;
    private boolean useEngineModeLatency = true;

    public static void main(final String[] arguments) throws RuntimeException {
        new TruffleSqueakLauncher().launch(arguments);
    }

    @Override
    protected List<String> preprocessArguments(final List<String> arguments, final Map<String, String> polyglotOptions) {
        final String launcherName = System.getProperty("org.graalvm.launcher.executablename", "trufflesqueak");
        if (launcherName.endsWith("trufflesqueak-polyglot-get") || launcherName.endsWith("trufflesqueak-polyglot-get.exe")) {
            if (isAOT()) {
                throw abort("trufflesqueak-polyglot-get is not available in a native standalone. Please try again with a JVM standalone of TruffleSqueak.");
            } else {
                polyglotGet(arguments);
            }
        }

        final List<String> unrecognized = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            final String arg = arguments.get(i);
            if (isExistingImageFile(arg)) {
                imagePath = Paths.get(arg).toAbsolutePath().toString();
                imageArguments = getRemainingArguments(arguments, i);
                break;
            } else if ("--".equals(arg)) {
                imageArguments = getRemainingArguments(arguments, i);
                break;
            } else if (SqueakLanguageOptions.CODE_FLAG.equals(arg) || SqueakLanguageOptions.CODE_FLAG_SHORT.equals(arg)) {
                sourceCode = arguments.get(++i);
                headless = true;
            } else if (SqueakLanguageOptions.HEADLESS_FLAG.equals(arg)) {
                headless = true;
            } else if (SqueakLanguageOptions.PRINT_IMAGE_PATH_FLAG.equals(arg)) {
                printImagePath = true;
            } else if (SqueakLanguageOptions.QUIET_FLAG.equals(arg)) {
                quiet = true;
            } else if (SqueakLanguageOptions.TRANSCRIPT_FORWARDING_FLAG.equals(arg)) {
                enableTranscriptForwarding = true;
            } else {
                if (arg.contains(ENGINE_MODE_OPTION)) {
                    useEngineModeLatency = false; // engine.Mode set explicitly
                }
                unrecognized.add(arg);
            }
        }
        return unrecognized;
    }

    private static String[] getRemainingArguments(final List<String> arguments, final int index) {
        return arguments.subList(index + 1, arguments.size()).toArray(new String[0]);
    }

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        if (headless) {
            System.exit(execute(contextBuilder));
        } else {
            // Give JWM the Main Thread to run the native OS event loop
            App.start(() -> {

                // Run the Squeak VM in a background thread
                final Thread squeakVMThread = new Thread(() -> {
                    int exitCode = -1; // Assume crash by default

                    try {
                        // Execute the Squeak image
                        exitCode = execute(contextBuilder);
                    } catch (Throwable t) {
                        // Log the fatal crash through the GraalVM launcher framework
                        throw abort(t);
                    } finally {
                        // GUARANTEED TO RUN: When Squeak exits or crashes, tell the JWM window to close
                        App.runOnUIThread(App::terminate);
                    }

                    // Shut down the JVM completely (only reached if no exception was thrown)
                    System.exit(exitCode);

                }, "SqueakVM-Thread");

                squeakVMThread.start();
            });
        }
    }

    private int execute(final Context.Builder contextBuilder) {
        imagePath = SqueakImageLocator.findImage(imagePath, quiet);
        if (printImagePath) {
            println(imagePath);
            return 0;
        }
        final String runtimeName = getRuntimeName();
        // only ever use latency on Graal
        useEngineModeLatency = useEngineModeLatency && runtimeName.contains("Graal");
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.IMAGE_PATH, imagePath);
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.HEADLESS, Boolean.toString(headless));
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.QUIET, Boolean.toString(quiet));
        contextBuilder.arguments(getLanguageId(), imageArguments);
        if (useEngineModeLatency) {
            contextBuilder.option(ENGINE_MODE_OPTION, ENGINE_MODE_LATENCY);
        }
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
            if (!quiet) {
                final String engineModeSuffix = useEngineModeLatency ? " (" + ENGINE_MODE_LATENCY + " mode)" : "";
                println(String.format("[trufflesqueak] Running %s on %s%s...", new File(imagePath).getName(), runtimeName, engineModeSuffix));
            }
            if (sourceCode != null) {
                if (!quiet) {
                    println(String.format("[trufflesqueak] Evaluating '%s'...", sourceCode));
                }
                final Value result = context.eval(
                                Source.newBuilder(getLanguageId(), sourceCode, "Compiler>>#evaluate:").internal(true).cached(false).mimeType(SqueakLanguageConfig.ST_MIME_TYPE).build());
                if (!quiet) {
                    println("[trufflesqueak] Result: " + (result.isString() ? result.asString() : result.toString()));
                }
                return 0;
            } else {
                final Value image = context.eval(
                                Source.newBuilder(getLanguageId(), imagePath, SqueakLanguageConfig.IMAGE_SOURCE_NAME).internal(true).cached(false).mimeType(SqueakLanguageConfig.MIME_TYPE).build());
                if (out != null && err != null) {
                    out.setUp(context);
                    err.setUp(context);
                }
                image.execute();
                throw abort("A Squeak/Smalltalk image cannot return a result, it can only exit.");
            }
        } catch (final IllegalArgumentException e) {
            if (e.getMessage().contains("Could not find option with name " + SqueakLanguageConfig.ID)) {
                final String thisPackageName = getClass().getPackage().getName();
                final String parentPackageName = thisPackageName.substring(0, thisPackageName.lastIndexOf('.'));
                throw abort(String.format("Failed to load TruffleSqueak. Please ensure '%s' is on the Java class path.", parentPackageName));
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
        return TruffleSqueakLauncher.class.getName();
    }

    @Override
    protected String[] getDefaultLanguages() {
        return EMPTY_STRING_ARRAY; // Allow all languages (same effect of `--polyglot`)
    }

    @Override
    protected void printHelp(final OptionCategory maxCategory) {
        println("Usage: trufflesqueak [options] <image file> [image arguments]\n");
        println("Basic options:");
        launcherOption(SqueakLanguageOptions.CODE_FLAG + " \"<code>\", " + SqueakLanguageOptions.CODE_FLAG_SHORT + " \"<code>\"", SqueakLanguageOptions.CODE_HELP);
        launcherOption(SqueakLanguageOptions.TRANSCRIPT_FORWARDING_FLAG, SqueakLanguageOptions.TRANSCRIPT_FORWARDING_HELP);
        launcherOption(SqueakLanguageOptions.HEADLESS_FLAG, SqueakLanguageOptions.HEADLESS_HELP);
        launcherOption(SqueakLanguageOptions.PRINT_IMAGE_PATH_FLAG, SqueakLanguageOptions.PRINT_IMAGE_PATH_HELP);
        launcherOption(SqueakLanguageOptions.QUIET_FLAG, SqueakLanguageOptions.QUIET_HELP);
    }

    @Override
    protected void collectArguments(final Set<String> options) {
        options.addAll(List.of(SqueakLanguageOptions.CODE_FLAG, SqueakLanguageOptions.CODE_FLAG_SHORT, SqueakLanguageOptions.HEADLESS_FLAG,
                        SqueakLanguageOptions.QUIET_FLAG, SqueakLanguageOptions.PRINT_IMAGE_PATH_FLAG, SqueakLanguageOptions.RESOURCE_SUMMARY_FLAG, SqueakLanguageOptions.TRANSCRIPT_FORWARDING_FLAG));
    }

    private static boolean isExistingImageFile(final String fileName) {
        try {
            return fileName.endsWith(".image") && Files.exists(Paths.get(fileName));
        } catch (final SecurityException | InvalidPathException e) {
            return false;
        }
    }

    /** See LanguageLauncherBase#getTempEngine(). */
    private static String getRuntimeName() {
        try (Engine engine = Engine.newBuilder().useSystemProperties(false).//
                        out(OutputStream.nullOutputStream()).//
                        err(OutputStream.nullOutputStream()).//
                        option("engine.WarnInterpreterOnly", "false").//
                        build()) {
            return engine.getImplementationName();
        }
    }

    /* Maven Downloader support */

    private static void polyglotGet(final List<String> arguments) {
        final String smalltalkHome = getPropertyOrFail("org.graalvm.language.smalltalk.home");
        final String outputDir = smalltalkHome + File.separator + "modules";
        final List<String> args = new ArrayList<>();
        args.add("-o");
        args.add(outputDir);
        args.add("-v");
        args.add(getPropertyOrFail("org.graalvm.version"));
        if (arguments.size() == 1 && !arguments.getFirst().startsWith("-")) {
            args.add("-a");
        }
        args.addAll(arguments);
        try {
            Main.main(args.toArray(EMPTY_STRING_ARRAY));
        } catch (Exception e) {
            throw new Error(e);
        }
        System.exit(0);
    }

    private static String getPropertyOrFail(final String property) {
        final String value = System.getProperty(property);
        if (value == null) {
            throw new UnsupportedOperationException("Expected system property '" + property + "' to be set");
        }
        return value;
    }
}
