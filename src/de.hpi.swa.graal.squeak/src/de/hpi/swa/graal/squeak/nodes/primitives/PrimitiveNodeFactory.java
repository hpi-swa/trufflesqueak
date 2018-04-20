package de.hpi.swa.graal.squeak.nodes.primitives;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.ReceiverAndArgumentsNode;
import de.hpi.swa.graal.squeak.nodes.plugins.FilePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.FloatArrayPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.LargeIntegers;
import de.hpi.swa.graal.squeak.nodes.plugins.MiscPrimitivePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.GraalSqueakPlugin;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArrayStreamPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.BlockClosurePrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives.PrimitiveFailedNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.IOPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.StoragePrimitives;

public abstract class PrimitiveNodeFactory {
    @CompilationFinal(dimensions = 1) private static final AbstractPrimitiveFactoryHolder[] indexPrimitives = new AbstractPrimitiveFactoryHolder[]{
                    new ArithmeticPrimitives(),
                    new ArrayStreamPrimitives(),
                    new BlockClosurePrimitives(),
                    new ControlPrimitives(),
                    new IOPrimitives(),
                    new MiscellaneousPrimitives(),
                    new StoragePrimitives()};
    @CompilationFinal(dimensions = 1) private static final AbstractPrimitiveFactoryHolder[] plugins = new AbstractPrimitiveFactoryHolder[]{
                    new FilePlugin(),
                    new FloatArrayPlugin(),
                    new LargeIntegers(),
                    new MiscPrimitivePlugin(),
                    new GraalSqueakPlugin()};
    @CompilationFinal(dimensions = 1) private static final String[] simulatedPlugins = new String[]{"BitBltPlugin", "B2DPlugin", "BalloonPlugin"};
    @CompilationFinal private static final Map<Integer, NodeFactory<? extends AbstractPrimitiveNode>> primitiveTable;

    static {
        primitiveTable = new HashMap<>();
        fillPrimitiveTable(indexPrimitives);
        fillPrimitiveTable(plugins);
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode forIndex(final CompiledMethodObject method, final int primitiveIndex) {
        if (264 <= primitiveIndex && primitiveIndex <= 520) {
            return ControlPrimitives.PrimQuickReturnReceiverVariableNode.create(method, primitiveIndex - 264);
        }
        final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = primitiveTable.get(primitiveIndex);
        if (nodeFactory != null) {
            return createInstance(method, nodeFactory, nodeFactory.getNodeClass().getAnnotation(SqueakPrimitive.class));
        }
        return null;
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode forName(final CompiledMethodObject method, final String moduleName, final String functionName) {
        for (int i = 0; i < simulatedPlugins.length; i++) {
            if (moduleName.equals(simulatedPlugins[i])) {
                return SimulationPrimitiveNode.create(method, moduleName, functionName, new SqueakNode[]{ReceiverAndArgumentsNode.create(method)});
            }
        }
        for (AbstractPrimitiveFactoryHolder plugin : plugins) {
            if (!plugin.getClass().getSimpleName().equals(moduleName)) {
                continue;
            }
            try {
                final List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = plugin.getFactories();
                for (NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                    final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                    final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                    if (functionName.equals(primitive.name())) {
                        return createInstance(method, nodeFactory, primitive);
                    }
                }
            } catch (RuntimeException e) {
                break;
            }
        }
        return PrimitiveFailedNode.create(method);
    }

    public static Set<String> getPluginNames() {
        final HashSet<String> names = new HashSet<>(plugins.length);
        for (AbstractPrimitiveFactoryHolder plugin : plugins) {
            names.add(plugin.getClass().getSimpleName());
        }
        for (int i = 0; i < simulatedPlugins.length; i++) {
            names.add(simulatedPlugins[i] + " (simulated)");
        }
        return names;
    }

    private static AbstractPrimitiveNode createInstance(final CompiledMethodObject method, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory, final SqueakPrimitive primitive) {
        if (primitive.variableArguments()) {
            return nodeFactory.createNode(method, new SqueakNode[]{ReceiverAndArgumentsNode.create(method)});
        }
        final int numArgs = primitive.numArguments();
        final SqueakNode[] arguments = new SqueakNode[numArgs];
        for (int i = 0; i < numArgs; i++) {
            arguments[i] = ArgumentNode.create(method, i);
        }
        return nodeFactory.createNode(method, arguments);
    }

    private static void fillPrimitiveTable(final AbstractPrimitiveFactoryHolder[] primitiveFactories) {
        for (AbstractPrimitiveFactoryHolder primitiveFactory : primitiveFactories) {
            final List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = primitiveFactory.getFactories();
            for (NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                if (primitive == null) {
                    continue;
                }
                if (primitive.index() > 0) {
                    addEntryToPrimitiveTable(primitive.index(), nodeFactory);
                }
                for (int index : primitive.indices()) {
                    addEntryToPrimitiveTable(index, nodeFactory);
                }
            }
        }
    }

    private static void addEntryToPrimitiveTable(final int index, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        assert !primitiveTable.containsKey(index); // primitives are not allowed to override others
        primitiveTable.put(index, nodeFactory);
    }
}
