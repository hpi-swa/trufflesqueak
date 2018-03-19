package de.hpi.swa.trufflesqueak.nodes.context;

import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNodeWithImage;

public abstract class SqueakLookupClassNode extends AbstractNodeWithImage {
    public static SqueakLookupClassNode create(SqueakImageContext image) {
        return SqueakLookupClassNodeGen.create(image);
    }

    protected SqueakLookupClassNode(SqueakImageContext image) {
        super(image);
    }

    public abstract ClassObject executeLookup(Object receiver);

    @Specialization
    protected ClassObject squeakClass(boolean object) {
        if (object) {
            return image.trueClass;
        } else {
            return image.falseClass;
        }
    }

    protected boolean isNil(Object object) {
        return object == image.nil;
    }

    @Specialization
    protected ClassObject squeakClass(long object) {
        if (object < LargeIntegerObject.SMALLINTEGER32_MIN) {
            return image.largeNegativeIntegerClass;
        } else if (object <= LargeIntegerObject.SMALLINTEGER32_MAX) {
            return image.smallIntegerClass;
        } else {
            return image.largePositiveIntegerClass;
        }
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject squeakClass(char object) {
        return image.characterClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject squeakClass(double object) {
        return image.floatClass;
    }

    @Specialization
    protected ClassObject squeakClass(@SuppressWarnings("unused") BlockClosureObject ch) {
        return image.blockClosureClass;
    }

    @Specialization
    protected ClassObject squeakClass(@SuppressWarnings("unused") ContextObject ch) {
        return image.methodContextClass;
    }

    @SuppressWarnings("unused")
    @Specialization
    protected ClassObject nilClass(NilObject object) {
        return image.nilClass;
    }

    @Specialization
    protected ClassObject squeakClass(BaseSqueakObject object) {
        return object.getSqClass();
    }
}
