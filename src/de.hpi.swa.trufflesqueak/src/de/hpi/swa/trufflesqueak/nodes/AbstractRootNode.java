/*
 * Copyright (c) 2023-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2023-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */

package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;

public abstract class AbstractRootNode extends RootNode {
    protected final SqueakImageContext image;

    @Child protected AbstractExecuteContextNode executeBytecodeNode;

    protected AbstractRootNode(final SqueakImageContext image, final CompiledCodeObject code) {
        super(image.getLanguage(), code.getFrameDescriptor());
        this.image = image;
        executeBytecodeNode = new ExecuteBytecodeNode(code);
    }

    public final CompiledCodeObject getCode() {
        return executeBytecodeNode.getCodeObject();
    }

    @Override
    public final String getName() {
        return toString();
    }

    @Override
    public final boolean isCloningAllowed() {
        return true;
    }
}
