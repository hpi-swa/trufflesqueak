/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;

@NodeInfo(cost = NodeCost.NONE)
public final class EnterCodeNode extends RootNode {
    private final CompiledCodeObject code;

    @Child private ExecuteContextNode executeContextNode;

    protected EnterCodeNode(final SqueakLanguage language, final CompiledCodeObject code) {
        super(language, code.getFrameDescriptor());
        this.code = code;
        executeContextNode = ExecuteContextNode.create(code, false);
    }

    protected EnterCodeNode(final EnterCodeNode codeNode) {
        this(codeNode.code.image.getLanguage(), codeNode.code);
    }

    public static EnterCodeNode create(final SqueakLanguage language, final CompiledCodeObject code) {
        return new EnterCodeNode(language, code);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        return executeContextNode.executeFresh(frame);
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
    public boolean isCloningAllowed() {
        return true;
    }
}
