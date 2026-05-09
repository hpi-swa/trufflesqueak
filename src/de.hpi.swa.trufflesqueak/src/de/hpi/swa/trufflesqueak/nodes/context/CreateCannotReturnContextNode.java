/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

public final class CreateCannotReturnContextNode extends AbstractNode {
    private static final int INITIAL_PC = 0;
    private static final int INITIAL_SP = 1;
    private static final int NUM_ARGUMENTS = 1;

    private final CompiledCodeObject cannotReturnMethod;
    private final FrameDescriptor cannotReturnMethodFrameDescriptor;

    public CreateCannotReturnContextNode(final SqueakImageContext image) {
        cannotReturnMethod = lookupCannotReturnMethod(image);
        cannotReturnMethodFrameDescriptor = cannotReturnMethod.getFrameDescriptor();
    }

    public ContextObject execute(final ContextObject returningContext, final Object returnValue) {
        assert cannotReturnMethod.getStartPCZeroBased() == INITIAL_PC;
        assert cannotReturnMethod.getNumTemps() == INITIAL_SP;
        assert cannotReturnMethod.getNumArgs() == NUM_ARGUMENTS;
        assert cannotReturnMethod.getFrameDescriptor() == cannotReturnMethodFrameDescriptor;

        final Object[] frameArgs = FrameAccess.newWith(returningContext, null, returningContext, returnValue);
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArgs, cannotReturnMethodFrameDescriptor);
        final ContextObject cannotReturnContext = new ContextObject(truffleFrame);

        FrameAccess.setInstructionPointer(truffleFrame, INITIAL_PC);
        FrameAccess.setStackPointer(truffleFrame, INITIAL_SP);

        // The arguments must be in both the frame arguments and the stack.
        cannotReturnContext.atTempPut(0, returnValue);
        // No temporary variables to nil out because INITIAL_SP == NUM_ARGUMENTS.

        return cannotReturnContext;
    }

    private static CompiledCodeObject lookupCannotReturnMethod(final SqueakImageContext image) {
        CompilerAsserts.neverPartOfCompilation();
        final ClassObject receiverClass = image.methodContextClass;
        final MethodCacheEntry cacheEntry = image.findMethodCacheEntry(receiverClass, image.cannotReturn);
        Object lookupResult = cacheEntry.getResult();
        if (lookupResult == null) {
            lookupResult = receiverClass.lookupMethodInMethodDictSlow(image.cannotReturn);
            cacheEntry.setResult(lookupResult);
        }
        if (!(lookupResult instanceof CompiledCodeObject)) {
            throw SqueakException.create("cannotReturn: must be a CompiledCodeObject");
        }
        return (CompiledCodeObject) lookupResult;
    }
}
