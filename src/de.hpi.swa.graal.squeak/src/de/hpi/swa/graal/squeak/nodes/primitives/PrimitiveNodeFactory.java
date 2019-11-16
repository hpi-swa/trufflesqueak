/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes.primitives;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNodes.AbstractArgumentNode;
import de.hpi.swa.graal.squeak.nodes.context.ArgumentNodes.ArgumentNode;
import de.hpi.swa.graal.squeak.nodes.plugins.B2DPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.BMPReadWriterPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.ClipboardExtendedPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.CroquetPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.DSAPrims;
import de.hpi.swa.graal.squeak.nodes.plugins.DropPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.FilePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.FloatArrayPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.GraalSqueakPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.JPEGReaderPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.LargeIntegers;
import de.hpi.swa.graal.squeak.nodes.plugins.LocalePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.Matrix2x3Plugin;
import de.hpi.swa.graal.squeak.nodes.plugins.MiscPrimitivePlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.NullPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.PolyglotPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.SecurityPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.SoundCodecPrims;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakFFIPrims;
import de.hpi.swa.graal.squeak.nodes.plugins.SqueakSSL;
import de.hpi.swa.graal.squeak.nodes.plugins.UUIDPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.UnixOSProcessPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.Win32OSProcessPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.ZipPlugin;
import de.hpi.swa.graal.squeak.nodes.plugins.network.SocketPlugin;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArithmeticPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ArrayStreamPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.BlockClosurePrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ContextPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.ControlPrimitivesFactory;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.IOPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.MiscellaneousPrimitives;
import de.hpi.swa.graal.squeak.nodes.primitives.impl.StoragePrimitives;
import de.hpi.swa.graal.squeak.util.OSDetector;

public final class PrimitiveNodeFactory {
    private static final int PRIMITIVE_EXTERNAL_CALL_INDEX = 117;
    private static final int PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX = 264;
    private static final int PRIMITIVE_LOAD_INST_VAR_UPPER_INDEX = 520;
    private static final int MAX_PRIMITIVE_INDEX = 575;
    @CompilationFinal(dimensions = 1) private static final byte[] NULL_MODULE_NAME = NullPlugin.class.getSimpleName().getBytes();

    // Using an array instead of a HashMap requires type-checking to be disabled here.
    @SuppressWarnings("unchecked") @CompilationFinal(dimensions = 1) private final NodeFactory<? extends AbstractPrimitiveNode>[] primitiveTable = (NodeFactory<? extends AbstractPrimitiveNode>[]) new NodeFactory<?>[MAX_PRIMITIVE_INDEX];

    private final EconomicMap<String, UnmodifiableEconomicMap<String, NodeFactory<? extends AbstractPrimitiveNode>>> pluginsMap = EconomicMap.create();

    @TruffleBoundary
    public void initialize(final SqueakImageContext image) {
        final AbstractPrimitiveFactoryHolder[] indexPrimitives = new AbstractPrimitiveFactoryHolder[]{
                        new ArithmeticPrimitives(),
                        new ArrayStreamPrimitives(),
                        new BlockClosurePrimitives(),
                        new ContextPrimitives(),
                        new ControlPrimitives(),
                        new IOPrimitives(),
                        new MiscellaneousPrimitives(),
                        new StoragePrimitives()};
        fillPrimitiveTable(indexPrimitives);

        final AbstractPrimitiveFactoryHolder[] plugins = new AbstractPrimitiveFactoryHolder[]{
                        new B2DPlugin(),
                        new BitBltPlugin(),
                        new BMPReadWriterPlugin(),
                        new ClipboardExtendedPlugin(),
                        new CroquetPlugin(),
                        new DropPlugin(),
                        new DSAPrims(),
                        new FilePlugin(),
                        new FloatArrayPlugin(),
                        new GraalSqueakPlugin(),
                        new HostWindowPlugin(),
                        new JPEGReaderPlugin(),
                        new LargeIntegers(),
                        new LocalePlugin(),
                        new Matrix2x3Plugin(),
                        new MiscPrimitivePlugin(),
                        new NullPlugin(),
                        new PolyglotPlugin(),
                        new SecurityPlugin(),
                        new SocketPlugin(),
                        new SoundCodecPrims(),
                        new SqueakFFIPrims(),
                        new SqueakSSL(),
                        new UUIDPlugin(),
                        new ZipPlugin(),
                        OSDetector.SINGLETON.isWindows() ? new Win32OSProcessPlugin() : new UnixOSProcessPlugin()};
        fillPrimitiveTable(plugins);
        fillPluginMap(image, plugins);
    }

    public AbstractPrimitiveNode forIndex(final CompiledMethodObject method, final int primitiveIndex) {
        CompilerAsserts.neverPartOfCompilation("Primitive node instantiation should never happen on fast path");
        assert primitiveIndex >= 0 : "Unexpected negative primitiveIndex";
        if (primitiveIndex == PRIMITIVE_EXTERNAL_CALL_INDEX) {
            return namedFor(method);
        } else if (PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX <= primitiveIndex && primitiveIndex <= PRIMITIVE_LOAD_INST_VAR_UPPER_INDEX) {
            return ControlPrimitivesFactory.PrimLoadInstVarNodeFactory.create(method, primitiveIndex - PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX, new AbstractArgumentNode[]{new ArgumentNode(0)});
        } else if (primitiveIndex <= MAX_PRIMITIVE_INDEX) {
            final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = primitiveTable[primitiveIndex - 1];
            if (nodeFactory != null) {
                return createInstance(method, nodeFactory);
            }
        }
        return null;
    }

    public AbstractPrimitiveNode namedFor(final CompiledMethodObject method) {
        final Object[] values = ((ArrayObject) method.getLiteral(0)).getObjectStorage();
        if (values[1] == NilObject.SINGLETON) {
            return null;
        } else if (values[0] == NilObject.SINGLETON) {
            final NativeObject functionName = (NativeObject) values[1];
            return forName(method, NULL_MODULE_NAME, functionName.getByteStorage());
        } else {
            final NativeObject moduleName = (NativeObject) values[0];
            final NativeObject functionName = (NativeObject) values[1];
            return forName(method, moduleName.getByteStorage(), functionName.getByteStorage());
        }
    }

    private AbstractPrimitiveNode forName(final CompiledMethodObject method, final byte[] moduleName, final byte[] functionName) {
        CompilerAsserts.neverPartOfCompilation("Primitive node instantiation should never happen on fast path");
        final UnmodifiableEconomicMap<String, NodeFactory<? extends AbstractPrimitiveNode>> functionNameToNodeFactory = pluginsMap.get(new String(moduleName));
        if (functionNameToNodeFactory != null) {
            final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = functionNameToNodeFactory.get(new String(functionName));
            if (nodeFactory != null) {
                return createInstance(method, nodeFactory);
            }
        }
        return null;
    }

    public String[] getPluginNames() {
        final Set<String> target = new HashSet<>();
        for (final String key : pluginsMap.getKeys()) {
            target.add(key);
        }
        return target.toArray(new String[target.size()]);
    }

    private static AbstractPrimitiveNode createInstance(final CompiledMethodObject method, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        final int primitiveArity = nodeFactory.getExecutionSignature().size();
        final AbstractArgumentNode[] argumentNodes = new AbstractArgumentNode[primitiveArity];
        for (int i = 0; i < primitiveArity; i++) {
            argumentNodes[i] = AbstractArgumentNode.create(i, method.getNumArgs());
        }
        final AbstractPrimitiveNode primitiveNode = nodeFactory.createNode(method, argumentNodes);
        assert primitiveArity == primitiveNode.getNumArguments() : "Arities do not match in " + primitiveNode;
        return primitiveNode;
    }

    private void fillPrimitiveTable(final AbstractPrimitiveFactoryHolder[] primitiveFactories) {
        for (final AbstractPrimitiveFactoryHolder primitiveFactory : primitiveFactories) {
            final List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = primitiveFactory.getFactories();
            for (final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                if (primitive == null) {
                    continue;
                }
                for (final int index : primitive.indices()) {
                    addEntryToPrimitiveTable(index, nodeFactory);
                }
            }
        }
    }

    private void fillPluginMap(final SqueakImageContext image, final AbstractPrimitiveFactoryHolder[] plugins) {
        for (final AbstractPrimitiveFactoryHolder plugin : plugins) {
            if (!plugin.isEnabled(image)) {
                continue;
            }
            final List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = plugin.getFactories();
            final EconomicMap<String, NodeFactory<? extends AbstractPrimitiveNode>> functionNameToNodeFactory = EconomicMap.create(nodeFactories.size());
            for (final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                for (final String name : primitive.names()) {
                    assert !functionNameToNodeFactory.containsKey(name) : "Primitive name is not unique: " + name;
                    functionNameToNodeFactory.put(name, nodeFactory);
                }
            }
            final String pluginName = plugin.getClass().getSimpleName();
            pluginsMap.put(pluginName, EconomicMap.create(functionNameToNodeFactory));
        }
    }

    private void addEntryToPrimitiveTable(final int index, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        assert index < MAX_PRIMITIVE_INDEX : "primitive table array not large enough";
        assert primitiveTable[index - 1] == null : "primitives are not allowed to override others (#" + index + ")";
        primitiveTable[index - 1] = nodeFactory;
    }
}
