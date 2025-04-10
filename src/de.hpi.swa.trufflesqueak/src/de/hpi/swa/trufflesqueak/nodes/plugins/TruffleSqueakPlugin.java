/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
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
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.JavaObjectWrapper;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class TruffleSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return TruffleSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "debugPrint")
    protected abstract static class PrimPrintArgsNode extends AbstractPrimitiveNode implements Primitive1 {
        @Specialization
        protected final Object printArgs(final Object receiver, final Object value) {
            final SqueakImageContext image = getContext();
            if (value instanceof final NativeObject o && o.isByteType()) {
                image.printToStdOut(o.asStringUnsafe());
            } else {
                image.printToStdOut(value);
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
    @ImportStatic(FORM.class)
    @SqueakPrimitive(names = "primitiveFormToBufferedImage")
    protected abstract static class PrimFormToBufferedImageNode extends AbstractPrimitiveNode implements Primitive1WithFallback {
        @Specialization(guards = "form.instsize() > OFFSET")
        protected final Object doFormToBufferedImage(@SuppressWarnings("unused") final Object receiver, final PointersObject form,
                        @Bind final Node node,
                        @Cached final AbstractPointersObjectReadNode readNode) {
            try {
                /* Extract information from form. */
                final NativeObject bits = readNode.executeNative(node, form, FORM.BITS);
                final int width = readNode.executeInt(node, form, FORM.WIDTH);
                final int height = readNode.executeInt(node, form, FORM.HEIGHT);
                final long depth = readNode.executeLong(node, form, FORM.DEPTH);
                if (!bits.isIntType() || depth != 32) {
                    CompilerDirectives.transferToInterpreter();
                    throw PrimitiveFailed.GENERIC_ERROR;
                }
                /* Use bitmap's storage as backend for BufferedImage. */
                return getContext().env.asGuestValue(MiscUtils.new32BitBufferedImage(bits.getIntStorage(), width, height, true));
            } catch (final ClassCastException e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }
}
