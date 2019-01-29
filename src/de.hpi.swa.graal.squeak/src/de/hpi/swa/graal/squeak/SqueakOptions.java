package de.hpi.swa.graal.squeak;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;
import org.graalvm.options.OptionValues;

import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleLanguage.Env;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

@Option.Group(SqueakLanguageConfig.ID)
public final class SqueakOptions {

    //@formatter:off
    @Option(category = OptionCategory.USER, help = "Path to image")
    public static final OptionKey<String> ImagePath = new OptionKey<>("");

    // TODO: use ImageArguments
    @Option(category = OptionCategory.USER, help = "Image arguments")
    public static final OptionKey<String> ImageArguments = new OptionKey<>("");

    @Option(category = OptionCategory.USER, help = "Run without a display")
    public static final OptionKey<Boolean> Headless = new OptionKey<>(true);

    @Option(category = OptionCategory.USER, help = "Disable image interrupt handler")
    public static final OptionKey<Boolean> DisableInterruptHandler = new OptionKey<>(false);

    @Option(category = OptionCategory.DEBUG, help = "Trace process switches")
    public static final OptionKey<Boolean> TraceProcessSwitches = new OptionKey<>(false);

    @Option(category = OptionCategory.DEBUG, help = "Enable verbose output")
    public static final OptionKey<Boolean> Verbose = new OptionKey<>(false);

    @Option(category = OptionCategory.EXPERT, help = "For internal testing purposes only")
    public static final OptionKey<Boolean> Testing = new OptionKey<>(false);
    //@formatter:on

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
        public final boolean traceProcessSwitches;
        public final boolean isVerbose;
        public final boolean isTesting;

        public SqueakContextOptions(final Env env) {
            final OptionValues options = env.getOptions();
            imagePath = options.get(SqueakOptions.ImagePath);
            imageArguments = options.get(SqueakOptions.ImageArguments);
            isHeadless = options.get(SqueakOptions.Headless);
            disableInterruptHandler = options.get(SqueakOptions.DisableInterruptHandler);
            traceProcessSwitches = options.get(SqueakOptions.TraceProcessSwitches);
            isVerbose = options.get(SqueakOptions.Verbose);
            isTesting = options.get(SqueakOptions.Testing);
        }
    }
}
