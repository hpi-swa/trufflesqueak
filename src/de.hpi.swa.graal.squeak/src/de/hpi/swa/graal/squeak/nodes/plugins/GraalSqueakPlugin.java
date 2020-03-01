/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class GraalSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return GraalSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "debugPrint")
    protected abstract static class PrimPrintArgsNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        protected PrimPrintArgsNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "value.isByteType()")
        protected Object printArgs(final Object receiver, final NativeObject value) {
            method.image.printToStdOut(value.asStringUnsafe());
            return receiver;
        }

        @Fallback
        protected Object printArgs(final Object receiver, final Object value) {
            method.image.printToStdOut(value);
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveLookupGraalSymbol")
    protected abstract static class PrimLookupGraalSymbolNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimLookupGraalSymbolNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = {"value.isByteType()"})
        protected final Object doLookupGraalSymbol(@SuppressWarnings("unused") final Object receiver, final NativeObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image,
                        @Cached final BranchProfile errorProfile) {
            final String symbolName = value.asStringUnsafe();
            try {
                final Class<?> symbolClass = Class.forName(symbolName, true, method.getCallTarget().getClass().getClassLoader());
                return NilObject.nullToNil(image.env.asHostSymbol(symbolClass));
            } catch (final ClassNotFoundException e) {
                errorProfile.enter();
                throw PrimitiveFailed.GENERIC_ERROR;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCallTarget")
    protected abstract static class PrimGetCallTargetNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGetCallTargetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final CompiledCodeObject code,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.env.asGuestValue(code.getCallTarget());
        }
    }
}
