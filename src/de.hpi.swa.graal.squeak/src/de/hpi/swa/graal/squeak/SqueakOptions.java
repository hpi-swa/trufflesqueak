package de.hpi.swa.graal.squeak;

import org.graalvm.options.OptionCategory;
import org.graalvm.options.OptionDescriptors;
import org.graalvm.options.OptionKey;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Option;
import com.oracle.truffle.api.TruffleLanguage.Env;

import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;

@Option.Group(SqueakLanguageConfig.ID)
public final class SqueakOptions {

    private SqueakOptions() {
        // no instances
    }

    //@formatter:off
    @Option(category = OptionCategory.USER, help = "Path to image")
    public static final OptionKey<String> ImagePath = new OptionKey<>("Squeak.image");

    @Option(category = OptionCategory.USER, help = "Run without a display")
    public static final OptionKey<Boolean> Headless = new OptionKey<>(false);

    @Option(category = OptionCategory.USER, help = "Disable image interrupt handler")
    public static final OptionKey<Boolean> DisableInterruptHandler = new OptionKey<>(false);

    @Option(category = OptionCategory.DEBUG, help = "Trace process switches")
    public static final OptionKey<Boolean> TraceProcessSwitches = new OptionKey<>(false);

    @Option(category = OptionCategory.DEBUG, help = "Enable verbose output")
    public static final OptionKey<Boolean> Verbose = new OptionKey<>(false);

    @Option(category = OptionCategory.EXPERT, help = "For internal testing purposes only")
    public static final OptionKey<Boolean> Testing = new OptionKey<>(false);
    //@formatter:on

    public static OptionDescriptors createDescriptors() {
        return new SqueakOptionsOptionDescriptors();
    }

    @TruffleBoundary
    public static <T> T getOption(final Env env, final OptionKey<T> key) {
        if (env == null) {
            return key.getDefaultValue();
        }
        return env.getOptions().get(key);
    }
}
