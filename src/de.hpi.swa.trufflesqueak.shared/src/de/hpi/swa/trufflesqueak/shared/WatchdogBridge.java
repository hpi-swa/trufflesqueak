/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.shared;

public final class WatchdogBridge {
    private static volatile Runnable timeoutAction;

    public static void setAction(final Runnable dumper) {
        timeoutAction = dumper;
    }

    public static void triggerAction() {
        if (timeoutAction != null) {
            timeoutAction.run();
        }
    }
}
