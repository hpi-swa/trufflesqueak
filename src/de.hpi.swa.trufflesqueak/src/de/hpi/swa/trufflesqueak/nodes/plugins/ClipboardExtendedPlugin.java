/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3WithFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class ClipboardExtendedPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioAddClipboardData")
    protected abstract static class PrimIOAddClipboardDataNode extends AbstractPrimitiveNode implements Primitive3WithFallback {

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doAdd(final PointersObject receiver, final Object clipboard, final Object data, final Object dataFormat) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioClearClipboard")
    protected abstract static class PrimIOClearClipboardNode extends AbstractPrimitiveNode implements Primitive1WithFallback {

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doClear(final PointersObject receiver, final Object clipboard) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioCreateClipboard")
    protected abstract static class PrimIOCreateClipboardNode extends AbstractPrimitiveNode implements Primitive0 {

        @Specialization
        protected static final long doCreate(@SuppressWarnings("unused") final Object receiver) {
            return 0L; /* See ExtendedClipboardInterface>>initialize. */
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioGetClipboardFormat")
    protected abstract static class PrimIOGetClipboardFormatNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object doGet(final PointersObject receiver, final Object clipboard, final long formatNumber) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "ioReadClipboardData")
    protected abstract static class PrimIOReadClipboardDataNode extends AbstractPrimitiveNode implements Primitive2WithFallback {

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
