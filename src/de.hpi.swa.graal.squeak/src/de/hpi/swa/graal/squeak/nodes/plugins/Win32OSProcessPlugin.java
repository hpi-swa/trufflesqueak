package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class Win32OSProcessPlugin extends AbstractOSProcessPlugin {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        final List<NodeFactory<? extends AbstractPrimitiveNode>> factories = new ArrayList<>();
        factories.addAll(Win32OSProcessPluginFactory.getFactories());
        factories.addAll(AbstractOSProcessPluginFactory.getFactories());
        return factories;
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveGetEnvironmentStrings")
    protected abstract static class PrimGetEnvironmentStringNode extends AbstractPrimitiveNode {
        protected PrimGetEnvironmentStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        @TruffleBoundary
        protected final Object doGet(@SuppressWarnings("unused") final Object receiver) {
            final Map<String, String> envMap = System.getenv();
            final List<String> strings = new ArrayList<>();
            for (Map.Entry<String, String> entry : envMap.entrySet()) {
                strings.add(entry.getKey() + "=" + entry.getValue());
            }
            return code.image.wrap(String.join("\n", strings));
        }
    }
}
