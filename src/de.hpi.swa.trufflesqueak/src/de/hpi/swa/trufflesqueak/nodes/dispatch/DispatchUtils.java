/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
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
import de.hpi.swa.trufflesqueak.model.NativeObject;
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

    /**
     * Creates the complete assumption array for a message fallback node (DNU or CI). On top of the
     * standard class hierarchy stability, it registers two assumptions:
     *
     * Fallback Method Stability: Tracks the `callTargetStable` of the resolved fallback method
     * itself. This ensures the AST node is invalidated if the actual #doesNotUnderstand: or
     * #cannotInterpret: method is later modified or recompiled.
     *
     * Absent Selector Stability: Tracks an image-global assumption that the specific failing
     * selector does not exist. If a method for this missing selector is compiled, the VM's cache
     * flush (primitive 119) will trip this assumption globally. This prevents "stranded DNU" nodes
     * by forcing them to drop and re-resolve to the newly added method.
     */
    static Assumption[] getAssumptionsForMessageFallback(final Assumption[] classAssumptions, final NativeObject selector, final CompiledCodeObject fallbackMethod) {
        final Assumption[] finalAssumptions = new Assumption[classAssumptions.length + 2];
        System.arraycopy(classAssumptions, 0, finalAssumptions, 0, classAssumptions.length);
        finalAssumptions[classAssumptions.length] = fallbackMethod.getCallTargetStable();
        finalAssumptions[classAssumptions.length + 1] = SqueakImageContext.getSlow().getAbsentSelectorAssumption(selector);
        return finalAssumptions;
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
