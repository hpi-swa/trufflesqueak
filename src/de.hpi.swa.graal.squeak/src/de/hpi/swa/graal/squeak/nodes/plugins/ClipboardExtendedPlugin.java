/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.BinaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.QuaternaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.TernaryPrimitive;
import de.hpi.swa.graal.squeak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class ClipboardExtendedPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioAddClipboardData")
    protected abstract static class PrimIOAddClipboardDataNode extends AbstractPrimitiveNode implements QuaternaryPrimitive {
        protected PrimIOAddClipboardDataNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doAdd(final PointersObject receiver, final Object clipboard, final Object data, final Object dataFormat) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioClearClipboard")
    protected abstract static class PrimIOClearClipboardNode extends AbstractPrimitiveNode implements BinaryPrimitive {
        protected PrimIOClearClipboardNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doClear(final PointersObject receiver, final Object clipboard) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioCreateClipboard")
    protected abstract static class PrimIOCreateClipboardNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        protected PrimIOCreateClipboardNode(final CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected static final long doCreate(@SuppressWarnings("unused") final Object receiver) {
            return 0L; /* See ExtendedClipboardInterface>>initialize. */
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioGetClipboardFormat")
    protected abstract static class PrimIOGetClipboardFormatNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimIOGetClipboardFormatNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doGet(final PointersObject receiver, final Object clipboard, final long formatNumber) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioReadClipboardData")
    protected abstract static class PrimIOReadClipboardDataNode extends AbstractPrimitiveNode implements TernaryPrimitive {
        protected PrimIOReadClipboardDataNode(final CompiledMethodObject method) {
            super(method);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doRead(final PointersObject receiver, final Object clipboard, final Object dataFormat) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return ClipboardExtendedPluginFactory.getFactories();
    }
}
