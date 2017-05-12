package de.hpi.swa.trufflesqueak.model;

import java.math.BigInteger;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.exceptions.UnwrappingError;
import de.hpi.swa.trufflesqueak.util.Chunk;

public class LargeInteger extends SqueakObject {
    private BigInteger integer;

    public LargeInteger(SqueakImageContext img) {
	super(img);
    }

    public LargeInteger(SqueakImageContext img, BaseSqueakObject klass) {
	super(img, klass);
    }

    public LargeInteger(SqueakImageContext img, BigInteger i) {
	super(img);
	ClassObject liKlass = img.largePositiveIntegerClass;
	if (i.compareTo(BigInteger.ZERO) < 0) {
	    liKlass = img.largeNegativeIntegerClass;
	}
	setSqClass(liKlass);
	integer = i;
    }

    @Override
    public void fillin(Chunk chunk) {
	super.fillin(chunk);
	byte[] bytes = chunk.getBytes();
	setBytes(bytes);
    }

    @Override
    public BaseSqueakObject at0(int l) {
	return image.wrapInt(getBytes()[l]);
    }

    @Override
    public void atput0(int idx, BaseSqueakObject object) throws UnwrappingError {
	byte b = (byte) object.unwrapInt();
	byte[] bytes = getBytes();
	bytes[idx] = b;
	setBytes(bytes);
    }

    private void setBytes(byte[] bytes) {
	integer = new BigInteger(bytes);
	if (isNegative()) {
	    integer = integer.negate();
	}
    }

    public byte[] getBytes() {
	byte[] bytes;
	if (isNegative()) {
	    bytes = integer.negate().toByteArray();
	} else {
	    bytes = integer.toByteArray();
	}
	return bytes;
    }

    private boolean isNegative() {
	return getSqClass() == image.largeNegativeIntegerClass;
    }

    @Override
    public int size() {
	return getBytes().length;
    }

    @Override
    public int instsize() {
	return 0;
    }

    public BigInteger getValue() {
	return integer;
    }
}
