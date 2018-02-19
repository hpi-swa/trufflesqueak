package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BytesObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.WordsObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class MiscPrimitivePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscPrimitivePluginFactory.getFactories();
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCompareString", numArguments = 4)
    public static abstract class PrimCompareStringNode extends AbstractPrimitiveNode {

        public PrimCompareStringNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doBytesObject(@SuppressWarnings("unused") ClassObject receiver, BytesObject string1, BytesObject string2, BytesObject order) {
            if (isASCIIOrder(order)) {
                return compareASCII(string1, string2);
            } else {
                return compareCollated(string1, string2, order);
            }
        }

        private static boolean isASCIIOrder(BytesObject order) {
            byte[] bytes = order.getBytes();
            for (int i = 0; i < 256; i++) {
                if (bytes[i] != (byte) i) {
                    return false;
                }
            }
            return true;
        }

        private static long compareASCII(BytesObject string1, BytesObject string2) {
            byte[] bytes1 = string1.getBytes();
            byte[] bytes2 = string2.getBytes();
            int length1 = bytes1.length;
            int length2 = bytes2.length;
            int minLength = Math.min(bytes1.length, length2);
            for (int i = 0; i < minLength; i++) {
                byte c1 = bytes1[i];
                byte c2 = bytes2[i];
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

        private static long compareCollated(BytesObject string1, BytesObject string2, BytesObject order) {
            byte[] bytes1 = string1.getBytes();
            byte[] bytes2 = string2.getBytes();
            byte[] orderBytes = order.getBytes();
            int length1 = bytes1.length;
            int length2 = bytes2.length;
            int minLength = Math.min(length1, length2);
            for (int i = 0; i < minLength; i++) {
                byte c1 = orderBytes[bytes1[i]];
                byte c2 = orderBytes[bytes2[i]];
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
    @SqueakPrimitive(name = "primitiveCompressToByteArray", numArguments = 3) // TODO: implement primitive
    public static abstract class PrimCompressToByteArray extends AbstractPrimitiveNode {

        public PrimCompressToByteArray(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDecompressFromByteArray", numArguments = 4) // TODO: implement primitive
    public static abstract class PrimDecompressFromByteArray extends AbstractPrimitiveNode {

        public PrimDecompressFromByteArray(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFindFirstInString", numArguments = 4) // TODO: implement primitive
    public static abstract class PrimFindFirstInString extends AbstractPrimitiveNode {

        public PrimFindFirstInString(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFindSubstring", numArguments = 5) // TODO: implement primitive
    public static abstract class PrimFindSubstring extends AbstractPrimitiveNode {

        public PrimFindSubstring(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveIndexOfAsciiInString", numArguments = 4)
    public static abstract class PrimIndexOfAsciiInString extends AbstractPrimitiveNode {

        public PrimIndexOfAsciiInString(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doBytesObject(@SuppressWarnings("unused") ClassObject receiver, long value, BytesObject string, long start) {
            if (start < 0) {
                throw new PrimitiveFailed();
            }
            byte[] bytes = string.getBytes();
            for (int i = (int) (start - 1); i < bytes.length; i++) {
                if (bytes[i] == value) {
                    return i + 1;
                }
            }
            return 0;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveStringHash", numArguments = 3)
    public static abstract class PrimStringHash extends AbstractPrimitiveNode {

        public PrimStringHash(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doBytesObject(@SuppressWarnings("unused") ClassObject receiver, BytesObject string, long initialHash) {
            long hash = initialHash & 0xfffffff;
            long low;
            for (byte value : string.getBytes()) {
                hash += value;
                low = hash & 16383;
                hash = (0x260D * low + (((0x260d * (hash >> 14) + (0x0065 * low)) & 16383) * 16384)) & 0x0fffffff;
            }
            return hash;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveTranslateStringWithTable", numArguments = 5)
    public static abstract class PrimTranslateStringWithTable extends AbstractPrimitiveNode {

        public PrimTranslateStringWithTable(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected BytesObject doBytesObject(@SuppressWarnings("unused") ClassObject receiver, BytesObject string, long start, long stop, BytesObject table) {
            byte[] stringBytes = string.getBytes();
            byte[] tableBytes = table.getBytes();
            for (int i = (int) start - 1; i < stop; i++) {
                string.setByte(i, tableBytes[stringBytes[i]]);
            }
            return string;
        }

        @Specialization
        protected WordsObject doWordsObject(@SuppressWarnings("unused") ClassObject receiver, WordsObject string, long start, long stop, WordsObject table) {
            int[] stringBytes = string.getWords();
            int[] tableBytes = table.getWords();
            for (int i = (int) start - 1; i < stop; i++) {
                string.setInt(i, tableBytes[stringBytes[i]]);
            }
            return string;
        }

        @Specialization
        protected BytesObject doWordsObject(@SuppressWarnings("unused") ClassObject receiver, BytesObject string, long start, long stop, WordsObject table) {
            byte[] stringBytes = string.getBytes();
            int[] tableBytes = table.getWords();
            for (int i = (int) start - 1; i < stop; i++) {
                string.setByte(i, (byte) tableBytes[stringBytes[i]]);
            }
            return string;
        }
    }
}
