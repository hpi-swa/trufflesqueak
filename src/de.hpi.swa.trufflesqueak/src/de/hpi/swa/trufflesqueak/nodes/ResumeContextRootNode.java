/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.TruffleLanguage.ContextReference;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;

@NodeInfo(cost = NodeCost.NONE)
public final class ResumeContextRootNode extends RootNode {
    protected ContextObject activeContext;

    @CompilationFinal private ContextReference<SqueakImageContext> contextReference;

    @Child private AbstractExecuteContextNode executeContextNode;

    protected ResumeContextRootNode(final SqueakLanguage language, final ContextObject context) {
        super(language, context.getTruffleFrame().getFrameDescriptor());
        activeContext = context;
        final BlockClosureObject closure = context.getClosure();
        final CompiledCodeObject code = closure == null ? context.getCodeObject() : closure.getCompiledBlock();
        executeContextNode = ExecuteContextNode.create(code);
        contextReference = lookupContextReference(SqueakLanguage.class);
    }

    public static ResumeContextRootNode create(final SqueakLanguage language, final ContextObject activeContext) {
        return new ResumeContextRootNode(language, activeContext);
    }

    @Override
    public Object execute(final VirtualFrame frame) {
        try {
            return executeContextNode.executeResume(activeContext.getTruffleFrame(), activeContext.getInstructionPointerForBytecodeLoop());
        } finally {
            contextReference.get().lastSeenContext = null; // Stop materialization here.
        }
    }

    public ContextObject getActiveContext() {
        return activeContext;
    }

    public void setActiveContext(final ContextObject activeContext) {
        this.activeContext = activeContext;
    }

    @Override
    protected boolean isInstrumentable() {
        return false;
    }

    @Override
    public String getName() {
        CompilerAsserts.neverPartOfCompilation();
        return activeContext.toString();
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        return activeContext.toString();
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }
}
