/*
 * Copyright (c) 2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.ArrayList;

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
        final ClassObject targetClass;
        final Assumption callTargetStable;
        if (lookupResult instanceof CompiledCodeObject method) {
            assert method.isCompiledMethod();
            targetClass = method.getMethodClassSlow();
            callTargetStable = method.getCallTargetStable();
        } else {
            /* DNU or OAM, return assumptions for all superclasses. */
            targetClass = null;
            callTargetStable = null;
        }
        return createAssumptions(startClass, targetClass, callTargetStable);
    }

    static Assumption[] createAssumptions(final ClassObject startClass, final ClassObject targetClass, final Assumption callTargetStable) {
        if (startClass == targetClass) {
            if (callTargetStable == null) {
                return new Assumption[]{startClass.getClassHierarchyAndMethodDictStable()};
            } else {
                return new Assumption[]{startClass.getClassHierarchyAndMethodDictStable(), callTargetStable};
            }
        } else {
            final ArrayList<Assumption> list = new ArrayList<>();
            if (callTargetStable != null) {
                list.add(callTargetStable);
            }
            ClassObject currentClass = startClass;
            while (currentClass != null) {
                list.add(currentClass.getClassHierarchyAndMethodDictStable());
                if (currentClass == targetClass) {
                    break;
                } else {
                    currentClass = currentClass.getSuperclassOrNull();
                }
            }
            // TODO: the receiverClass can be an outdated version of methodClass. In this case, a
            // list of assumptions for the entire class hierarchy is returned. Maybe this can/should
            // be avoided.
            return list.toArray(new Assumption[0]);
        }
    }

    static void logMissingPrimitive(final AbstractPrimitiveNode primitiveNode, final CompiledCodeObject code) {
        assert primitiveNode == null && code.hasPrimitive();
        final int primitiveIndex = code.primitiveIndex();
        if (primitiveIndex == PrimitiveNodeFactory.PRIMITIVE_EXTERNAL_CALL_INDEX) {
            LogUtils.PRIMITIVES.fine(() -> "Named primitive not found for " + code);
        } else if (primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_SIMULATION_GUARD_INDEX &&
                        primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_ENSURE_MARKER_INDEX &&
                        primitiveIndex != PrimitiveNodeFactory.PRIMITIVE_ON_DO_MARKER_INDEX &&
                        primitiveIndex != 65 && primitiveIndex != 66 && primitiveIndex != 67) {
            LogUtils.PRIMITIVES.fine(() -> "Primitive #" + code.primitiveIndex() + " not found for " + code);
        }
    }

    public static void logPrimitiveFailed(final NodeInterface primitiveNode) {
        LogUtils.PRIMITIVES.finer(() -> primitiveNode.getClass().getSimpleName() + " failed");
    }

    @TruffleBoundary
    public static void handlePrimitiveFailedIndirect(final Node node, final CompiledCodeObject method, final PrimitiveFailed primitiveFailed) {
        if (method.hasStoreIntoTemp1AfterCallPrimitive()) {
            SqueakImageContext.get(node).setPrimFailCode(primitiveFailed);
        }
    }
}
