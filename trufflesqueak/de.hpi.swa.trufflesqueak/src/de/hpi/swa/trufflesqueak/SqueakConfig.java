package de.hpi.swa.trufflesqueak;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

public class SqueakConfig {
    @CompilationFinal private final String imagePath;
    @CompilationFinal private final boolean verbose;
    @CompilationFinal private final boolean tracing;
    @CompilationFinal private final boolean disableInterrupts;
    @CompilationFinal private final String receiver;
    @CompilationFinal private final String selector;
    @CompilationFinal(dimensions = 1) private final String[] restArgs;
    @CompilationFinal private final List<String> unrecognized = new ArrayList<>();

    @SuppressWarnings("hiding")
    public SqueakConfig(String[] args) {
        this.imagePath = args.length > 0 ? args[0] : "unknown";
        boolean verbose = false;
        boolean tracing = false;
        boolean disableInterrupts = false;
        String receiver = "nil";
        String selector = null;
        String[] restArgs = null;

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
                    disableInterrupts = true;
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
            }
        }

        this.verbose = verbose;
        this.tracing = tracing;
        this.disableInterrupts = disableInterrupts;
        this.receiver = receiver;
        this.selector = selector;
        this.restArgs = restArgs;
    }

    public String[] toStringArgs() {
        List<String> sb = new ArrayList<>();
        sb.add(imagePath);
        if (verbose) {
            sb.add("-v");
        }
        if (tracing) {
            sb.add("-t");
        }
        if (receiver != null) {
            sb.add("-r");
            sb.add(receiver.toString());
        }
        if (selector != null) {
            sb.add("-m");
            sb.add(selector.toString());
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

    public Object getReceiver() {
        if (receiver.equals("nil")) {
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
}
