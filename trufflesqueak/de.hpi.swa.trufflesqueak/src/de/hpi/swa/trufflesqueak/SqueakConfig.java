package de.hpi.swa.trufflesqueak;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;

public class SqueakConfig {
    private boolean verbose = false;
    private boolean tracing = false;
    private String receiver = "nil";
    private String selector = "yourself";
    private String[] restArgs = new String[0];

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isTracing() {
        return tracing;
    }

    public void setTracing(boolean tracing) {
        this.tracing = tracing;
    }

    public BaseSqueakObject getReceiver(SqueakImageContext img) {
        if (receiver.equals("nil")) {
            return img.nil;
        } else {
            return img.wrapInt(Integer.parseInt(receiver));
        }
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String[] getRestArgs() {
        return restArgs;
    }

    public void setRestArgs(String[] restArgs) {
        this.restArgs = restArgs;
    }
}
