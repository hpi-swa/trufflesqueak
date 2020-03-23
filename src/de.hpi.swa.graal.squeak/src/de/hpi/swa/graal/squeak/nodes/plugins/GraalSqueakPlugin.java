/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.interop.JavaObjectWrapper;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
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
    @SqueakPrimitive(names = "primitiveGetTruffleRuntime")
    protected abstract static class PrimGetTruffleRuntimeNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimGetTruffleRuntimeNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            return JavaObjectWrapper.wrap(Truffle.getRuntime());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetCallTarget")
    protected abstract static class PrimGetCallTargetNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimGetCallTargetNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final CompiledCodeObject code) {
            return JavaObjectWrapper.wrap(code.getCallTarget());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetVMObject")
    protected abstract static class PrimGetVMObjectNode extends AbstractPrimitiveNode implements BinaryPrimitiveWithoutFallback {
        protected PrimGetVMObjectNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver, final Object target) {
            return JavaObjectWrapper.wrap(target);
        }
    }
}
