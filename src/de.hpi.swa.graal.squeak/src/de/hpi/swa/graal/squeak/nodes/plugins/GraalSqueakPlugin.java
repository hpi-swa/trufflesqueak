package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public final class GraalSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return GraalSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "debugPrint", numArguments = 2)
    protected abstract static class PrimPrintArgsNode extends AbstractPrimitiveNode {
        protected PrimPrintArgsNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @TruffleBoundary
        private void debugPrint(final Object o) {
            if (o instanceof NativeObject) {
                code.image.getOutput().println(((NativeObject) o).toString());
            } else {
                code.image.getOutput().println(o.toString());
            }
        }

        @Specialization
        protected Object printArgs(final Object receiver, final Object value) {
            code.image.getOutput().println(value.toString());
            return receiver;
        }
    }
}
