/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;

import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageOptions;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

@Option.Group(SqueakLanguageConfig.ID)
public final class SqueakOptions {

    @Option(name = SqueakLanguageOptions.IMAGE_PATH, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.IMAGE_PATH_HELP, usageSyntax = "path/to/your.image")//
    public static final OptionKey<String> ImagePath = new OptionKey<>("");

    @Option(name = SqueakLanguageOptions.IMAGE_ARGUMENTS, category = OptionCategory.USER, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.IMAGE_ARGUMENTS_HELP, usageSyntax = "'arg1 arg2 ...'")//
    public static final OptionKey<String> ImageArguments = new OptionKey<>("");

    @Option(name = SqueakLanguageOptions.HEADLESS, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.HEADLESS_HELP, usageSyntax = "true|false")//
    public static final OptionKey<Boolean> Headless = new OptionKey<>(true);

    @Option(name = SqueakLanguageOptions.INTERCEPT_MESSAGES, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.INTERCEPT_MESSAGES_HELP, //
                    usageSyntax = "'Object>>becomeForward:,Behavior>>allInstances,...'")//
    public static final OptionKey<String> InterceptMessages = new OptionKey<>("");

    @Option(name = SqueakLanguageOptions.QUIET, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.QUIET_HELP, usageSyntax = "false|true")//
    public static final OptionKey<Boolean> Quiet = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.INTERRUPTS, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.INTERRUPTS_HELP, usageSyntax = "false|true")//
    public static final OptionKey<Boolean> Interrupts = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.RESOURCE_SUMMARY, category = OptionCategory.USER, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.RESOURCE_SUMMARY_HELP, usageSyntax = "false|true")//
    public static final OptionKey<Boolean> ResourceSummary = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.CONTEXT_STACK_DEPTH, category = OptionCategory.USER, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.CONTEXT_STACK_DEPTH_HELP, usageSyntax = "number")//
    public static final OptionKey<Integer> ContextStackDepth = new OptionKey<>(2 << 12);

    @Option(name = SqueakLanguageOptions.SIGNAL_INPUT_SEMAPHORE, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.SIGNAL_INPUT_SEMAPHORE_HELP, usageSyntax = "false|true")//
    public static final OptionKey<Boolean> SignalInputSemaphore = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.STARTUP, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.STARTUP_HELP, usageSyntax = "false|true")//
    public static final OptionKey<Boolean> Startup = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.TESTING, category = OptionCategory.INTERNAL, stability = OptionStability.STABLE, help = SqueakLanguageOptions.TESTING_HELP, usageSyntax = "false|true")//
    public static final OptionKey<Boolean> Testing = new OptionKey<>(false);

    private SqueakOptions() { // no instances
    }

    public static OptionDescriptors createDescriptors() {
        return new SqueakOptionsOptionDescriptors();
    }

    public record SqueakContextOptions(String imagePath, String[] imageArguments, boolean printResourceSummary, boolean isHeadless, boolean disableInterruptHandler,
                    int maxContextStackDepth, boolean disableStartup, boolean isTesting, boolean signalInputSemaphore) {
        public static SqueakContextOptions create(final OptionValues options) {
            return new SqueakContextOptions(
                            options.get(ImagePath).isEmpty() ? null : options.get(ImagePath),
                            options.get(ImageArguments).isEmpty() ? ArrayUtils.EMPTY_STRINGS_ARRAY : options.get(ImageArguments).split(","),
                            options.get(ResourceSummary),
                            options.get(Headless),
                            options.get(Interrupts),
                            Math.max(0, options.get(ContextStackDepth)),
                            options.get(Startup),
                            options.get(Testing),
                            options.get(SignalInputSemaphore));
        }
    }
}
