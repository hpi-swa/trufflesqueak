/*
 * Copyright (c) 2017-2021 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.interop.JavaObjectWrapper;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.trufflesqueak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveFallbacks.BinaryPrimitiveFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.MiscUtils;

public final class TruffleSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return TruffleSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "debugPrint")
    protected abstract static class PrimPrintArgsNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object printArgs(final Object receiver, final Object value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            if (value instanceof NativeObject && ((NativeObject) value).isByteType()) {
                image.printToStdOut(((NativeObject) value).asStringUnsafe());
            } else {
                image.printToStdOut(value);
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetTruffleRuntime")
    protected abstract static class PrimGetTruffleRuntimeNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            return JavaObjectWrapper.wrap(Truffle.getRuntime());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCallTarget")
    protected abstract static class PrimGetCallTargetNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final CompiledCodeObject code) {
            return JavaObjectWrapper.wrap(code.getCallTarget());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetVMObject")
    protected abstract static class PrimGetVMObjectNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final Object target) {
            return JavaObjectWrapper.wrap(target);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FORM.class)
    @SqueakPrimitive(names = "primitiveFormToBufferedImage")
    protected abstract static class PrimFormToBufferedImageNode extends AbstractPrimitiveNode implements BinaryPrimitiveFallback {
        @Specialization(guards = "form.instsize() > OFFSET")
        protected static final Object doFormToBufferedImage(@SuppressWarnings("unused") final Object receiver, final PointersObject form,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                /* Extract information from form. */
                final NativeObject bits = readNode.executeNative(form, FORM.BITS);
                final int width = readNode.executeInt(form, FORM.WIDTH);
                final int height = readNode.executeInt(form, FORM.HEIGHT);
                final long depth = readNode.executeLong(form, FORM.DEPTH);
                if (!bits.isIntType() || depth != 32) {
                    CompilerDirectives.transferToInterpreter();
                    throw PrimitiveFailed.GENERIC_ERROR;
                }
                /* Use bitmap's storage as backend for BufferedImage. */
                return image.env.asGuestValue(MiscUtils.new32BitBufferedImage(bits.getIntStorage(), width, height, true));
            } catch (final ClassCastException e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }
}
