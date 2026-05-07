/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.MaterializedFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.MethodCacheEntry;

@GenerateInline(false)
public abstract class CreateCannotReturnContextNode extends AbstractNode {

    public abstract ContextObject execute(ContextObject returningContext, Object returnValue);

    @Specialization
    protected static final ContextObject doCreate(final ContextObject returningContext, final Object returnValue,
                    @Cached("lookupCannotReturn()") final CompiledCodeObject method) {
        return createContext(returningContext, returnValue, method);
    }

    @TruffleBoundary
    private static ContextObject createContext(final ContextObject returningContext, final Object returnValue, final CompiledCodeObject method) {
        assert method.getNumArgs() == 1;

        final Object[] frameArgs = FrameAccess.newWith(returningContext, null, returningContext, returnValue);
        final MaterializedFrame truffleFrame = Truffle.getRuntime().createMaterializedFrame(frameArgs, method.getFrameDescriptor());
        final ContextObject cannotReturnContext = new ContextObject(truffleFrame);

        final int initialPC = method.getStartPCZeroBased();
        final int initialSP = method.getNumTemps();

        FrameAccess.setInstructionPointer(truffleFrame, initialPC);
        FrameAccess.setStackPointer(truffleFrame, initialSP);

        // The arguments must be in both the frame arguments and the stack.
        cannotReturnContext.atTempPut(0, returnValue);
        // Nil the temporary variables.
        for (int i = method.getNumArgs(); i < initialSP; i++) {
            cannotReturnContext.atTempPut(i, NilObject.SINGLETON);
        }

        return cannotReturnContext;
    }

    @NeverDefault
    protected final CompiledCodeObject lookupCannotReturn() {
        final SqueakImageContext image = getContext();
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
