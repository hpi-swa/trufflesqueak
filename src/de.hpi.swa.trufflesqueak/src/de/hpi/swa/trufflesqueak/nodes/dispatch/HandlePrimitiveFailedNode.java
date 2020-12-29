/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeCost;
import com.oracle.truffle.api.nodes.NodeInfo;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes.ArrayObjectSizeNode;
import de.hpi.swa.trufflesqueak.nodes.dispatch.HandlePrimitiveFailedNodeFactory.HandlePrimitiveFailedImplNodeGen;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

@NodeInfo(cost = NodeCost.NONE)
public abstract class HandlePrimitiveFailedNode extends AbstractNode {
    @CompilationFinal protected static int cachedPrimitiveErrorTableSize = -1;

    public static HandlePrimitiveFailedNode create(final CompiledCodeObject method) {
        assert method.hasPrimitive();
        if (method.hasStoreIntoTemp1AfterCallPrimitive()) {
            return HandlePrimitiveFailedImplNodeGen.create();
        } else {
            return new HandlePrimitiveFailedNoopNode();
        }
    }

    public static final Object[] uncached(final CompiledCodeObject method, final PrimitiveFailed pf, final Object[] receiverAndArguments) {
        CompilerAsserts.neverPartOfCompilation();
        assert method.hasPrimitive();
        if (method.hasStoreIntoTemp1AfterCallPrimitive()) {
            final ArrayObject primitiveErrorTable = method.getSqueakClass().getImage().primitiveErrorTable;
            if (cachedPrimitiveErrorTableSize < 0) {
                cachedPrimitiveErrorTableSize = ArrayObjectSizeNode.getUncached().execute(primitiveErrorTable);
            }
            final int reasonCode = pf.getReasonCode();
            final Object errorObject;
            if (reasonCode < cachedPrimitiveErrorTableSize) {
                errorObject = ArrayObjectReadNode.getUncached().execute(primitiveErrorTable, reasonCode);
            } else {
                errorObject = (long) reasonCode;
            }
            return ArrayUtils.copyWithLast(receiverAndArguments, errorObject);
        } else {
            return receiverAndArguments;
        }
    }

    public abstract Object[] execute(PrimitiveFailed pf, Object[] receiverAndArguments);

    /*
     * Look up error symbol in error table and push it to stack by appending it to the arguments.
     * The fallback code pops the error symbol into the corresponding temporary variable. See
     * StackInterpreter>>#getErrorObjectFromPrimFailCode for more information.
     */
    protected abstract static class HandlePrimitiveFailedImplNode extends HandlePrimitiveFailedNode {
        @Specialization(guards = {"pf.getReasonCode() < getCachedPrimitiveErrorTableSize()"})
        protected static final Object[] doHandleWithLookup(final PrimitiveFailed pf, final Object[] receiverAndArguments,
                        @Cached final ArrayObjectReadNode readNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return ArrayUtils.copyWithLast(receiverAndArguments, readNode.execute(image.primitiveErrorTable, pf.getReasonCode()));
        }

        @Specialization(guards = {"pf.getReasonCode() >= getCachedPrimitiveErrorTableSize()"})
        protected static final Object[] doHandleRawValue(final PrimitiveFailed pf, final Object[] receiverAndArguments,
                        @SuppressWarnings("unused") @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return ArrayUtils.copyWithLast(receiverAndArguments, (long) pf.getReasonCode());
        }

        protected final int getCachedPrimitiveErrorTableSize() {
            if (cachedPrimitiveErrorTableSize < 0) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
                cachedPrimitiveErrorTableSize = ArrayObjectSizeNode.getUncached().execute(lookupContext().primitiveErrorTable);
            }
            return cachedPrimitiveErrorTableSize;
        }
    }

    private static final class HandlePrimitiveFailedNoopNode extends HandlePrimitiveFailedNode {
        @Override
        public Object[] execute(final PrimitiveFailed pf, final Object[] receiverAndArguments) {
            return receiverAndArguments;
        }
    }
}
