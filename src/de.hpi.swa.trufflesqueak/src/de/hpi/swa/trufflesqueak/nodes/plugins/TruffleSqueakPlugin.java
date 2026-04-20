/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.ArrayList;
import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Bind;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.JavaObjectWrapper;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.accessing.ArrayObjectNodes;
import de.hpi.swa.trufflesqueak.nodes.interpreter.AbstractInterpreterNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.LogUtils;

public final class TruffleSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return TruffleSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "debugPrint")
    protected abstract static class PrimPrintArgsNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final Object printArgs(final Object receiver, final Object value) {
            if (value instanceof final NativeObject o && o.isByteType()) {
                LogUtils.DEBUG.info(o::asStringUnsafe);
            } else {
                LogUtils.DEBUG.info(() -> String.valueOf(value));
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetTruffleRuntime")
    protected abstract static class PrimGetTruffleRuntimeNode extends AbstractPrimitiveNode implements Primitive0 {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            return JavaObjectWrapper.wrap(Truffle.getRuntime());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetVMObject")
    protected abstract static class PrimGetVMObjectNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final Object target) {
            return JavaObjectWrapper.wrap(target);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveVMObjectToHostObject")
    protected abstract static class PrimVMObjectToHostObjectNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver, final JavaObjectWrapper target) {
            return getContext().env.asGuestValue(target.unwrap());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetDirectCallNodes")
    protected abstract static class PrimGetDirectCallNodesNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        @CompilerDirectives.TruffleBoundary
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final JavaObjectWrapper target) {
            final Object wrappedObject = target.unwrap();
            if (wrappedObject instanceof final RootNode rootNode) {
                final List<DirectCallNode> callNodes = new ArrayList<>();
                rootNode.accept(node -> {
                    if (node instanceof final DirectCallNode dcn) {
                        callNodes.add(dcn);
                    } else if (node instanceof AbstractInterpreterNode ain) {
                        ain.visitDataNodes(dataNode -> {
                            if (dataNode instanceof DirectCallNode dcn) {
                                callNodes.add(dcn);
                            }
                            return true;
                        });
                    }
                    return true;
                });
                return JavaObjectWrapper.wrap(callNodes.toArray(new DirectCallNode[0]));
            } else {
                throw PrimitiveFailed.andTransferToInterpreter();
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveSetDNUShortcutSelectors")
    protected abstract static class PrimSetDNUShortcutSelectorsNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization
        protected final Object doSet(final Object receiver, final ArrayObject array,
                        @Bind final Node node,
                        @Cached final ArrayObjectNodes.ArrayObjectToObjectArrayCopyNode toObjectArrayNode) {

            final Object[] elements = toObjectArrayNode.execute(node, array);
            final int arraySize = elements.length;

            if (arraySize > SqueakImageContext.MAX_DNU_SHORTCUT_ARITY + 1) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.BAD_ARGUMENT;
            }

            final NativeObject[] isolatedSelectors = new NativeObject[arraySize];

            for (int i = 0; i < arraySize; i++) {
                final Object element = elements[i];

                if (element instanceof final NativeObject nativeObject) {
                    isolatedSelectors[i] = nativeObject;
                } else if (element == NilObject.SINGLETON) {
                    isolatedSelectors[i] = null;
                } else {
                    CompilerDirectives.transferToInterpreter();
                    throw PrimitiveFailed.BAD_ARGUMENT;
                }
            }

            getContext().setDNUShortcutSelectors(isolatedSelectors);
            return receiver;
        }
    }
}
