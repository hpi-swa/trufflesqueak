package de.hpi.swa.graal.squeak;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public final class SqueakConfig {
    @CompilationFinal private final String imagePath;
    @CompilationFinal private final String imageDirectory;
    @CompilationFinal private final boolean verbose;
    @CompilationFinal private final boolean tracing;
    @CompilationFinal private final boolean disableInterrupts;
    @CompilationFinal private final boolean testing;
    @CompilationFinal private final String receiver;
    @CompilationFinal private final String selector;
    @CompilationFinal(dimensions = 1) private final String[] restArgs;
    @CompilationFinal private final List<String> unrecognized = new ArrayList<>();

    @SuppressWarnings("hiding")
    public SqueakConfig(final String[] args) {
        assert args.length > 0;
        final File imageFile = new File(args[0].trim());
        final File parentFile = imageFile.getParentFile();
        this.imagePath = imageFile.getAbsolutePath();
        this.imageDirectory = parentFile == null ? null : parentFile.getAbsolutePath();
        boolean verbose = false;
        boolean tracing = false;
        boolean disableInterrupts = false;
        boolean testing = false;
        String receiver = "nil";
        String selector = null;
        String[] restArgs = new String[]{};

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
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
                    receiver = args[++i];
                    break;
                case "--method":
                case "-m":
                    selector = args[++i];
                    break;
                case "--":
                case "--args":
                    restArgs = Arrays.copyOfRange(args, i + 1, args.length);
                    i = args.length;
                    break;
                default:
                    unrecognized.add(args[i]);
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
        return selector != null; // make better?
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
}
