package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.Returns.TopLevelReturn;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;

public abstract class UnwindContextChainNode extends AbstractNodeWithImage {

    public static UnwindContextChainNode create(final SqueakImageContext image) {
        return UnwindContextChainNodeGen.create(image);
    }

    protected UnwindContextChainNode(final SqueakImageContext image) {
        super(image);
    }

    public abstract ContextObject executeUnwind(AbstractSqueakObject startContext, AbstractSqueakObject targetContext, Object returnValue);

    @SuppressWarnings("unused")
    @Specialization
    protected static final ContextObject doTopLevelReturn(final NilObject startContext, final AbstractSqueakObject targetContext, final Object returnValue) {
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
    protected static final ContextObject doUnwind(final ContextObject startContext, final ContextObject targetContext, final Object returnValue) {
        ContextObject context = startContext;
        while (context != targetContext) {
            final AbstractSqueakObject sender = context.getSender();
            if (sender.isNil()) {
                sender.image.printVerbose("[graalsqueak] Unwind error (sender is nil)");
                break;
            }
            context.terminate();
            context = (ContextObject) sender;
        }
        targetContext.push(returnValue);
        return targetContext;
    }

    @Specialization
    protected final ContextObject doFail(final AbstractSqueakObject startContext, final ContextObject targetContext, final Object returnValue) {
        image.printToStdErr("Unable to unwind context chain (start: " + startContext + "; target: " + targetContext + ")");
        image.printSqStackTrace();
        targetContext.push(returnValue);
        return targetContext;
    }

    @Fallback
    protected static final ContextObject doFail(final AbstractSqueakObject startContext, final AbstractSqueakObject targetContext, final Object returnValue) {
        throw new SqueakException("Failed to unwind context chain (start:", startContext, ", target:", targetContext, ", returnValue:", returnValue, ")");
    }
}
