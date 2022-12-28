/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.Arrays;
import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DenyReplace;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractSingletonPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class SecurityPlugin extends AbstractPrimitiveFactoryHolder {

    @DenyReplace
    @SqueakPrimitive(names = "primitiveCanWriteImage")
    public static final class PrimCanWriteImageNode extends AbstractSingletonPrimitiveNode {
        @Override
        public Object execute() {
            return BooleanObject.wrap(getContext().env.getCurrentWorkingDirectory().isWritable());
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

    @DenyReplace
    @SqueakPrimitive(names = "primitiveGetUntrustedUserDirectory")
    public static final class PrimGetUntrustedUserDirectoryNode extends AbstractSingletonPrimitiveNode {
        @Override
        public Object execute() {
            return getContext().getResourcesDirectory();
        }
    }

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return SecurityPluginFactory.getFactories();
    }

    @Override
    public List<Class<? extends AbstractSingletonPrimitiveNode>> getSingletonPrimitives() {
        return Arrays.asList(PrimCanWriteImageNode.class, PrimGetUntrustedUserDirectoryNode.class);
    }
}
