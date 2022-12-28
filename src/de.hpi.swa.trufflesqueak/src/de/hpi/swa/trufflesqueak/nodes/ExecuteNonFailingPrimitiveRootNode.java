/*
 * Copyright (c) 2021-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;

@NodeInfo(language = SqueakLanguageConfig.ID, cost = NodeCost.NONE)
public final class ExecuteNonFailingPrimitiveRootNode extends RootNode {
    private final CompiledCodeObject code;

    @Child private AbstractPrimitiveNode primitiveNode;

    public ExecuteNonFailingPrimitiveRootNode(final SqueakLanguage language, final CompiledCodeObject code, final AbstractPrimitiveNode primitiveNode) {
        super(language, code.getFrameDescriptor());
        this.code = code;
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
        return code.toString();
    }

    @Override
    protected boolean isTrivial() {
        return true;
    }
}
