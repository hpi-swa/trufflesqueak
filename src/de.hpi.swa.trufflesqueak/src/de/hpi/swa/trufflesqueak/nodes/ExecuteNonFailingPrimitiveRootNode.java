/*
 * Copyright (c) 2021-2024 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2024 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.DispatchPrimitiveNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

@NodeInfo(language = SqueakLanguageConfig.ID)
public final class ExecuteNonFailingPrimitiveRootNode extends AbstractRootNode {

    @Child private DispatchPrimitiveNode primitiveNode;

    public ExecuteNonFailingPrimitiveRootNode(final SqueakLanguage language, final CompiledCodeObject code, final DispatchPrimitiveNode primitiveNode) {
        super(language, code);
        this.primitiveNode = primitiveNode;
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            return primitiveNode.execute(frame);
        } catch (final PrimitiveFailed pf) {
            throw CompilerDirectives.shouldNotReachHere();
        }
    }

    @Override
    public String getName() {
        return toString();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return getCode().toString();
    }

    @Override
    protected boolean isTrivial() {
        return true;
    }
}
