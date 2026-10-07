/*
 * Copyright (c) 2025-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2025-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInterface;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
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
            // Count the required array size
            int depth = (callTargetStable != null) ? 1 : 0;
            ClassObject currentClass = startClass;
            while (currentClass != null) {
                depth++;
                if (currentClass == targetClass) {
                    break;
                }
                currentClass = currentClass.getSuperclassOrNull();
            }

            // Allocate exactly sized array and populate
            final Assumption[] assumptions = new Assumption[depth];
            int index = 0;
            if (callTargetStable != null) {
                assumptions[index++] = callTargetStable;
            }

            currentClass = startClass;
            while (currentClass != null) {
                assumptions[index++] = currentClass.getClassHierarchyAndMethodDictStable();
                if (currentClass == targetClass) {
                    break;
                }
                currentClass = currentClass.getSuperclassOrNull();
            }

            return assumptions;
        }
    }

    /**
     * Creates the complete assumption array for a message fallback node (DNU or CI). On top of the
     * standard class hierarchy stability, it registers two assumptions:
     * <p>
     * Fallback Method Stability: Tracks the `callTargetStable` of the resolved fallback method
     * itself. This ensures the AST node is invalidated if the actual #doesNotUnderstand: or
     * #cannotInterpret: method is later modified or recompiled.
     * <p>
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

    @ExplodeLoop
    static PointersObject buildNestedMessage(final CreateMessageNode createMessageNode,
                    final NativeObject originalSelector, final NativeObject cannotInterpretSelector,
                    final Object receiver, final Object[] arguments, final int fallbackDepth) {
        PointersObject message = createMessageNode.execute(originalSelector, receiver, arguments);
        for (int i = 1; i < fallbackDepth; i++) {
            message = createMessageNode.execute(cannotInterpretSelector, receiver, new Object[]{message});
        }
        return message;
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
