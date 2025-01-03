/*
 * Copyright (c) 2023-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class AbstractRootNode extends RootNode {
    private final CompiledCodeObject code;

    protected AbstractRootNode(final TruffleLanguage<?> language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
    }

    protected final CompiledCodeObject getCode() {
        return code;
    }
}
