/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.graalvm.collections.EconomicMap;
import org.graalvm.collections.UnmodifiableEconomicMap;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNodes.AbstractArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNodes.ArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.B2DPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.BMPReadWriterPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.BitBltPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.ClipboardExtendedPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.CroquetPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.DSAPrims;
import de.hpi.swa.trufflesqueak.nodes.plugins.DropPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.FilePlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.FloatArrayPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.HostWindowPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.JPEGReadWriter2Plugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.JPEGReaderPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
import de.hpi.swa.trufflesqueak.nodes.plugins.LocalePlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.Matrix2x3Plugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.MiscPrimitivePlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.NullPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.PolyglotPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.SecurityPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.SoundCodecPrims;
import de.hpi.swa.trufflesqueak.nodes.plugins.SqueakFFIPrims;
import de.hpi.swa.trufflesqueak.nodes.plugins.SqueakSSL;
import de.hpi.swa.trufflesqueak.nodes.plugins.TruffleSqueakPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.UUIDPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.UnixOSProcessPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.Win32OSProcessPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.ZipPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.network.SocketPlugin;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArrayStreamPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ContextPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitivesFactory;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.IOPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives;
import de.hpi.swa.trufflesqueak.util.OSDetector;

public final class PrimitiveNodeFactory {
    private static final int PRIMITIVE_EXTERNAL_CALL_INDEX = 117;
    private static final int PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX = 264;
    private static final int PRIMITIVE_LOAD_INST_VAR_UPPER_INDEX = 520;
    private static final int MAX_PRIMITIVE_INDEX = 575;
    @CompilationFinal(dimensions = 1) private static final byte[] NULL_MODULE_NAME = NullPlugin.class.getSimpleName().getBytes();

    // Using an array instead of a HashMap requires type-checking to be disabled here.
    @SuppressWarnings("unchecked") @CompilationFinal(dimensions = 1) private static final NodeFactory<? extends AbstractPrimitiveNode>[] PRIMITIVE_TABLE = (NodeFactory<? extends AbstractPrimitiveNode>[]) new NodeFactory<?>[MAX_PRIMITIVE_INDEX];

    private static final EconomicMap<String, UnmodifiableEconomicMap<String, NodeFactory<? extends AbstractPrimitiveNode>>> PLUGIN_MAP = EconomicMap.create();

    static {
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
                        new TruffleSqueakPlugin(),
                        new HostWindowPlugin(),
                        new JPEGReaderPlugin(),
                        new JPEGReadWriter2Plugin(),
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
        fillPluginMap(plugins);
    }

    private PrimitiveNodeFactory() {
    }

    public static AbstractPrimitiveNode forIndex(final CompiledCodeObject method, final boolean useStack, final int primitiveIndex) {
        CompilerAsserts.neverPartOfCompilation("Primitive node instantiation should never happen on fast path");
        assert primitiveIndex >= 0 : "Unexpected negative primitiveIndex";
        if (primitiveIndex == PRIMITIVE_EXTERNAL_CALL_INDEX) {
            return namedFor(method, useStack);
        } else if (PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX <= primitiveIndex && primitiveIndex <= PRIMITIVE_LOAD_INST_VAR_UPPER_INDEX) {
            return ControlPrimitivesFactory.PrimLoadInstVarNodeFactory.create(primitiveIndex - PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX,
                            new AbstractArgumentNode[]{ArgumentNode.create(0, 0, useStack)});
        } else if (primitiveIndex <= MAX_PRIMITIVE_INDEX) {
            final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = PRIMITIVE_TABLE[primitiveIndex - 1];
            if (nodeFactory != null) {
                return createInstance(method, useStack, nodeFactory);
            }
        }
        return null;
    }

    public static AbstractPrimitiveNode namedFor(final CompiledCodeObject method, final boolean useStack) {
        final Object[] values = ((ArrayObject) method.getLiteral(0)).getObjectStorage();
        if (values[1] == NilObject.SINGLETON) {
            return null;
        } else if (values[0] == NilObject.SINGLETON) {
            final NativeObject functionName = (NativeObject) values[1];
            return forName(method, useStack, NULL_MODULE_NAME, functionName.getByteStorage());
        } else {
            final NativeObject moduleName = (NativeObject) values[0];
            final NativeObject functionName = (NativeObject) values[1];
            return forName(method, useStack, moduleName.getByteStorage(), functionName.getByteStorage());
        }
    }

    private static AbstractPrimitiveNode forName(final CompiledCodeObject method, final boolean useStack, final byte[] moduleName, final byte[] functionName) {
        CompilerAsserts.neverPartOfCompilation("Primitive node instantiation should never happen on fast path");
        final UnmodifiableEconomicMap<String, NodeFactory<? extends AbstractPrimitiveNode>> functionNameToNodeFactory = PLUGIN_MAP.get(new String(moduleName));
        if (functionNameToNodeFactory != null) {
            final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = functionNameToNodeFactory.get(new String(functionName));
            if (nodeFactory != null) {
                return createInstance(method, useStack, nodeFactory);
            }
        }
        return null;
    }

    public static String[] getPluginNames() {
        final Set<String> target = new HashSet<>();
        for (final String key : PLUGIN_MAP.getKeys()) {
            target.add(key);
        }
        return target.toArray(new String[0]);
    }

    private static AbstractPrimitiveNode createInstance(final CompiledCodeObject method, final boolean useStack, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        final int primitiveArity = nodeFactory.getExecutionSignature().size();
        final AbstractArgumentNode[] argumentNodes;
        argumentNodes = new AbstractArgumentNode[primitiveArity];
        for (int i = 0; i < primitiveArity; i++) {
            argumentNodes[i] = AbstractArgumentNode.create(i, method.getNumArgs(), useStack);
        }
        final AbstractPrimitiveNode primitiveNode = nodeFactory.createNode((Object) argumentNodes);
        assert primitiveArity == primitiveNode.getNumArguments() : "Arities do not match in " + primitiveNode;
        if (primitiveNode.acceptsMethod(method)) {
            return primitiveNode;
        } else {
            return null;
        }
    }

    private static void fillPrimitiveTable(final AbstractPrimitiveFactoryHolder[] primitiveFactories) {
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

    private static void fillPluginMap(final AbstractPrimitiveFactoryHolder[] plugins) {
        for (final AbstractPrimitiveFactoryHolder plugin : plugins) {
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
            PLUGIN_MAP.put(pluginName, EconomicMap.create(functionNameToNodeFactory));
        }
    }

    private static void addEntryToPrimitiveTable(final int index, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        assert index <= MAX_PRIMITIVE_INDEX : "primitive table array not large enough";
        assert PRIMITIVE_TABLE[index - 1] == null : "primitives are not allowed to override others (#" + index + ")";
        PRIMITIVE_TABLE[index - 1] = nodeFactory;
    }
}
