/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.exceptions;

import com.oracle.truffle.api.nodes.ControlFlowException;

public final class ProcessSwitch extends ControlFlowException {
    public static final ProcessSwitch SINGLETON = new ProcessSwitch();
    private static final long serialVersionUID = 1L;

    private ProcessSwitch() {
    }
}
