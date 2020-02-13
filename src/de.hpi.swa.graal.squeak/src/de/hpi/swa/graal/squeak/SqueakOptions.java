/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleLanguage.Env;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageOptions;

@Option.Group(SqueakLanguageConfig.ID)
public final class SqueakOptions {

    @Option(name = SqueakLanguageOptions.IMAGE_PATH, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.IMAGE_PATH_HELP)//
    public static final OptionKey<String> ImagePath = new OptionKey<>("");

    @Option(name = SqueakLanguageOptions.IMAGE_ARGUMENTS, category = OptionCategory.USER, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.IMAGE_ARGUMENTS_HELP)//
    public static final OptionKey<String> ImageArguments = new OptionKey<>("");

    @Option(name = SqueakLanguageOptions.HEADLESS, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.HEADLESS_HELP)//
    public static final OptionKey<Boolean> Headless = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.INTERCEPT_MESSAGES, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.INTERCEPT_MESSAGES_HELP)//
    public static final OptionKey<String> InterceptMessages = new OptionKey<>("");

    @Option(name = SqueakLanguageOptions.QUIET, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.QUIET_HELP)//
    public static final OptionKey<Boolean> Quiet = new OptionKey<>(true);

    @Option(name = SqueakLanguageOptions.INTERRUPTS, category = OptionCategory.USER, stability = OptionStability.STABLE, help = SqueakLanguageOptions.INTERRUPTS_HELP)//
    public static final OptionKey<Boolean> Interrupts = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.SIGNAL_INPUT_SEMAPHORE, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.SIGNAL_INPUT_SEMAPHORE_HELP)//
    public static final OptionKey<Boolean> SignalInputSemaphore = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.STACK_DEPTH_PROTECTION, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.STACK_DEPTH_PROTECTION_HELP)//
    public static final OptionKey<Boolean> StackDepthProtection = new OptionKey<>(false);

    @Option(name = SqueakLanguageOptions.STORAGE_STRATEGIES, category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = SqueakLanguageOptions.STORAGE_STRATEGIES_HELP)//
    public static final OptionKey<Boolean> StorageStrategies = new OptionKey<>(true);

    @Option(name = SqueakLanguageOptions.TESTING, category = OptionCategory.INTERNAL, stability = OptionStability.STABLE, help = SqueakLanguageOptions.TESTING_HELP)//
    public static final OptionKey<Boolean> Testing = new OptionKey<>(false);

    private SqueakOptions() { // no instances
    }

    public static OptionDescriptors createDescriptors() {
        return new SqueakOptionsOptionDescriptors();
    }

    public static final class SqueakContextOptions {
        public final String imagePath;
        public final String[] imageArguments;
        public final boolean isHeadless;
        public final boolean isQuiet;
        public final boolean disableInterruptHandler;
        public final boolean enableStackDepthProtection;
        public final boolean enableStorageStrategies;
        public final boolean isTesting;
        public final boolean signalInputSemaphore;

        public SqueakContextOptions(final Env env) {
            final OptionValues options = env.getOptions();
            imagePath = options.get(ImagePath);
            imageArguments = options.get(ImageArguments).isEmpty() ? new String[0] : options.get(ImageArguments).split(",");
            isHeadless = options.get(Headless);
            isQuiet = options.get(Quiet);
            disableInterruptHandler = options.get(Interrupts);
            signalInputSemaphore = options.get(SignalInputSemaphore);
            enableStackDepthProtection = options.get(StackDepthProtection);
            enableStorageStrategies = options.get(StorageStrategies);
            isTesting = options.get(Testing);
        }
    }
}
