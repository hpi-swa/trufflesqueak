package de.hpi.swa.graal.squeak;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionStability;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleLanguage.Env;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

@Option.Group(SqueakLanguageConfig.ID)
public final class SqueakOptions {

    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Path to image")//
    public static final OptionKey<String> ImagePath = new OptionKey<>("");

    // TODO: use ImageArguments
    @Option(category = OptionCategory.USER, stability = OptionStability.EXPERIMENTAL, help = "Image arguments")//
    public static final OptionKey<String> ImageArguments = new OptionKey<>("");

    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Run without a display")//
    public static final OptionKey<Boolean> Headless = new OptionKey<>(false);

    @Option(category = OptionCategory.USER, stability = OptionStability.STABLE, help = "Disable image interrupt handler")//
    public static final OptionKey<Boolean> DisableInterruptHandler = new OptionKey<>(false);

    @Option(category = OptionCategory.INTERNAL, stability = OptionStability.EXPERIMENTAL, help = "For internal testing purposes only")//
    public static final OptionKey<Boolean> Testing = new OptionKey<>(false);

    private SqueakOptions() { // no instances
    }

    public static OptionDescriptors createDescriptors() {
        return new SqueakOptionsOptionDescriptors();
    }

    public static final class SqueakContextOptions {
        public final String imagePath;
        public final String imageArguments;
        public final boolean isHeadless;
        public final boolean disableInterruptHandler;
        public final boolean isTesting;

        public SqueakContextOptions(final Env env) {
            final OptionValues options = env.getOptions();
            imagePath = options.get(SqueakOptions.ImagePath);
            imageArguments = options.get(SqueakOptions.ImageArguments);
            isHeadless = options.get(SqueakOptions.Headless);
            disableInterruptHandler = options.get(SqueakOptions.DisableInterruptHandler);
            isTesting = options.get(SqueakOptions.Testing);
        }
    }
}
