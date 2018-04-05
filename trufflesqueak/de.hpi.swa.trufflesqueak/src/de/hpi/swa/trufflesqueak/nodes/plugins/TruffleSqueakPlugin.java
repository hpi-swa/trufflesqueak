package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public final class TruffleSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return TruffleSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "debugPrint", numArguments = 2)
    protected static abstract class PrimPrintArgsNode extends AbstractPrimitiveNode {
        protected PrimPrintArgsNode(CompiledMethodObject code) {
            super(code);
        }

        @TruffleBoundary
        private static void debugPrint(Object o) {
            if (o instanceof NativeObject) {
                System.out.println(((NativeObject) o).toString());
            } else {
                System.out.println(o.toString());
            }
        }

        @Specialization
        protected Object printArgs(Object receiver, Object value) {
            code.image.getOutput().println(value.toString());
            return receiver;
        }
    }
}
