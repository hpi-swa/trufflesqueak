package de.hpi.swa.graal.squeak.nodes.primitives;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.SqueakNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNode;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.FilePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.FloatArrayPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.GraalSqueakPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.LargeIntegers;
import de.hpi.swa.graal.squeak.nodes.plugins.LocalePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.Matrix2x3Plugin;
import de.hpi.swa.graal.squeak.nodes.plugins.MiscPrimitivePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.PolyglotPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.UUIDPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.UnixOSProcessPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.Win32OSProcessPlugin;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArrayStreamPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.BlockClosurePrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives.PrimitiveFailedNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.IOPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives.SimulationPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.StoragePrimitives;
import de.hpi.swa.graal.squeak.nodes.plugins.SocketPlugin;

public final class PrimitiveNodeFactory {
    private static final int MAX_PRIMITIVE_INDEX = 575;
    @CompilationFinal(dimensions = 1) private static final AbstractPrimitiveFactoryHolder[] indexPrimitives = new AbstractPrimitiveFactoryHolder[]{
                    new ArithmeticPrimitives(),
                    new ArrayStreamPrimitives(),
                    new BlockClosurePrimitives(),
                    new ControlPrimitives(),
                    new IOPrimitives(),
                    new MiscellaneousPrimitives(),
                    new StoragePrimitives()};
    @CompilationFinal(dimensions = 1) private static final AbstractPrimitiveFactoryHolder[] plugins = new AbstractPrimitiveFactoryHolder[]{
                    new BitBltPlugin(),
                    new FilePlugin(),
                    new FloatArrayPlugin(),
                    new GraalSqueakPlugin(),
                    new HostWindowPlugin(),
                    new LargeIntegers(),
                    new LocalePlugin(),
                    new Matrix2x3Plugin(),
                    new MiscPrimitivePlugin(),
                    new PolyglotPlugin(),
                    new UnixOSProcessPlugin(),
                    new UUIDPlugin(),
                    new Win32OSProcessPlugin(),
                    new SocketPlugin()};
    @CompilationFinal(dimensions = 1) private static final String[] simulatedPlugins = new String[]{"B2DPlugin", "BalloonPlugin"};

    // Using an array instead of a HashMap requires type-checking to be disabled here.
    @SuppressWarnings("unchecked") @CompilationFinal(dimensions = 1) private static final NodeFactory<? extends AbstractPrimitiveNode>[] primitiveTable = (NodeFactory<? extends AbstractPrimitiveNode>[]) new NodeFactory<?>[MAX_PRIMITIVE_INDEX];

    static {
        fillPrimitiveTable(indexPrimitives);
        fillPrimitiveTable(plugins);
    }

    private PrimitiveNodeFactory() {
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode forIndex(final CompiledMethodObject method, final int primitiveIndex) {
        if (264 <= primitiveIndex && primitiveIndex <= 520) {
            return ControlPrimitives.PrimQuickReturnReceiverVariableNode.create(method, primitiveIndex - 264);
        }
        final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = getFromPrimitiveTable(primitiveIndex);
        if (nodeFactory != null) {
            return createInstance(method, nodeFactory);
        }
        return null;
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode forName(final CompiledMethodObject method, final String moduleName, final String functionName) {
        for (int i = 0; i < simulatedPlugins.length; i++) {
            if (moduleName.equals(simulatedPlugins[i])) {
                return SimulationPrimitiveNode.create(method, moduleName, functionName);
            }
        }
        for (AbstractPrimitiveFactoryHolder plugin : plugins) {
            final String pluginName = plugin.getClass().getSimpleName();
            if (!pluginName.equals(moduleName)) {
                continue;
            }
            try {
                final List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = plugin.getFactories();
                for (NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                    final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                    final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                    if (functionName.equals(primitive.name())) {
                        return createInstance(method, nodeFactory);
                    }
                }
                if (plugin.useSimulationAsFallback()) {
                    return SimulationPrimitiveNode.create(method, pluginName, functionName);
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

    private static AbstractPrimitiveNode createInstance(final CompiledMethodObject method, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        final int primitiveArity = nodeFactory.getExecutionSignature().size();
        final SqueakNode[] argumentNodes = new SqueakNode[primitiveArity];
        for (int i = 0; i < primitiveArity; i++) {
            argumentNodes[i] = ArgumentNode.create(method, i);
        }
        return nodeFactory.createNode(method, primitiveArity, argumentNodes);
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

    private static NodeFactory<? extends AbstractPrimitiveNode> getFromPrimitiveTable(final int index) {
        if (index <= MAX_PRIMITIVE_INDEX) {
            return primitiveTable[index - 1];
        }
        return null;
    }

    private static void addEntryToPrimitiveTable(final int index, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        assert index < MAX_PRIMITIVE_INDEX : "primitive table array not large enough";
        assert primitiveTable[index - 1] == null : "primitives are not allowed to override others (#" + index + ")";
        primitiveTable[index - 1] = nodeFactory;
    }
}
