package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.graal.squeak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.graal.squeak.nodes.primitives.SqueakPrimitive;

public class MiscPrimitivePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscPrimitivePluginFactory.getFactories();
    }

    public abstract static class AbstractMiscPrimitiveNode extends AbstractPrimitiveNode {
        @Child protected NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        public AbstractMiscPrimitiveNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        protected final boolean isASCIIOrder(final NativeObject order) {
            final byte[] bytes = getBytesNode.execute(order);
            for (int i = 0; i < 256; i++) {
                if (bytes[i] != (byte) i) {
                    return false;
                }
            }
            return true;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCompareString")
    public abstract static class PrimCompareStringNode extends AbstractMiscPrimitiveNode {

        public PrimCompareStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "isASCIIOrder(order)")
        protected final long doAsciiOrder(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string1, final NativeObject string2,
                        @SuppressWarnings("unused") final NativeObject order) {
            final byte[] bytes1 = getBytesNode.execute(string1);
            final byte[] bytes2 = getBytesNode.execute(string2);
            final int length1 = bytes1.length;
            final int length2 = bytes2.length;
            final int minLength = Math.min(bytes1.length, length2);
            for (int i = 0; i < minLength; i++) {
                final byte c1 = bytes1[i];
                final byte c2 = bytes2[i];
                if (c1 != c2) {
                    if (c1 < c2) {
                        return 1;
                    } else {
                        return 3;
                    }
                }
            }
            if (length1 == length2) {
                return 2;
            } else if (length1 < length2) {
                return 1;
            } else {
                return 3;
            }
        }

        @Specialization(guards = "!isASCIIOrder(order)")
        protected final long doCollated(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string1, final NativeObject string2, final NativeObject order) {
            final byte[] bytes1 = getBytesNode.execute(string1);
            final byte[] bytes2 = getBytesNode.execute(string2);
            final byte[] orderBytes = getBytesNode.execute(order);
            final int length1 = bytes1.length;
            final int length2 = bytes2.length;
            final int minLength = Math.min(length1, length2);
            for (int i = 0; i < minLength; i++) {
                final byte c1 = orderBytes[bytes1[i]];
                final byte c2 = orderBytes[bytes2[i]];
                if (c1 != c2) {
                    if (c1 < c2) {
                        return 1;
                    } else {
                        return 3;
                    }
                }
            }
            if (length1 == length2) {
                return 2;
            } else if (length1 < length2) {
                return 1;
            } else {
                return 3;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCompressToByteArray")
    public abstract static class PrimCompressToByteArrayNode extends AbstractMiscPrimitiveNode {

        public PrimCompressToByteArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object compress(final AbstractSqueakObject bitmap, final Object bm, final Object from) {
            // TODO: implement primitive
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDecompressFromByteArray")
    public abstract static class PrimDecompressFromByteArrayNode extends AbstractMiscPrimitiveNode {

        public PrimDecompressFromByteArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization
        protected static final Object decompress(final AbstractSqueakObject bitmap, final Object bm, final Object from, final long index) {
            // TODO: implement primitive
            throw new PrimitiveFailed();
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFindFirstInString")
    public abstract static class PrimFindFirstInStringNode extends AbstractMiscPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child private SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();
        @Child private NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        public PrimFindFirstInStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doFind(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
            if (sizeNode.execute(inclusionMap) != 256) {
                return 0;
            }
            final byte[] stringBytes = getBytesNode.execute(string);
            final int stringSize = stringBytes.length;
            int index = (int) start;
            for (; index <= stringSize; index++) {
                if ((long) at0Node.execute(inclusionMap, (long) at0Node.execute(string, index - 1)) != 0) {
                    break;
                }
            }
            if (index > stringSize) {
                return 0;
            } else {
                return index;
            }
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFindSubstring")
    public abstract static class PrimFindSubstringNode extends AbstractMiscPrimitiveNode {

        public PrimFindSubstringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "isASCIIOrder(matchTable)")
        protected long doFindAscii(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject key, final NativeObject body, final long start,
                        @SuppressWarnings("unused") final NativeObject matchTable) {
            return body.toString().indexOf(key.toString(), (int) start - 1) + 1;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isASCIIOrder(matchTable)")
        protected long doFindWithMatchTable(final AbstractSqueakObject receiver, final NativeObject key, final NativeObject body, final long start, final NativeObject matchTable) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveIndexOfAsciiInString")
    public abstract static class PrimIndexOfAsciiInStringNode extends AbstractMiscPrimitiveNode {
        @Child private NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        public PrimIndexOfAsciiInStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doNativeObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long value, final NativeObject string, final long start) {
            if (start < 0) {
                throw new PrimitiveFailed();
            }
            final byte[] bytes = getBytesNode.execute(string);
            for (int i = (int) (start - 1); i < bytes.length; i++) {
                if (bytes[i] == value) {
                    return i + 1;
                }
            }
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveStringHash")
    public abstract static class PrimStringHashNode extends AbstractMiscPrimitiveNode {
        @Child private NativeGetBytesNode getBytesNode = NativeGetBytesNode.create();

        public PrimStringHashNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected long doNativeObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string, final long initialHash) {
            long hash = initialHash & 0xfffffff;
            long low;
            for (byte value : getBytesNode.execute(string)) {
                hash += value & 0xff;
                low = hash & 16383;
                hash = (0x260D * low + (((0x260d * (hash >> 14) + (0x0065 * low)) & 16383) * 16384)) & 0x0fffffff;
            }
            return hash;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTranslateStringWithTable")
    public abstract static class PrimTranslateStringWithTableNode extends AbstractMiscPrimitiveNode {
        private final ValueProfile storageType = ValueProfile.createClassProfile();
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();

        public PrimTranslateStringWithTableNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "string.isByteType()")
        protected NativeObject doNativeObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string, final long start, final long stop, final NativeObject table) {
            final byte[] bytes = string.getByteStorage(storageType);
            for (int i = (int) start - 1; i < stop; i++) {
                final Long tableValue = (Long) at0Node.execute(table, (long) at0Node.execute(string, i));
                bytes[i] = tableValue.byteValue();
            }
            return string;
        }
    }
}
