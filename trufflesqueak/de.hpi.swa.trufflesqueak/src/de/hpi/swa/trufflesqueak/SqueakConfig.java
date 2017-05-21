package de.hpi.swa.trufflesqueak;

import java.util.Arrays;

public class SqueakConfig {
    private final boolean verbose;
    private final boolean tracing;
    private final String receiver;
    private final String selector;
    private final String[] restArgs;

    @SuppressWarnings("hiding")
    public SqueakConfig(String[] args) {
        boolean verbose = false;
        boolean tracing = false;
        String receiver = "nil";
        String selector = "yourself";
        String[] restArgs = null;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--verbose":
                case "-v":
                    verbose = true;
                    break;
                case "--trace":
                case "-t":
                    tracing = true;
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
                    restArgs = Arrays.copyOfRange(args, i + 1, args.length);
                    i = args.length;
                    break;
            }
        }

        this.verbose = verbose;
        this.tracing = tracing;
        this.receiver = receiver;
        this.selector = selector;
        this.restArgs = restArgs;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isTracing() {
        return tracing;
    }

    public Object getReceiver() {
        if (receiver.equals("nil")) {
            return null;
        } else {
            return Integer.parseInt(receiver);
        }
    }

    public String getSelector() {
        return selector;
    }

    public String[] getRestArgs() {
        return restArgs;
    }
}
