/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.graalvm.collections.EconomicMap;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeFactory;

import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.plugins.B2DPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.BMPReadWriterPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.BitBltPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.ClipboardExtendedPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.CroquetPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.DSAPrims;
import de.hpi.swa.trufflesqueak.nodes.plugins.DropPlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.FilePlugin;
import de.hpi.swa.trufflesqueak.nodes.plugins.Float64ArrayPlugin;
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
import de.hpi.swa.trufflesqueak.nodes.plugins.ffi.PrimExternalCallNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.network.SocketPlugin;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArithmeticPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ArrayStreamPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.BlockClosurePrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ContextPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.ControlPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.IOPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.MiscellaneousPrimitives;
import de.hpi.swa.trufflesqueak.nodes.primitives.impl.StoragePrimitives;
import de.hpi.swa.trufflesqueak.util.OS;

public final class PrimitiveNodeFactory {
    public static final int PRIMITIVE_SIMULATION_GUARD_INDEX = 19;
    public static final int PRIMITIVE_EXTERNAL_CALL_INDEX = 117;
    public static final int PRIMITIVE_ENSURE_MARKER_INDEX = 198;
    public static final int PRIMITIVE_ON_DO_MARKER_INDEX = 199;
    private static final int PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX = 264;
    private static final int PRIMITIVE_LOAD_INST_VAR_UPPER_INDEX = 520;
    private static final int NAMED_PRIMITIVE_MODULE_NAME_INDEX = 0;
    private static final int NAMED_PRIMITIVE_FUNCTION_NAME_INDEX = 1;
    private static final int MAX_PRIMITIVE_INDEX = 578;
    private static final String NULL_MODULE_NAME = NullPlugin.class.getSimpleName();

    private static final EconomicMap<Integer, AbstractPrimitiveNode> SINGLETON_PRIMITIVE_TABLE = EconomicMap.create();
    private static final EconomicMap<Integer, EconomicMap<Integer, NodeFactory<? extends AbstractPrimitiveNode>>> PRIMITIVE_TABLE = EconomicMap.create(MAX_PRIMITIVE_INDEX);
    private static final EconomicMap<String, EconomicMap<String, AbstractPrimitiveNode>> SINGLETON_PLUGIN_MAP = EconomicMap.create();
    private static final EconomicMap<String, EconomicMap<String, EconomicMap<Integer, NodeFactory<? extends AbstractPrimitiveNode>>>> PLUGIN_MAP = EconomicMap.create();

    static {
        final AbstractPrimitiveFactoryHolder[] indexPrimitives = {
                        new ArithmeticPrimitives(),
                        new ArrayStreamPrimitives(),
                        new BlockClosurePrimitives(),
                        new ContextPrimitives(),
                        new ControlPrimitives(),
                        new IOPrimitives(),
                        new MiscellaneousPrimitives(),
                        new StoragePrimitives()};
        fillPrimitiveTable(indexPrimitives);

        final AbstractPrimitiveFactoryHolder[] plugins = {
                        new B2DPlugin(),
                        new BitBltPlugin(),
                        new BMPReadWriterPlugin(),
                        new ClipboardExtendedPlugin(),
                        new CroquetPlugin(),
                        new DropPlugin(),
                        new DSAPrims(),
                        new FilePlugin(),
                        new FloatArrayPlugin(),
                        new Float64ArrayPlugin(),
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
                        OS.isWindows() ? new Win32OSProcessPlugin() : new UnixOSProcessPlugin()};
        fillPrimitiveTable(plugins);
        fillPluginMap(plugins);
    }

    private PrimitiveNodeFactory() {
    }

    public static String[] getPluginNames() {
        final Set<String> target = new HashSet<>();
        for (final String key : PLUGIN_MAP.getKeys()) {
            target.add(key);
        }
        return target.toArray(new String[0]);
    }

    public static boolean isNonFailing(final CompiledCodeObject method) {
        final int primitiveIndex = method.primitiveIndex();
        return isLoadInstVar(primitiveIndex) || SINGLETON_PRIMITIVE_TABLE.containsKey(primitiveIndex);
        // checking SINGLETON_PLUGIN_MAP is probably not worth the effort.
    }

    @TruffleBoundary
    public static AbstractPrimitiveNode getOrCreateIndexedOrNamed(final CompiledCodeObject method) {
        assert method.hasPrimitive();
        final int primitiveIndex = method.primitiveIndex();
        final int numReceiverAndArguments = 1 + method.getNumArgs();
        if (primitiveIndex == PRIMITIVE_EXTERNAL_CALL_INDEX) {
            return getOrCreateNamed(method, numReceiverAndArguments);
        } else {
            final AbstractPrimitiveNode primitiveNode = getOrCreateIndexed(primitiveIndex, numReceiverAndArguments);
            if (primitiveNode != null && primitiveNode.acceptsMethod(method)) {
                return primitiveNode;
            } else {
                return null;
            }
        }
    }

    public static AbstractPrimitiveNode getOrCreateIndexed(final int primitiveIndex, final int numReceiverAndArguments) {
        assert primitiveIndex <= MAX_PRIMITIVE_INDEX;
        if (isLoadInstVar(primitiveIndex)) {
            assert numReceiverAndArguments == 1;
            return ControlPrimitives.PrimLoadInstVarNode.create(primitiveIndex - PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX);
        } else {
            final AbstractPrimitiveNode primitiveNode = SINGLETON_PRIMITIVE_TABLE.get(primitiveIndex);
            if (primitiveNode != null) {
                return primitiveNode;
            }
            final EconomicMap<Integer, NodeFactory<? extends AbstractPrimitiveNode>> map = PRIMITIVE_TABLE.get(primitiveIndex);
            if (map == null) {
                return null;
            }
            final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = map.get(numReceiverAndArguments);
            if (nodeFactory != null) {
                return createNode(nodeFactory, numReceiverAndArguments);
            } else {
                return null;
            }
        }
    }

    public static AbstractPrimitiveNode getOrCreateNamed(final CompiledCodeObject method, final int numReceiverAndArguments) {
        assert method.primitiveIndex() == PRIMITIVE_EXTERNAL_CALL_INDEX;
        final Object[] values = ((ArrayObject) method.getLiteral(0)).getObjectStorage();
        if (values[NAMED_PRIMITIVE_FUNCTION_NAME_INDEX] == NilObject.SINGLETON) {
            return null;
        }
        final String moduleName = values[NAMED_PRIMITIVE_MODULE_NAME_INDEX] instanceof final NativeObject m ? m.asStringUnsafe() : NULL_MODULE_NAME;
        final String functionName = ((NativeObject) values[NAMED_PRIMITIVE_FUNCTION_NAME_INDEX]).asStringUnsafe();

        final PrimExternalCallNode externalCallNode = PrimExternalCallNode.load(moduleName, functionName, numReceiverAndArguments);
        if (externalCallNode != null) {
            return externalCallNode;
        }

        // TODO: expand to more args?
        if (numReceiverAndArguments == 1) { // Check for singleton plugin primitive
            final AbstractPrimitiveNode primitiveNode = SINGLETON_PLUGIN_MAP.get(moduleName, EconomicMap.emptyMap()).get(functionName);
            if (primitiveNode != null) {
                return primitiveNode;
            }
        }
        final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory = PLUGIN_MAP.get(moduleName, EconomicMap.emptyMap()).get(functionName, EconomicMap.emptyMap()).get(numReceiverAndArguments);
        if (nodeFactory != null) {
            return createNode(nodeFactory, numReceiverAndArguments);
        }
        return null;
    }

    private static boolean isLoadInstVar(final int primitiveIndex) {
        return PRIMITIVE_LOAD_INST_VAR_LOWER_INDEX <= primitiveIndex && primitiveIndex <= PRIMITIVE_LOAD_INST_VAR_UPPER_INDEX;
    }

    private static AbstractPrimitiveNode createNode(final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory, final int numReceiverAndArguments) {
        assert numReceiverAndArguments == nodeFactory.getExecutionSignature().size();
        return nodeFactory.createNode();
    }

    private static void fillPrimitiveTable(final AbstractPrimitiveFactoryHolder[] primitiveFactories) {
        for (final AbstractPrimitiveFactoryHolder primitiveFactory : primitiveFactories) {
            for (final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : primitiveFactory.getFactories()) {
                final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                for (final int index : primitive.indices()) {
                    addEntryToPrimitiveTable(index, nodeFactory);
                }
            }
            for (final AbstractSingletonPrimitiveNode singletonPrimitiveNode : primitiveFactory.getSingletonPrimitives()) {
                final SqueakPrimitive primitive = singletonPrimitiveNode.getClass().getAnnotation(SqueakPrimitive.class);
                for (final int index : primitive.indices()) {
                    assert !SINGLETON_PRIMITIVE_TABLE.containsKey(index) && !PRIMITIVE_TABLE.containsKey(index);
                    SINGLETON_PRIMITIVE_TABLE.put(index, singletonPrimitiveNode);
                }
            }
        }
    }

    private static void fillPluginMap(final AbstractPrimitiveFactoryHolder[] plugins) {
        for (final AbstractPrimitiveFactoryHolder plugin : plugins) {
            final String pluginName = plugin.getClass().getSimpleName();
            final List<? extends NodeFactory<? extends AbstractPrimitiveNode>> nodeFactories = plugin.getFactories();
            final EconomicMap<String, EconomicMap<Integer, NodeFactory<? extends AbstractPrimitiveNode>>> functionNameToNodeFactory = EconomicMap.create(nodeFactories.size());
            for (final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory : nodeFactories) {
                final Class<? extends AbstractPrimitiveNode> primitiveClass = nodeFactory.getNodeClass();
                final SqueakPrimitive primitive = primitiveClass.getAnnotation(SqueakPrimitive.class);
                for (final String name : primitive.names()) {
                    EconomicMap<Integer, NodeFactory<? extends AbstractPrimitiveNode>> map = functionNameToNodeFactory.get(name);
                    if (map == null) {
                        map = EconomicMap.create();
                        functionNameToNodeFactory.put(name, map);
                    }
                    final int numReceiverAndArguments = nodeFactory.getExecutionSignature().size();
                    assert !map.containsKey(numReceiverAndArguments) : "Primitive name is not unique: " + name;
                    map.put(numReceiverAndArguments, nodeFactory);
                }
            }
            PLUGIN_MAP.put(pluginName, functionNameToNodeFactory);

            final EconomicMap<String, AbstractPrimitiveNode> functionNameToSingletonNode = EconomicMap.create(plugin.getSingletonPrimitives().size());
            for (final AbstractSingletonPrimitiveNode singletonPrimitiveNode : plugin.getSingletonPrimitives()) {
                final SqueakPrimitive primitive = singletonPrimitiveNode.getClass().getAnnotation(SqueakPrimitive.class);
                for (final String name : primitive.names()) {
                    assert !functionNameToSingletonNode.containsKey(name);
                    functionNameToSingletonNode.put(name, singletonPrimitiveNode);
                }
            }
            SINGLETON_PLUGIN_MAP.put(pluginName, functionNameToSingletonNode);
        }
    }

    private static void addEntryToPrimitiveTable(final int index, final NodeFactory<? extends AbstractPrimitiveNode> nodeFactory) {
        assert index <= MAX_PRIMITIVE_INDEX : "primitive table array not large enough";
        assert !SINGLETON_PRIMITIVE_TABLE.containsKey(index);
        EconomicMap<Integer, NodeFactory<? extends AbstractPrimitiveNode>> map = PRIMITIVE_TABLE.get(index);
        if (map == null) {
            map = EconomicMap.create();
            PRIMITIVE_TABLE.put(index, map);
        }
        final int numReceiverAndArguments = nodeFactory.getExecutionSignature().size();
        assert !map.containsKey(numReceiverAndArguments) : "primitives are not allowed to override others (#" + index + ")";
        map.put(nodeFactory.getExecutionSignature().size(), nodeFactory);
    }
}
