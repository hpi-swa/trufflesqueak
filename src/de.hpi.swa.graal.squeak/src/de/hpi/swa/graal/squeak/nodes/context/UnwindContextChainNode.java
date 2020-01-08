/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.context;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;

public abstract class UnwindContextChainNode extends AbstractNode {
    public static UnwindContextChainNode create() {
        return UnwindContextChainNodeGen.create();
    }

    public abstract ContextObject executeUnwind(Object startContext, Object targetContext, Object returnValue);

    @SuppressWarnings("unused")
    @Specialization
    protected static final ContextObject doTopLevelReturn(final NilObject startContext, final Object targetContext, final Object returnValue) {
        throw new TopLevelReturn(returnValue);
    }

    @Specialization(guards = {"startContext == targetContext", "startContext.isPrimitiveContext()"})
    protected static final ContextObject doUnwindPrimitiveContext(@SuppressWarnings("unused") final ContextObject startContext, final ContextObject targetContext, final Object returnValue) {
        final ContextObject sender = (ContextObject) targetContext.getSender();
        return doUnwindQuick(sender, sender, returnValue); // Skip primitive contexts.
    }

    @Specialization(guards = {"startContext == targetContext", "!startContext.isPrimitiveContext()"})
    protected static final ContextObject doUnwindQuick(@SuppressWarnings("unused") final ContextObject startContext, final ContextObject targetContext, final Object returnValue) {
        targetContext.push(returnValue);
        return targetContext;
    }

    @Specialization(guards = {"startContext != targetContext"})
    protected static final ContextObject doUnwind(final ContextObject startContext, final ContextObject targetContext, final Object returnValue,
                    @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
        ContextObject context = startContext;
        while (context != targetContext) {
            final AbstractSqueakObject sender = context.getSender();
            if (sender == NilObject.SINGLETON) {
                image.printToStdErr("Unwind error: sender of", context, "is nil, unwinding towards", targetContext, "with return value:", returnValue);
                break;
            }
            context.terminate();
            context = (ContextObject) sender;
        }
        targetContext.push(returnValue);
        return targetContext;
    }
}
