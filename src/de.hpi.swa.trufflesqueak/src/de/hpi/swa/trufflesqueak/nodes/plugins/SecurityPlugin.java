/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class SecurityPlugin extends AbstractPrimitiveFactoryHolder {
    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveCanWriteImage")
    protected abstract static class PrimCanWriteImageNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object doCanWrite(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return BooleanObject.wrap(image.env.getCurrentWorkingDirectory().isWritable());
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveDisableImageWrite")
    protected abstract static class PrimDisableImageWriteNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object doDisable(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetSecureUserDirectory")
    protected abstract static class PrimGetSecureUserDirectoryNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            throw PrimitiveFailed.GENERIC_ERROR; // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetUntrustedUserDirectory")
    protected abstract static class PrimGetUntrustedUserDirectoryNode extends AbstractPrimitiveNode {
        @Specialization
        protected static final NativeObject doGet(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.getResourcesDirectory();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SecurityPluginFactory.getFactories();
    }
}
