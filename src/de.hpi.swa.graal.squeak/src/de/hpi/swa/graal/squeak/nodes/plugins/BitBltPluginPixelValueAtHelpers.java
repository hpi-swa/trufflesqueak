package de.hpi.swa.graal.squeak.nodes.plugins;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.profiles.BranchProfile;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveFailed;
import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginPixelValueAtHelpersFactory.PixelValueAtExecuteHelperNodeGen;
import de.hpi.swa.graal.squeak.nodes.plugins.BitBltPluginPixelValueAtHelpersFactory.PixelValueAtExtractHelperNodeGen;

public final class BitBltPluginPixelValueAtHelpers {

    private BitBltPluginPixelValueAtHelpers() {
    }

    protected abstract static class PixelValueAtExtractHelperNode extends Node {
        @Child private PixelValueAtExecuteHelperNode executeNode = PixelValueAtExecuteHelperNode.create();

        protected static PixelValueAtExtractHelperNode create() {
            return PixelValueAtExtractHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, Object bitmap);

        @Specialization(guards = "bitmap.isIntType()")
        protected final long doInts(final PointersObject receiver, final long xValue, final long yValue, final NativeObject bitmap) {
            return executeNode.executeValueAt(receiver, xValue, yValue, bitmap.getIntStorage(), receiver.at0(FORM.WIDTH), receiver.at0(FORM.HEIGHT), receiver.at0(FORM.DEPTH));
        }

        @SuppressWarnings("unused")
        @Fallback
        protected static final long doPrimitiveFail(final PointersObject receiver, final long xValue, final long yValue, final Object bitmap) {
            throw new PrimitiveFailed();
        }
    }

    protected abstract static class PixelValueAtExecuteHelperNode extends Node {
        private final BranchProfile errorProfile = BranchProfile.create();

        protected static PixelValueAtExecuteHelperNode create() {
            return PixelValueAtExecuteHelperNodeGen.create();
        }

        protected abstract long executeValueAt(PointersObject receiver, long xValue, long yValue, int[] words, Object width, Object height, Object depth);

        @SuppressWarnings("unused")
        @Specialization(guards = "xValue >= width || yValue >= height")
        protected static final long doQuickReturn(final PointersObject receiver, final long xValue, final long yValue, final int[] words, final long width, final long height,
                        final long depth) {
            return 0L;
        }

        @Specialization(guards = {"xValue < width", "yValue < height"})
        protected final long doInts(@SuppressWarnings("unused") final PointersObject receiver, final long xValue, final long yValue, final int[] words, final long width,
                        final long height, final long depth) {
            final long ppW = Math.floorDiv(32, depth);
            final long stride = Math.floorDiv(width + ppW - 1, ppW);
            if (words.length > stride * height) {
                errorProfile.enter();
                throw new PrimitiveFailed();
            }
            final int index = (int) ((yValue * stride) + Math.floorDiv(xValue, ppW));
            final int word = words[index];
            final long mask = 0xFFFFFFFFL >> (32 - depth);
            final long shift = 32 - (((xValue & (ppW - 1)) + 1) * depth);
            return ((word >> shift) & mask) & 0xffffffffL;
        }

        @Fallback
        protected static final long doFail(final PointersObject receiver, final long xValue, final long yValue,
                        final int[] words, final Object width, final Object height, final Object depth) {
            throw new SqueakException("Unsupported operation reached:", receiver, xValue, yValue, words, width, height, depth);
        }
    }
}
