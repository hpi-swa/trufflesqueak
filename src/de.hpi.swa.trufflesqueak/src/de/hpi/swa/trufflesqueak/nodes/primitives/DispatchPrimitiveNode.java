/*
 * Copyright (c) 2024-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2024-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.context.ArgumentNodes.AbstractArgumentNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive9;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive11;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive8;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive10;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory.ArgumentsLocation;

public abstract class DispatchPrimitiveNode extends AbstractNode {
    @Child protected AbstractPrimitiveNode primitiveNode;

    private DispatchPrimitiveNode(final AbstractPrimitiveNode primitiveNode) {
        this.primitiveNode = primitiveNode;
    }

    public static DispatchPrimitiveNode create(final AbstractPrimitiveNode primitiveNode, final ArgumentsLocation location, final int numReceiverAndArguments) {
        final AbstractArgumentNode[] argumentNodes = createArgumentNodes(location, numReceiverAndArguments);
        return switch (numReceiverAndArguments) {
            case 1 -> new DispatchPrimitive1Node(primitiveNode, argumentNodes);
            case 2 -> new DispatchPrimitive2Node(primitiveNode, argumentNodes);
            case 3 -> new DispatchPrimitive3Node(primitiveNode, argumentNodes);
            case 4 -> new DispatchPrimitive4Node(primitiveNode, argumentNodes);
            case 5 -> new DispatchPrimitive5Node(primitiveNode, argumentNodes);
            case 6 -> new DispatchPrimitive6Node(primitiveNode, argumentNodes);
            case 7 -> new DispatchPrimitive7Node(primitiveNode, argumentNodes);
            case 8 -> new DispatchPrimitive8Node(primitiveNode, argumentNodes);
            case 9 -> new DispatchPrimitive9Node(primitiveNode, argumentNodes);
            case 10 -> new DispatchPrimitive10Node(primitiveNode, argumentNodes);
            case 11 -> new DispatchPrimitive11Node(primitiveNode, argumentNodes);
            case 12 -> new DispatchPrimitive12Node(primitiveNode, argumentNodes);
            default -> throw SqueakException.create("Unexpected number of arguments " + numReceiverAndArguments);
        };
    }

    private static AbstractArgumentNode[] createArgumentNodes(final ArgumentsLocation location, final int numReceiverAndArguments) {
        final AbstractArgumentNode[] argumentNodes = new AbstractArgumentNode[numReceiverAndArguments];
        final boolean useStack = location == ArgumentsLocation.ON_STACK || location == ArgumentsLocation.ON_STACK_REVERSED;
        final int offset = location == ArgumentsLocation.ON_STACK_REVERSED ? numReceiverAndArguments : 0;
        for (int i = 0; i < numReceiverAndArguments; i++) {
            argumentNodes[i] = AbstractArgumentNode.create(i - offset, useStack);
        }
        return argumentNodes;
    }

    public abstract Object execute(VirtualFrame frame);

    public String getPrimitiveNodeClassSimpleName() {
        return primitiveNode.getClass().getSimpleName();
    }

    private static final class DispatchPrimitive1Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;

        private DispatchPrimitive1Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive0) primitiveNode).execute(frame, receiverNode.execute(frame));
        }
    }

    private static final class DispatchPrimitive2Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;

        private DispatchPrimitive2Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive1) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive3Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;

        private DispatchPrimitive3Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive2) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive4Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;

        private DispatchPrimitive4Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive3) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive5Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;

        private DispatchPrimitive5Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive4) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive6Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;

        private DispatchPrimitive6Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive5) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive7Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;
        @Child private AbstractArgumentNode argument6Node;

        private DispatchPrimitive7Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
            argument6Node = argumentNodes[6];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive6) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame), argument6Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive8Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;
        @Child private AbstractArgumentNode argument6Node;
        @Child private AbstractArgumentNode argument7Node;

        private DispatchPrimitive8Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
            argument6Node = argumentNodes[6];
            argument7Node = argumentNodes[7];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive7) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame), argument6Node.execute(frame), argument7Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive9Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;
        @Child private AbstractArgumentNode argument6Node;
        @Child private AbstractArgumentNode argument7Node;
        @Child private AbstractArgumentNode argument8Node;

        private DispatchPrimitive9Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
            argument6Node = argumentNodes[6];
            argument7Node = argumentNodes[7];
            argument8Node = argumentNodes[8];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive8) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame), argument6Node.execute(frame), argument7Node.execute(frame), argument8Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive10Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;
        @Child private AbstractArgumentNode argument6Node;
        @Child private AbstractArgumentNode argument7Node;
        @Child private AbstractArgumentNode argument8Node;
        @Child private AbstractArgumentNode argument9Node;

        private DispatchPrimitive10Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
            argument6Node = argumentNodes[6];
            argument7Node = argumentNodes[7];
            argument8Node = argumentNodes[8];
            argument9Node = argumentNodes[9];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive9) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame), argument6Node.execute(frame), argument7Node.execute(frame), argument8Node.execute(frame),
                            argument9Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive11Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;
        @Child private AbstractArgumentNode argument6Node;
        @Child private AbstractArgumentNode argument7Node;
        @Child private AbstractArgumentNode argument8Node;
        @Child private AbstractArgumentNode argument9Node;
        @Child private AbstractArgumentNode argument10Node;

        private DispatchPrimitive11Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
            argument6Node = argumentNodes[6];
            argument7Node = argumentNodes[7];
            argument8Node = argumentNodes[8];
            argument9Node = argumentNodes[9];
            argument10Node = argumentNodes[10];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive10) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame), argument6Node.execute(frame), argument7Node.execute(frame), argument8Node.execute(frame),
                            argument9Node.execute(frame), argument10Node.execute(frame));
        }
    }

    private static final class DispatchPrimitive12Node extends DispatchPrimitiveNode {
        @Child private AbstractArgumentNode receiverNode;
        @Child private AbstractArgumentNode argument1Node;
        @Child private AbstractArgumentNode argument2Node;
        @Child private AbstractArgumentNode argument3Node;
        @Child private AbstractArgumentNode argument4Node;
        @Child private AbstractArgumentNode argument5Node;
        @Child private AbstractArgumentNode argument6Node;
        @Child private AbstractArgumentNode argument7Node;
        @Child private AbstractArgumentNode argument8Node;
        @Child private AbstractArgumentNode argument9Node;
        @Child private AbstractArgumentNode argument10Node;
        @Child private AbstractArgumentNode argument11Node;

        private DispatchPrimitive12Node(final AbstractPrimitiveNode primitiveNode, final AbstractArgumentNode[] argumentNodes) {
            super(primitiveNode);
            receiverNode = argumentNodes[0];
            argument1Node = argumentNodes[1];
            argument2Node = argumentNodes[2];
            argument3Node = argumentNodes[3];
            argument4Node = argumentNodes[4];
            argument5Node = argumentNodes[5];
            argument6Node = argumentNodes[6];
            argument7Node = argumentNodes[7];
            argument8Node = argumentNodes[8];
            argument9Node = argumentNodes[9];
            argument10Node = argumentNodes[10];
            argument11Node = argumentNodes[11];
        }

        @Override
        public Object execute(final VirtualFrame frame) {
            return ((Primitive11) primitiveNode).execute(frame, receiverNode.execute(frame), argument1Node.execute(frame), argument2Node.execute(frame), argument3Node.execute(frame),
                            argument4Node.execute(frame), argument5Node.execute(frame), argument6Node.execute(frame), argument7Node.execute(frame), argument8Node.execute(frame),
                            argument9Node.execute(frame), argument10Node.execute(frame), argument11Node.execute(frame));
        }
    }
}
