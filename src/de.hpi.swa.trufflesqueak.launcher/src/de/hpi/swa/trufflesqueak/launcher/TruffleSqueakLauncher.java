/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.launcher;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.graalvm.launcher.AbstractLanguageLauncher;
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
        final List<String> unrecognized = new ArrayList<>();
        for (int i = 0; i < arguments.size(); i++) {
            final String arg = arguments.get(i);
            if (isExistingImageFile(arg)) {
                imagePath = Paths.get(arg).toAbsolutePath().toString();
                final List<String> remainingArguments = arguments.subList(i + 1, arguments.size());
                imageArguments = remainingArguments.toArray(new String[0]);
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

    @Override
    protected void launch(final Context.Builder contextBuilder) {
        System.exit(execute(contextBuilder));
    }

    protected int execute(final Context.Builder contextBuilder) {
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
                final Value result = context.eval(
                                Source.newBuilder(getLanguageId(), sourceCode, "Compiler>>#evaluate:").internal(true).cached(false).mimeType(SqueakLanguageConfig.ST_MIME_TYPE).build());
                if (!quiet) {
                    println("[trufflesqueak] Result: " + (result.isString() ? result.asString() : result.toString()));
                }
                return 0;
            } else {
                final Value image = context.eval(Source.newBuilder(getLanguageId(), new File(imagePath)).internal(true).cached(false).mimeType(SqueakLanguageConfig.MIME_TYPE).build());
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
        options.addAll(Arrays.asList(SqueakLanguageOptions.CODE_FLAG, SqueakLanguageOptions.CODE_FLAG_SHORT, SqueakLanguageOptions.HEADLESS_FLAG,
                        SqueakLanguageOptions.QUIET_FLAG, SqueakLanguageOptions.PRINT_IMAGE_PATH_FLAG, SqueakLanguageOptions.RESOURCE_SUMMARY_FLAG, SqueakLanguageOptions.TRANSCRIPT_FORWARDING_FLAG));
    }

    @Override
    protected String[] getDefaultLanguages() {
        return new String[0]; // Allow all languages (same effect of `--polyglot`)
    }

    @Override
    protected VMType getDefaultVMType() {
        return VMType.JVM;
    }

    private static boolean isExistingImageFile(final String fileName) {
        try {
            return fileName.endsWith(".image") && Files.exists(Paths.get(fileName));
        } catch (final SecurityException | InvalidPathException e) {
            return false;
        }
    }

    private static String getRuntimeName() {
        try (Engine engine = Engine.create()) {
            return engine.getImplementationName();
        }
    }
}
