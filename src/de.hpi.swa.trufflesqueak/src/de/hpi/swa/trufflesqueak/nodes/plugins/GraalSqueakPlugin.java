/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.JavaObjectWrapper;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.graal.squeak.util.MiscUtils;

public final class GraalSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return GraalSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "debugPrint")
    protected abstract static class PrimPrintArgsNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
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
    protected abstract static class PrimGetTruffleRuntimeNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            return JavaObjectWrapper.wrap(Truffle.getRuntime());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCallTarget")
    protected abstract static class PrimGetCallTargetNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final CompiledCodeObject code) {
            return JavaObjectWrapper.wrap(code.getCallTarget());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetVMObject")
    protected abstract static class PrimGetVMObjectNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final Object target) {
            return JavaObjectWrapper.wrap(target);
        }
    }

    @GenerateNodeFactory
    @ImportStatic(FORM.class)
    @SqueakPrimitive(names = "primitiveFormToBufferedImage")
    protected abstract static class PrimFormToBufferedImageNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        @Specialization(guards = "form.instsize() > OFFSET")
        protected static final Object doFormToBufferedImage(@SuppressWarnings("unused") final Object receiver, final PointersObject form,
                        @Cached final AbstractPointersObjectReadNode readNode,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            try {
                /* Extract information from form. */
                final NativeObject bits = (NativeObject) readNode.execute(form, FORM.BITS);
                final int width = (int) (long) readNode.execute(form, FORM.WIDTH);
                final int height = (int) (long) readNode.execute(form, FORM.HEIGHT);
                final long depth = (long) readNode.execute(form, FORM.DEPTH);
                if (!bits.isIntType() || depth != 32) {
                    CompilerDirectives.transferToInterpreter();
                    throw PrimitiveFailed.GENERIC_ERROR;
                }
                /* Use bitmap's storage as backend for BufferedImage. */
                return image.env.asGuestValue(MiscUtils.new32BitBufferedImage(bits.getIntStorage(), width, height));
            } catch (final ClassCastException e) {
                CompilerDirectives.transferToInterpreter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }
}
