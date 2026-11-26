/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.logging.Level;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInterface;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class DispatchUtils {
    static Assumption[] createAssumptions(final ClassObject startClass, final Object lookupResult) {
        if (lookupResult instanceof final CompiledCodeObject method) {
            assert method.isCompiledMethod();
            return new Assumption[]{startClass.getLookupAssumption(method.getMethodClassSlow(), method)};
        } else {
            /* DNU or OAM, depend on all superclasses. */
            return new Assumption[]{startClass.getLookupAssumption(null, null)};
        }
    }

    static void logMissingPrimitive(final AbstractPrimitiveNode primitiveNode, final CompiledCodeObject code) {
        assert primitiveNode == null && code.hasPrimitive();
        final int primitiveIndex = code.primitiveIndex();
        if (LogUtils.PRIMITIVES.isLoggable(Level.FINE)) {
            if (primitiveIndex == PrimitiveNodeFactory.PRIMITIVE_EXTERNAL_CALL_INDEX) {
                LogUtils.PRIMITIVES.fine("Named primitive not found for " + code);
            } else if (primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_SIMULATION_GUARD_INDEX &&
                            primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_ENSURE_MARKER_INDEX &&
                            primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_ON_DO_MARKER_INDEX &&
                            primitiveIndex != 65 && primitiveIndex != 66 && primitiveIndex != 67) {
                LogUtils.PRIMITIVES.fine("Primitive #" + code.primitiveIndex() + " not found for " + code);
            }
        }
    }

    public static void logPrimitiveFailed(final NodeInterface primitiveNode) {
        if (LogUtils.PRIMITIVES.isLoggable(Level.FINER)) {
            logPrimitiveFailedBoundary(primitiveNode);
        }
    }

    @TruffleBoundary
    private static void logPrimitiveFailedBoundary(final NodeInterface primitiveNode) {
        LogUtils.PRIMITIVES.finer(primitiveNode.getClass().getSimpleName() + " failed");
    }

    @TruffleBoundary
    public static void handlePrimitiveFailedIndirect(final Node node, final CompiledCodeObject method, final PrimitiveFailed primitiveFailed) {
        if (method.hasStoreIntoTemp1AfterCallPrimitive()) {
            SqueakImageContext.get(node).setPrimFailCode(primitiveFailed);
        }
    }
}
