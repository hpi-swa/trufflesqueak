package de.hpi.swa.graal.squeak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.profiles.ValueProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.ERROR_TABLE;
import de.hpi.swa.graal.squeak.nodes.accessing.NativeObjectNodes.NativeGetBytesNode;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectAt0Node;
import de.hpi.swa.graal.squeak.nodes.accessing.SqueakObjectSizeNode;
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
        private final ValueProfile bmStorageType = ValueProfile.createClassProfile();
        private final ValueProfile baStorageType = ValueProfile.createClassProfile();

        public PrimDecompressFromByteArrayNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"bm.isIntType()", "ba.isByteType()"})
        protected final Object doDecompress(final AbstractSqueakObject receiver, final NativeObject bm, final NativeObject ba, final long index) {
            /**
             * <pre>
                 Decompress the body of a byteArray encoded by compressToByteArray (qv)...
                 The format is simply a sequence of run-coded pairs, {N D}*.
                     N is a run-length * 4 + data code.
                     D, the data, depends on the data code...
                         0   skip N words, D is absent
                             (could be used to skip from one raster line to the next)
                         1   N words with all 4 bytes = D (1 byte)
                         2   N words all = D (4 bytes)
                         3   N words follow in D (4N bytes)
                     S and N are encoded as follows (see decodeIntFrom:)...
                         0-223   0-223
                         224-254 (0-30)*256 + next byte (0-7935)
                         255     next 4 bytes
                 NOTE:  If fed with garbage, this routine could read past the end of ba, but it should fail before writing past the ned of bm.
             * </pre>
             */

            final byte[] baBytes = ba.getByteStorage(baStorageType);
            final int[] bmBytes = bm.getIntStorage(bmStorageType);
            int i = (int) index - 1;
            final int end = baBytes.length;
            int k = 0;
            final int pastEnd = bmBytes.length + 1;
            while (i < end) {
                // Decode next run start N
                int anInt = baBytes[i++] & 0xff;
                if (anInt > 223) {
                    if (anInt <= 254) {
                        anInt = (anInt - 224) * 256 + (baBytes[i++] & 0xff);
                    } else {
                        anInt = ((baBytes[i++] & 0xff) << 24) | ((baBytes[i++] & 0xff) << 16) | ((baBytes[i++] & 0xff) << 8) | (baBytes[i++] & 0xff);
                    }
                }
                final long n = anInt >> 2;
                if (k + n > pastEnd) {
                    throw new PrimitiveFailed(ERROR_TABLE.BAD_INDEX);
                }
                switch (anInt & 3) {
                    case 0: // skip
                        break;
                    case 1: { // n consecutive words of 4 bytes = the following byte
                        final int data = ((baBytes[i] & 0xff) << 24) | ((baBytes[i] & 0xff) << 16) | ((baBytes[i] & 0xff) << 8) | (baBytes[i++] & 0xff);
                        for (int j = 0; j < n; j++) {
                            bmBytes[k++] = data;
                        }
                        break;
                    }
                    case 2: { // n consecutive words = 4 following bytes
                        final int data = ((baBytes[i++] & 0xff) << 24) | ((baBytes[i++] & 0xff) << 16) | ((baBytes[i++] & 0xff) << 8) | (baBytes[i++] & 0xff);
                        for (int j = 0; j < n; j++) {
                            bmBytes[k++] = data;
                        }
                        break;
                    }

                    case 3: { // n consecutive words from the data
                        for (int m = 0; m < n; m++) {
                            bmBytes[k++] = ((baBytes[i++] & 0xff) << 24) | ((baBytes[i++] & 0xff) << 16) | ((baBytes[i++] & 0xff) << 8) | (baBytes[i++] & 0xff);
                        }
                        break;
                    }
                    default:
                        throw new SqueakException("primitiveDecompressFromByteArray: should not happen");
                }
            }
            return receiver;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFindFirstInString")
    public abstract static class PrimFindFirstInStringNode extends AbstractMiscPrimitiveNode {
        @Child private SqueakObjectAt0Node at0Node = SqueakObjectAt0Node.create();
        @Child protected SqueakObjectSizeNode sizeNode = SqueakObjectSizeNode.create();

        public PrimFindFirstInStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "sizeNode.execute(inclusionMap) != 256")
        protected static final long doFindNot256(final AbstractSqueakObject receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
            return 0L;
        }

        @Specialization(guards = "sizeNode.execute(inclusionMap) == 256")
        protected final long doFind(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string, final NativeObject inclusionMap, final long start) {
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
    public abstract static class PrimFindSubstringNode extends AbstractPrimitiveNode {
        private final ValueProfile byteType = ValueProfile.createClassProfile();
        private final ValueProfile intType = ValueProfile.createClassProfile();

        public PrimFindSubstringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Override
        public final Object executeWithArguments(final VirtualFrame frame, final Object... arguments) {
            try {
                return executeWithArgumentsSpecialized(frame, arguments);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        @Override
        public final Object executePrimitive(final VirtualFrame frame) {
            try {
                return executeFindSubstring(frame);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new PrimitiveFailed();
            }
        }

        public abstract Object executeFindSubstring(VirtualFrame frame);

        @Specialization(guards = {"key.isByteType()", "body.isByteType()", "matchTable.isByteType()"})
        protected final long doFind(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject key, final NativeObject body, final long start,
                        final NativeObject matchTable) {
            final byte[] keyBytes = key.getByteStorage(byteType);
            final int keyBytesLength = keyBytes.length;
            if (keyBytesLength == 0) {
                return 0L;
            }
            final byte[] bodyBytes = body.getByteStorage(byteType);
            final byte[] matchTableBytes = matchTable.getByteStorage(byteType);
            for (int startIndex = Math.max((int) start - 1, 0); startIndex <= bodyBytes.length - keyBytes.length; startIndex++) {
                int index = 0;
                while (matchTableBytes[bodyBytes[startIndex + index]] == matchTableBytes[keyBytes[index]]) {
                    if (index == keyBytesLength - 1) {
                        return startIndex + 1;
                    } else {
                        index++;
                    }
                }
            }
            return 0L;
        }

        @Specialization(guards = {"key.isByteType()", "body.isIntType()", "matchTable.isByteType()"})
        protected final long doFindWideBody(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject key, final NativeObject body, final long start,
                        final NativeObject matchTable) {
            final byte[] keyBytes = key.getByteStorage(byteType);
            final int keyBytesLength = keyBytes.length;
            if (keyBytesLength == 0) {
                return 0L;
            }
            final int[] bodyBytes = body.getIntStorage(intType);
            final byte[] matchTableBytes = matchTable.getByteStorage(byteType);
            for (int startIndex = Math.max((int) start - 1, 0); startIndex <= bodyBytes.length - keyBytes.length; startIndex++) {
                int index = 0;
                while (matchTableBytes[bodyBytes[startIndex + index]] == matchTableBytes[keyBytes[index]]) {
                    if (index == keyBytesLength - 1) {
                        return startIndex + 1;
                    } else {
                        index++;
                    }
                }
            }
            return 0L;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!key.isByteType() || !matchTable.isByteType()")
        protected static final long doInvalidKey(final AbstractSqueakObject receiver, final NativeObject key, final NativeObject body, final long start, final NativeObject matchTable) {
            throw new PrimitiveFailed(ERROR_TABLE.BAD_ARGUMENT);
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveIndexOfAsciiInString")
    public abstract static class PrimIndexOfAsciiInStringNode extends AbstractMiscPrimitiveNode {

        public PrimIndexOfAsciiInStringNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = "start >= 0")
        protected final long doNativeObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final long value, final NativeObject string, final long start) {
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

        public PrimStringHashNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization
        protected final long doNativeObject(@SuppressWarnings("unused") final AbstractSqueakObject receiver, final NativeObject string, final long initialHash) {
            long hash = initialHash & 0xfffffff;
            long low;
            final byte[] bytes = getBytesNode.execute(string);
            for (int i = 0; i < bytes.length; i++) {
                hash += bytes[i] & 0xff;
                low = hash & 16383;
                hash = (0x260D * low + (((0x260d * (hash >> 14) + (0x0065 * low)) & 16383) * 16384)) & 0x0fffffff;
            }
            return hash;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTranslateStringWithTable")
    public abstract static class PrimTranslateStringWithTableNode extends AbstractMiscPrimitiveNode {
        private final ValueProfile byteType = ValueProfile.createClassProfile();

        public PrimTranslateStringWithTableNode(final CompiledMethodObject method, final int numArguments) {
            super(method, numArguments);
        }

        @Specialization(guards = {"string.isByteType()", "table.isByteType()"})
        protected final AbstractSqueakObject doNativeObject(final AbstractSqueakObject receiver, final NativeObject string, final long start, final long stop,
                        final NativeObject table) {
            final byte[] stringBytes = string.getByteStorage(byteType);
            final byte[] tableBytes = table.getByteStorage(byteType);
            for (int i = (int) start - 1; i < stop; i++) {
                stringBytes[i] = tableBytes[stringBytes[i]];
            }
            return receiver;
        }
    }
}
