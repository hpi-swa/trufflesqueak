package de.hpi.swa.graal.squeak.nodes.plugins;

import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.BIT_BLT;
import de.hpi.swa.graal.squeak.model.ObjectLayouts.FORM;
import de.hpi.swa.graal.squeak.model.PointersObject;

public final class BitBltPluginHelpers {
    // private static final long ALL_ONES = 4294967295L;
    private static final int[] MASK_TABLE = new int[]{0, 1, 3, 0, 15, 31, 0, 0, 255, 0, 0, 0, 0, 0, 0, 0, 65535, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, -1};

    public static final class BitBltWrapper {
        private final PointersObject bitBlt;
        private final PointersObject sourceForm;
        private final PointersObject destForm;
        private final PointersObject halftoneForm;

        public BitBltWrapper(final PointersObject bitBlt) {
            this.bitBlt = bitBlt;
            this.sourceForm = bitBlt.at0(BIT_BLT.SOURCE_FORM) == bitBlt.image.nil ? null : (PointersObject) bitBlt.at0(BIT_BLT.SOURCE_FORM);
            this.destForm = bitBlt.at0(BIT_BLT.DEST_FORM) == bitBlt.image.nil ? null : (PointersObject) bitBlt.at0(BIT_BLT.DEST_FORM);
            this.halftoneForm = bitBlt.at0(BIT_BLT.HALFTONE_FORM) == bitBlt.image.nil ? null : (PointersObject) bitBlt.at0(BIT_BLT.HALFTONE_FORM);
        }

        public PointersObject getBitBlt() {
            return bitBlt;
        }

        protected boolean clipRange() {
            // "clip and adjust source origin and extent appropriately"
            if (getDestXRaw() == bitBlt.image.nil) {
                setDestX(0L);
            }
            if (getDestXRaw() == bitBlt.image.nil) {
                bitBlt.atput0(BIT_BLT.DEST_Y, 0);
                setDestY(0L);
            }
            if (getWidthRaw() == bitBlt.image.nil) {
                setWidth((long) destForm.at0(FORM.WIDTH));
            }
            if (getHeightRaw() == bitBlt.image.nil) {
                setHeight((long) destForm.at0(FORM.HEIGHT));
            }
            if (getSourceXRaw() == bitBlt.image.nil) {
                bitBlt.atput0(BIT_BLT.SOURCE_X, 0);
                setSourceX(0L);
            }
            if (getSourceYRaw() == bitBlt.image.nil) {
                setSourceY(0L);
            }
            if (getClipXRaw() == bitBlt.image.nil) {
                setClipX(0L);
            }
            if (getClipYRaw() == bitBlt.image.nil) {
                setClipY(0L);
            }
            if (getClipWidthRaw() == bitBlt.image.nil) {
                setClipWidth((long) destForm.at0(FORM.WIDTH));
            }
            if (getClipHeightRaw() == bitBlt.image.nil) {
                setClipHeight((long) destForm.at0(FORM.HEIGHT));
            }

            final long destX = getDestXLong();
            final long destY = getDestYLong();
            final long width = getWidthLong();
            final long height = getHeightLong();
            final long sourceX = getSourceXLong();
            final long sourceY = getSourceYLong();
            final long clipX = getClipXLong();
            final long clipY = getClipYLong();
            final long clipWidth = getClipWidthLong();
            final long clipHeight = getClipHeightLong();

            long sx;
            long dx;
            long bbW;
            long sy;
            long dy;
            long bbH;

            if (destX >= clipX) {
                sx = sourceX;
                dx = destX;
                bbW = width;
            } else {
                sx = sourceX + (clipX - destX);
                bbW = width - (clipX - destX);
                dx = clipX;
            }
            if ((dx + bbW) > (clipX + clipWidth)) {
                bbW = bbW - ((dx + bbW) - (clipX + clipWidth));
            }
            if (destY >= clipY) {
                sy = sourceY;
                dy = destY;
                bbH = height;
            } else {
                sy = sourceY + clipY - destY;
                bbH = height - (clipY - destY);
                dy = clipY;
            }
            if ((dy + bbH) > (clipY + clipHeight)) {
                bbH = bbH - ((dy + bbH) - (clipY + clipHeight));
            }
            if (sourceForm != null) {
                if (sx < 0) {
                    dx = dx - sx;
                    bbW = bbW + sx;
                    sx = 0;
                }
                final long sourceWidth = (long) sourceForm.at0(FORM.WIDTH);
                if (sx + bbW > sourceWidth) {
                    bbW = bbW - (sx + bbW - sourceWidth);
                }

                if (sy < 0) {
                    dy = dy - sy;
                    bbH = bbH + sy;
                    sy = 0;
                }
                final long sourceHeight = (long) sourceForm.at0(FORM.HEIGHT);
                if (sy + bbH > sourceHeight) {
                    bbH = bbH - (sy + bbH - sourceHeight);
                }
            }
            if (bbW <= 0 || bbH <= 0) {
                setSourceX(0);
                setSourceY(0);
                setDestX(0);
                setDestY(0);
                setClipX(0);
                setClipY(0);
                setWidth(0);
                setHeight(0);
                setClipWidth(0);
                setClipHeight(0);
                return true;
            }
            if (sx == sourceX && sy == sourceY && dx == destX && dy == destY && bbW == width && bbH == height) {
                return false;
            }
            setSourceX(sx);
            setSourceY(sy);
            setDestX(dx);
            setDestY(dy);
            setHeight(bbH);
            return true;
        }

        public long setDestX(final long value) {
            bitBlt.atput0(BIT_BLT.DEST_X, value);
            return value;
        }

        public long getDestXLong() {
            return (long) getDestXRaw();
        }

        private Object getDestXRaw() {
            return bitBlt.at0(BIT_BLT.DEST_X);
        }

        public long setDestY(final long value) {
            bitBlt.atput0(BIT_BLT.DEST_Y, value);
            return value;
        }

        public long getDestYLong() {
            return (long) getDestYRaw();
        }

        private Object getDestYRaw() {
            return bitBlt.at0(BIT_BLT.DEST_Y);
        }

        public long setWidth(final long value) {
            bitBlt.atput0(BIT_BLT.WIDTH, value);
            return value;
        }

        public long getWidthLong() {
            return (long) getWidthRaw();
        }

        private Object getWidthRaw() {
            return bitBlt.at0(BIT_BLT.WIDTH);
        }

        public long setHeight(final long value) {
            bitBlt.atput0(BIT_BLT.HEIGHT, value);
            return value;
        }

        public long getHeightLong() {
            return (long) getHeightRaw();
        }

        private Object getHeightRaw() {
            return bitBlt.at0(BIT_BLT.HEIGHT);
        }

        public long setSourceX(final long value) {
            bitBlt.atput0(BIT_BLT.SOURCE_X, value);
            return value;
        }

        public long getSourceXLong() {
            return (long) getSourceXRaw();
        }

        private Object getSourceXRaw() {
            return bitBlt.at0(BIT_BLT.SOURCE_X);
        }

        public long setSourceY(final long value) {
            bitBlt.atput0(BIT_BLT.SOURCE_Y, value);
            return value;
        }

        public long getSourceYLong() {
            return (long) getSourceYRaw();
        }

        private Object getSourceYRaw() {
            return bitBlt.at0(BIT_BLT.SOURCE_Y);
        }

        public long setClipX(final long value) {
            bitBlt.atput0(BIT_BLT.CLIP_X, value);
            return value;
        }

        public long getClipXLong() {
            return (long) getClipXRaw();
        }

        private Object getClipXRaw() {
            return bitBlt.at0(BIT_BLT.CLIP_X);
        }

        public long setClipY(final long value) {
            bitBlt.atput0(BIT_BLT.CLIP_Y, value);
            return value;
        }

        public long getClipYLong() {
            return (long) getClipYRaw();
        }

        private Object getClipYRaw() {
            return bitBlt.at0(BIT_BLT.CLIP_Y);
        }

        public long setClipWidth(final long value) {
            bitBlt.atput0(BIT_BLT.CLIP_WIDTH, value);
            return value;
        }

        public long getClipWidthLong() {
            return (long) getClipWidthRaw();
        }

        private Object getClipWidthRaw() {
            return bitBlt.at0(BIT_BLT.CLIP_WIDTH);
        }

        public long setClipHeight(final long value) {
            bitBlt.atput0(BIT_BLT.CLIP_HEIGHT, value);
            return value;
        }

        public long getClipHeightLong() {
            return (long) getClipHeightRaw();
        }

        private Object getClipHeightRaw() {
            return bitBlt.at0(BIT_BLT.CLIP_HEIGHT);
        }

        public long getSourceWidth() {
            return (long) sourceForm.at0(FORM.WIDTH);
        }

        public long getSourceDepth() {
            return (long) sourceForm.at0(FORM.DEPTH);
        }

        public long getDestDepth() {
            return (long) destForm.at0(FORM.DEPTH);
        }

        public int[] getDestWords() {
            return ((NativeObject) destForm.at0(FORM.DEPTH)).getIntStorage();
        }

        public int[] getSourceWords() {
            return ((NativeObject) sourceForm.at0(FORM.DEPTH)).getIntStorage();
        }

        // /* Compute masks for left and right destination words */
        // public void destMaskAndPointerInit() {
        // /* A mask, assuming power of two */
        // /* how many pixels in first word */
        //
        // final long destDepth = getDestDepth();
        // final long pixPerM1 = destPPW - 1;
        // final long startBits = destPPW - (dx & pixPerM1);
        // if (destMSB) {
        // mask1 = ALL_ONES >> (32 - (startBits * destDepth));
        // } else {
        // mask1 = ALL_ONES << (32 - (startBits * destDepth));
        // }
        // long endBits = (((dx + bbW) - 1) & pixPerM1) + 1;
        // if (destMSB) {
        // mask2 = SHL(ALL_ONES << (32 - (endBits * destDepth)));
        // } else {
        // mask2 = SHR(ALL_ONES >> (32 - (endBits * destDepth)));
        // }
        // if (bbW < startBits) {
        // mask1 = mask1 & mask2;
        // mask2 = 0;
        // nWords = 1;
        // } else {
        // nWords = (Math.floorDiv(((bbW - startBits) + pixPerM1), destPPW)) + 1;
        // }
        //
        // /* defaults for no overlap with source */
        // /* calculate byte addr and delta, based on first word of data */
        // /* Note pitch is bytes and nWords is longs, not bytes */
        //
        // hDir = (vDir = 1);
        // destIndex = ((dy * destPitch)) + ((DIV(dx, destPPW)) * 4);
        // destDelta = (destPitch * vDir) - (4 * (nWords * hDir));
        // }
        //
        // public void copyLoopPixMap(LongBinaryOperator mergeFnwith) {
        //
        // final long sourceDepth = getSourceDepth();
        // final long destDepth = getDestDepth();
        // final int sourcePPW = (int) Math.floorDiv(32, sourceDepth);
        // final int destPPW = (int) Math.floorDiv(32, destDepth);
        // final int sourcePixMask = MASK_TABLE[(int) sourceDepth];
        // final int destPixMask = MASK_TABLE[(int) getDestDepth()];
        //// mapperFlags = cmFlags & ~ColorMapNewStyle;
        // final long sourcePitch = getSourceWidth() + Math.floorDiv(sourcePPW - 1, sourcePPW * 4);
        // final long sx = getSourceXLong();
        // final long dx = getDestXLong();
        // final long dy = getDestYLong();
        // final long bbW = getWidthLong();
        // final long bbH = getHeightLong();
        // final long sourceIndex = ((getSourceYLong() * sourcePitch)) +
        // ((Math.floorDiv(getSourceXLong(), sourcePPW)) * 4);
        // long scrStartBits = sourcePPW - (sx & (sourcePPW - 1));
        // final long nSourceIncs;
        // if (bbW < scrStartBits) {
        // nSourceIncs = 0;
        // } else {
        // nSourceIncs = (Math.floorDiv((bbW - scrStartBits), sourcePPW)) + 1;
        // }
        //
        // /* Note following two items were already calculated in destmask setup! */
        //
        // long sourceDelta = sourcePitch - (nSourceIncs * 4);
        // long startBits = destPPW - (dx & (destPPW - 1));
        // long endBits = (((dx + bbW) - 1) & (destPPW - 1)) + 1;
        // if (bbW < startBits) {
        // startBits = bbW;
        // }
        // long srcShift = (sx & (sourcePPW - 1)) * sourceDepth;
        // long dstShift = (dx & (destPPW - 1)) * destDepth;
        // long srcShiftInc = sourceDepth;
        // long dstShiftInc = destDepth;
        // int dstShiftLeft = 0;
        //
        // final int[] destBits = getDestWords();
        //// if (sourceMSB) {
        //// srcShift = (32 - sourceDepth) - srcShift;
        //// srcShiftInc = 0 - srcShiftInc;
        //// }
        //// if (destMSB) {
        //// dstShift = (32 - destDepth) - dstShift;
        //// dstShiftInc = 0 - dstShiftInc;
        //// dstShiftLeft = 32 - destDepth;
        //// }
        // for (int i = 1; i <= bbH; i++) {
        // /* here is the vertical loop */
        // /* *** is it possible at all that noHalftone == false? *** */
        // final long halftoneWord;
        // if (noHalftone) {
        // halftoneWord = ALL_ONES;
        // } else {
        // halftoneWord = halftoneAt((dy + i) - 1);
        // }
        // long srcBitShift = srcShift;
        // long dstBitShift = dstShift;
        // long destMask = mask1;
        // /* Here is the horizontal loop... */
        // long nPix = startBits;
        // long words = nWords;
        // do {
        // /* pick up the word */
        // /* align next word to leftmost pixel */
        // skewWord = pickSourcePixelsflagssrcMaskdestMasksrcShiftIncdstShiftInc(nPix, mapperFlags,
        // sourcePixMask, destPixMask, srcShiftInc, dstShiftInc);
        // dstBitShift = dstShiftLeft;
        // if (destMask == ALL_ONES) {
        // /* avoid read-modify-write */
        // final long mergeWord = mergeFnwith.applyAsLong(skewWord & halftoneWord,
        // destBits[destIndex >>> 2]);
        // destBits[destIndex >>> 2] = destMask & mergeWord;
        // } else {
        // /* General version using dest masking */
        // destWord = destBits[destIndex >>> 2];
        // final long mergeWord = mergeFnwith.applyAsLong(skewWord & halftoneWord, destWord &
        // destMask);
        // destWord = (destMask & mergeWord) | (destWord & ~destMask);
        // destBits[destIndex >>> 2] = destWord;
        // }
        // destIndex += 4;
        // if (words == 2) {
        // /* e.g., is the next word the last word? */
        // /* set mask for last word in this row */
        // destMask = mask2;
        // nPix = endBits;
        // } else {
        // /* use fullword mask for inner loop */
        // destMask = ALL_ONES;
        // nPix = destPPW;
        // }
        // } while (!(((--words)) == 0));
        // sourceIndex += sourceDelta;
        // destIndex += destDelta;
        // }
        // }
        //
        protected long halftoneAt(final long index) {
            final long halftoneHeight = (long) halftoneForm.at0(FORM.HEIGHT);
            final long offset = (index - Math.floorDiv(index, halftoneHeight) * halftoneHeight);
            return ((NativeObject) halftoneForm.at0(FORM.BITS)).getIntStorage()[(int) offset];
        }
    }

    public static long alphaBlend24(final long sourceWord, final long destinationWord) {
        final long alpha = sourceWord >> 24;
        if (alpha == 0) {
            return destinationWord;
        }
        if (alpha == 255) {
            return sourceWord;
        }

        final long unAlpha = 255 - alpha;

        // blend red and blue
        long blendRB = ((sourceWord & 0xFF00FF) * alpha) +
                        ((destinationWord & 0xFF00FF) * unAlpha) + 0xFF00FF;

        // blend alpha and green
        long blendAG = ((((sourceWord >> 8) | 0xFF0000) & 0xFF00FF) * alpha) +
                        (((destinationWord >> 8) & 0xFF00FF) * unAlpha) + 0xFF00FF;

        blendRB = (blendRB + (((blendRB - 0x10001) >> 8) & 0xFF00FF) >> 8) & 0xFF00FF;
        blendAG = (blendAG + (((blendAG - 0x10001) >> 8) & 0xFF00FF) >> 8) & 0xFF00FF;
        return blendRB | (blendAG << 8);
    }

    public static long pixPaint25(final long sourceWord, final long destinationWord) {
        return (sourceWord | ((~sourceWord & 0xffffffff) & destinationWord));
    }

    public static long pixMask26(final long sourceWord, final long destinationWord) {
        return ((~sourceWord & 0xffffffff) & destinationWord);
    }

    public static long rgbAdd20(final long sourceWord, final long destinationWord) {
        /* Add RGBA components of the pixel separately */
        return partitionedAdd(sourceWord, destinationWord, 8, 255, 2155905152L);
    }

    public static long rgbMul37(final long sourceWord, final long destinationWord) {
        /* Mul RGBA components of the pixel separately */
        return partitionedMul(sourceWord, destinationWord, 8, 4);
    }

    public static long partitionedMul(final long word1, final long word2, final int nBits, final int nParts) {
        /* partition mask starts at the right */
        final int sMask = MASK_TABLE[nBits];
        final int dMask = sMask << nBits;
        /* optimized first step */
        long result = (((((word1 & sMask) + 1) * ((word2 & sMask) + 1)) - 1) & dMask) >> nBits;
        if (nParts == 1) {
            return result;
        }
        long product = (((((word1 >> nBits) & sMask) + 1) * ((((word2 >> nBits)) & sMask) + 1)) - 1) & dMask;
        result = result | product;
        if (nParts == 2) {
            return result;
        }
        product = ((((((word1 >> (2 * nBits))) & sMask) + 1) * ((((word2 >> (2 * nBits))) & sMask) + 1)) - 1) & dMask;
        result = result | (product << nBits);
        if (nParts == 3) {
            return result;
        }
        product = ((((((word1 >> (3 * nBits))) & sMask) + 1) * ((((word2 >> (3 * nBits))) & sMask) + 1)) - 1) & dMask;
        result = result | (product << (2 * nBits));
        return result;
    }

    public static long partitionedAdd(final long word1, final long word2, final long nBits, final long componentMask, final long carryOverflowMask) {
        /* mask to remove high bit of each component */
        final long w1 = word1 & carryOverflowMask;
        final long w2 = word2 & carryOverflowMask;
        /* sum without high bit to avoid overflowing over next component */
        final long sum = (word1 ^ w1) + (word2 ^ w2);
        /* detect overflow condition for saturating */
        final long carryOverflow = (w1 & w2) | ((w1 | w2) & sum);
        return ((sum ^ w1) ^ w2) | (((carryOverflow >> (nBits - 1))) * componentMask);
    }

}
