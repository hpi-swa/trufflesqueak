package de.hpi.swa.trufflesqueak.nodes.plugins;

import java.util.List;

import com.oracle.truffle.api.dsl.GenerateNodeFactory;
import com.oracle.truffle.api.dsl.NodeFactory;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveFactoryHolder;
import de.hpi.swa.trufflesqueak.nodes.primitives.AbstractPrimitiveNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.SqueakPrimitive;

public class MiscPrimitivePlugin extends AbstractPrimitiveFactoryHolder {

    @Override
    public List<? extends NodeFactory<? extends AbstractPrimitiveNode>> getFactories() {
        return MiscPrimitivePluginFactory.getFactories();
    }

    public static abstract class AbstractMiscPrimitiveNode extends AbstractPrimitiveNode {

        public AbstractMiscPrimitiveNode(CompiledMethodObject method) {
            super(method);
        }

        protected static boolean isASCIIOrder(NativeObject order) {
            byte[] bytes = order.getBytes();
            for (int i = 0; i < 256; i++) {
                if (bytes[i] != (byte) i) {
                    return false;
                }
            }
            return true;
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveCompareString", numArguments = 4)
    public static abstract class PrimCompareStringNode extends AbstractMiscPrimitiveNode {

        public PrimCompareStringNode(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isASCIIOrder(order)")
        protected final static long doAsciiOrder(@SuppressWarnings("unused") BaseSqueakObject receiver, NativeObject string1, NativeObject string2, @SuppressWarnings("unused") NativeObject order) {
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

        @Specialization(guards = "!isASCIIOrder(order)")
        protected final static long doCollated(@SuppressWarnings("unused") BaseSqueakObject receiver, NativeObject string1, NativeObject string2, NativeObject order) {
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
    public static abstract class PrimCompressToByteArray extends AbstractMiscPrimitiveNode {

        public PrimCompressToByteArray(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveDecompressFromByteArray", numArguments = 4) // TODO: implement primitive
    public static abstract class PrimDecompressFromByteArray extends AbstractMiscPrimitiveNode {

        public PrimDecompressFromByteArray(CompiledMethodObject method) {
            super(method);
        }

    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveFindFirstInString", numArguments = 4)
    public static abstract class PrimFindFirstInString extends AbstractMiscPrimitiveNode {

        public PrimFindFirstInString(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doFind(@SuppressWarnings("unused") BaseSqueakObject receiver, NativeObject string, NativeObject inclusionMap, long start) {
            byte[] inclusionBytes = inclusionMap.getBytes();
            if (inclusionBytes.length != 256) {
                return 0;
            }
            byte[] stringBytes = string.getBytes();
            int stringSize = stringBytes.length;
            int index = (int) start;
            for (; index <= stringSize; index++) {
                if (inclusionBytes[Byte.toUnsignedInt(stringBytes[index - 1])] != 0) {
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
    @SqueakPrimitive(name = "primitiveFindSubstring", numArguments = 5)
    public static abstract class PrimFindSubstring extends AbstractMiscPrimitiveNode {

        public PrimFindSubstring(CompiledMethodObject method) {
            super(method);
        }

        @Specialization(guards = "isASCIIOrder(matchTable)")
        protected long doFindAscii(@SuppressWarnings("unused") BaseSqueakObject receiver, NativeObject key, NativeObject body, long start, @SuppressWarnings("unused") NativeObject matchTable) {
            return body.toString().indexOf(key.toString(), (int) start - 1) + 1;
        }

        @SuppressWarnings("unused")
        @Specialization(guards = "!isASCIIOrder(matchTable)")
        protected long doFindWithMatchTable(BaseSqueakObject receiver, NativeObject key, NativeObject body, long start, NativeObject matchTable) {
            throw new PrimitiveFailed(); // TODO: implement primitive
        }
    }

    @GenerateNodeFactory
    @SqueakPrimitive(name = "primitiveIndexOfAsciiInString", numArguments = 4)
    public static abstract class PrimIndexOfAsciiInString extends AbstractMiscPrimitiveNode {

        public PrimIndexOfAsciiInString(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doNativeObject(@SuppressWarnings("unused") BaseSqueakObject receiver, long value, NativeObject string, long start) {
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
    public static abstract class PrimStringHash extends AbstractMiscPrimitiveNode {

        public PrimStringHash(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected long doNativeObject(@SuppressWarnings("unused") BaseSqueakObject receiver, NativeObject string, long initialHash) {
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
    public static abstract class PrimTranslateStringWithTable extends AbstractMiscPrimitiveNode {

        public PrimTranslateStringWithTable(CompiledMethodObject method) {
            super(method);
        }

        @Specialization
        protected NativeObject doNativeObject(@SuppressWarnings("unused") BaseSqueakObject receiver, NativeObject string, long start, long stop, NativeObject table) {
            for (int i = (int) start - 1; i < stop; i++) {
                Long tableValue = (long) table.at0(((Long) string.getNativeAt0(i)).intValue());
                string.setByte(i, tableValue.byteValue());
            }
            return string;
        }
    }
}
