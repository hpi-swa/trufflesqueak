package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public final class TruffleSqueakPlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return TruffleSqueakPluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "debugPrint")
    protected static abstract class PrimPrintArgs extends AbstractPrimitiveNode {
        protected PrimPrintArgs(CompiledMethodObject code) {
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
        protected Object printArgs(VirtualFrame frame) {
            Object[] arguments = frame.getArguments();
            for (int i = FrameAccess.RCVR_AND_ARGS_START + 1; i < arguments.length; i++) {
                debugPrint(arguments[i]);
            }
            return code.image.nil;
        }
    }
}
