package de.hpi.swa.graal.squeak.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakConfig {
    public static final String ID = "squeaksmalltalk";
    public static final String NAME = "Squeak/Smalltalk";
    public static final String MIME_TYPE = "application/x-squeak-smalltalk";
    public static final String VERSION = "0.1";

    private final String imagePath;
    private final String imageDirectory;
    private final boolean verbose;
    private final boolean tracing;
    private final boolean disableInterrupts;
    private final boolean testing;
    private final boolean headless;
    private final String receiver;
    private final String selector;
    @CompilationFinal(dimensions = 1) private final String[] restArgs;
    private final List<String> unrecognized = new ArrayList<>();

    @SuppressWarnings("hiding")
    public SqueakConfig(final String[] arguments) {
        assert arguments.length >= 1;
        final String firstArgument = arguments[0];
        if (firstArgument.startsWith("--")) { // `--help` or other option requested
            unrecognized.add(firstArgument);
            this.imagePath = null;
            this.imageDirectory = null;
        } else {
            final File imageFile = new File(firstArgument.trim());
            final File parentFile = imageFile.getParentFile();
            this.imagePath = imageFile.getAbsolutePath();
            this.imageDirectory = parentFile == null ? null : parentFile.getAbsolutePath();
        }

        boolean verbose = false;
        boolean tracing = false;
        boolean disableInterrupts = false;
        boolean testing = false;
        String receiver = "nil";
        String selector = null;
        String[] restArgs = new String[]{};

        outerloop: for (int i = 1; i < arguments.length; i++) {
            switch (arguments[i]) {
                case "--verbose":
                case "-v":
                    verbose = true;
                    break;
                case "--trace":
                case "-t":
                    tracing = true;
                    break;
                case "--disable-interrupts":
                case "-d":
                    disableInterrupts = true;
                    break;
                case "--testing":
                    testing = true;
                    break;
                case "--receiver":
                case "-r":
                    receiver = arguments[++i];
                    break;
                case "--method":
                case "-m":
                    selector = arguments[++i];
                    break;
                case "--":
                case "--args":
                    restArgs = Arrays.copyOfRange(arguments, i + 1, arguments.length);
                    break outerloop;
                default:
                    unrecognized.add(arguments[i]);
                    break;
            }
        }

        this.verbose = verbose;
        this.tracing = tracing;
        this.disableInterrupts = disableInterrupts;
        this.testing = testing;
        this.receiver = receiver;
        this.selector = selector;
        this.restArgs = restArgs;
        this.headless = isTesting() || isCustomContext(); // TODO: turn this into a dedicated flag
    }

    public String[] toStringArgs() {
        final List<String> sb = new ArrayList<>();
        sb.add(imagePath);
        if (verbose) {
            sb.add("-v");
        }
        if (tracing) {
            sb.add("-t");
        }
        if (receiver != null) {
            sb.add("-r");
            sb.add(receiver);
        }
        if (selector != null) {
            sb.add("-m");
            sb.add(selector);
        }
        if (disableInterrupts) {
            sb.add("-d");
        }
        if (testing) {
            sb.add("--testing");
        }
        if (restArgs != null) {
            sb.add("--args");
            for (String s : restArgs) {
                sb.add(s);
            }
        }
        return sb.toArray(new String[0]);
    }

    public List<String> getUnrecognized() {
        return unrecognized;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isTracing() {
        return tracing;
    }

    public boolean isCustomContext() {
        return selector != null; // TODO: make better?
    }

    public String getImagePath() {
        return imagePath;
    }

    public String getImageDirectory() {
        return imageDirectory;
    }

    public Object getReceiver() {
        if ("nil".equals(receiver)) {
            return null;
        } else {
            return Long.parseLong(receiver);
        }
    }

    public String getSelector() {
        return selector;
    }

    public String[] getRestArgs() {
        return restArgs;
    }

    public boolean disableInterruptHandler() {
        return disableInterrupts;
    }

    public boolean isTesting() {
        return testing;
    }

    public boolean isHeadless() {
        return headless;
    }
}
