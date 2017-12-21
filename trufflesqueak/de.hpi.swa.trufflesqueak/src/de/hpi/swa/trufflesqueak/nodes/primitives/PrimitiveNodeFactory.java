package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.frame.FrameArgumentProfileNode;
import de.hpi.swa.trufflesqueak.nodes.context.stack.PeekStackNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.FilePlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.nodes.plugins.TruffleSqueakPlugin;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArrayStreamPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives.PrimitiveFailedNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.IOPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives;

public abstract class PrimitiveNodeFactory {
    private static AbstractPrimitiveFactoryHolder[] indexPrimitives = new AbstractPrimitiveFactoryHolder[]{
                    new ArithmeticPrimitives(),
                    new ArrayStreamPrimitives(),
                    new BlockClosurePrimitives(),
                    new ControlPrimitives(),
                    new IOPrimitives(),
                    new MiscellaneousPrimitives(),
                    new StoragePrimitives()};
    private static AbstractPrimitiveFactoryHolder[] plugins = new AbstractPrimitiveFactoryHolder[]{
                    new LargeIntegers(),
                    new FilePlugin(),
                    new TruffleSqueakPlugin()};
    private static Map<Integer, NodeFactory<? extends AbstractPrimitiveNode>> primitiveTable;
    private static Map<NativeObject, NodeFactory<? extends AbstractPrimitiveNode>> eagerPrimitiveTable;

    @TruffleBoundary
    public static AbstractPrimitiveNode forIndex(CompiledMethodObject method, int primitiveIndex) {
        if (264 <= primitiveIndex && primitiveIndex <= 520) {
            return ControlPrimitives.PrimQuickReturnReceiverVariableNode.create(method, primitiveIndex - 264);
        }
        try {
            NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = getPrimitiveTable().get(primitiveIndex);
            return createInstance(method, nodeFactory, nodeFactory.getNodeClass().getAnnotation(SqueakPrimitive.class));
        } catch (NullPointerException e) {
            return PrimitiveFailedNode.create(method);
        }
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode forSelector(CompiledCodeObject code, NativeObject nativeObject, int expectedNumArgs) {
        NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = getEagerPrimitiveTable(code).get(nativeObject);
        SqueakPrimitive primitive = nodeFactory.getNodeClass().getAnnotation(SqueakPrimitive.class);
        int numArguments = primitive.numArguments();
        assert numArguments == expectedNumArgs;
        SqueakNode[] arguments = new SqueakNode[numArguments];
        for (int i = 0; i < numArguments; i++) {
            arguments[i] = new PeekStackNode(code, numArguments - 1 - i);
        }
        return nodeFactory.createNode(code, arguments);
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode forName(CompiledMethodObject method, String modulename, String functionname) {
        for (AbstractPrimitiveFactoryHolder plugin : plugins) {
            if (!plugin.getClass().getSimpleName().equals(modulename)) {
                continue;
            }
            try {
                List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = plugin.getFactories();
                for (NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                    Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                    SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                    if (functionname.equals(primitive.name())) {
                        return createInstance(method, nodeFactory, primitive);
                    }
                }
            } catch (RuntimeException e) {
                break;
            }
        }
        return PrimitiveFailedNode.create(method);
    }

    private static AbstractPrimitiveNode createInstance(CompiledMethodObject method, NodeFactory<? extends AbstractPrimitiveNode> nodeFactory, SqueakPrimitive primitive) {
        int numArgs = primitive.numArguments();
        SqueakNode[] arguments = new SqueakNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            arguments[i] = new FrameArgumentProfileNode(new FrameArgumentNode(i));
        }
        return nodeFactory.createNode(method, arguments);
    }

    private static Map<Integer, NodeFactory<? extends AbstractPrimitiveNode>> getPrimitiveTable() {
        if (primitiveTable == null) {
            primitiveTable = new HashMap<>();
            fillPrimitiveTable(indexPrimitives);
            fillPrimitiveTable(plugins);
        }
        return primitiveTable;
    }

    private static void fillPrimitiveTable(AbstractPrimitiveFactoryHolder[] primitiveFactories) {
        for (AbstractPrimitiveFactoryHolder primitiveFactory : primitiveFactories) {
            List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = primitiveFactory.getFactories();
            for (NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                if (primitive != null) {
                    if (primitive.index() > 0) {
                        addEntryToPrimitiveTable(primitive.index(), nodeFactory);
                    }
                    for (int index : primitive.indices()) {
                        addEntryToPrimitiveTable(index, nodeFactory);
                    }
                }
            }
        }
    }

    private static void addEntryToPrimitiveTable(int index, NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        if (primitiveTable.containsKey(index)) {
            throw new RuntimeException(String.format("Failed to register %s as primitive %d, because it is already assigned by %s.", nodeFactory, index, primitiveTable.get(index)));
        }
        primitiveTable.put(index, nodeFactory);
    }

    private static Map<NativeObject, NodeFactory<? extends AbstractPrimitiveNode>> getEagerPrimitiveTable(CompiledCodeObject code) {
        if (eagerPrimitiveTable == null) {
            eagerPrimitiveTable = new HashMap<>();
            // register the first 11 primitives for the first special selectors
            for (int i = 0; i < 11; i++) {
                eagerPrimitiveTable.put(code.image.nativeSpecialSelectors[i], getPrimitiveTable().get(i + 1));
            }
// eagerPrimitiveTable.put(code.image.at, getPrimitiveTable().get(60));
// eagerPrimitiveTable.put(code.image.atput, getPrimitiveTable().get(61));
        }
        return eagerPrimitiveTable;
    }
}
