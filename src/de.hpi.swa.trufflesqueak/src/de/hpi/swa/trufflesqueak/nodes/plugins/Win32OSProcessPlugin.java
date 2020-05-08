/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.CachedLibrary;

import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitive;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveInterfaces.UnaryPrimitiveWithoutFallback;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class Win32OSProcessPlugin extends AbstractOSProcessPlugin {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        final List<NodeFactory<? extends AbstractPrimitiveNode>> factories = new ArrayList<>();
        factories.addAll(Win32OSProcessPluginFactory.getFactories());
        factories.addAll(AbstractOSProcessPluginFactory.getFactories());
        return factories;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetEnvironmentStrings")
    protected abstract static class PrimGetEnvironmentStringNode extends AbstractPrimitiveNode implements UnaryPrimitiveWithoutFallback {
        @Specialization
        @TruffleBoundary
        protected static final Object doGet(@SuppressWarnings("unused") final Object receiver,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            final Map<String, String> envMap = image.env.getEnvironment();
            final List<String> strings = new ArrayList<>();
            for (final Map.Entry<String, String> entry : envMap.entrySet()) {
                strings.add(entry.getKey() + "=" + entry.getValue());
            }
            return image.asByteString(String.join("\n", strings));
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(names = "primitiveGetMainThreadID")
    protected abstract static class PrimGetMainThreadIDNode extends AbstractSysCallPrimitiveNode implements UnaryPrimitive {
        @Specialization(guards = "supportsNFI")
        protected final long doGetMainThreadID(@SuppressWarnings("unused") final Object receiver,
                        @CachedLibrary("getSysCallObject()") final InteropLibrary lib) {
            return getValue(lib);
        }

        @Override
        protected final String getFunctionName() {
            return "GetCurrentThreadId";
        }
    }
}
