/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive0;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive10;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive11;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive2;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive3;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive5;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive6;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive7;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive8;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive9;

public abstract class AbstractPrimitiveNode extends AbstractNode {

    public final Object executeWithArguments(final VirtualFrame frame, final Object receiver, final Object... a) {
        return switch (a.length) {
            case 0 -> ((Primitive0) this).execute(frame, receiver);
            case 1 -> ((Primitive1) this).execute(frame, receiver, a[0]);
            case 2 -> ((Primitive2) this).execute(frame, receiver, a[0], a[1]);
            case 3 -> ((Primitive3) this).execute(frame, receiver, a[0], a[1], a[2]);
            case 4 -> ((Primitive4) this).execute(frame, receiver, a[0], a[1], a[2], a[3]);
            case 5 -> ((Primitive5) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4]);
            case 6 -> ((Primitive6) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4], a[5]);
            case 7 -> ((Primitive7) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4], a[5], a[6]);
            case 8 -> ((Primitive8) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7]);
            case 9 -> ((Primitive9) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8]);
            case 10 -> ((Primitive10) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9]);
            case 11 -> ((Primitive11) this).execute(frame, receiver, a[0], a[1], a[2], a[3], a[4], a[5], a[6], a[7], a[8], a[9], a[10]);
            default -> throw SqueakException.create("Invalid number of arguments", a.length);
        };
    }

    public final int getNumArgumentsSlow() {
        CompilerAsserts.neverPartOfCompilation();
        if (this instanceof Primitive0) {
            return 0;
        } else if (this instanceof Primitive1) {
            return 1;
        } else if (this instanceof Primitive2) {
            return 2;
        } else if (this instanceof Primitive3) {
            return 3;
        } else if (this instanceof Primitive4) {
            return 4;
        } else if (this instanceof Primitive5) {
            return 5;
        } else if (this instanceof Primitive6) {
            return 6;
        } else if (this instanceof Primitive7) {
            return 7;
        } else if (this instanceof Primitive8) {
            return 8;
        } else if (this instanceof Primitive9) {
            return 9;
        } else if (this instanceof Primitive10) {
            return 10;
        } else if (this instanceof Primitive11) {
            return 11;
        } else {
            throw SqueakException.create(this + " has unexpected number of arguments");
        }
    }

    public boolean acceptsMethod(@SuppressWarnings("unused") final CompiledCodeObject method) {
        CompilerAsserts.neverPartOfCompilation();
        return true;
    }

    public boolean needsFrame() {
        return false;
    }

    public abstract static class AbstractPrimitiveWithFrameNode extends AbstractPrimitiveNode {
        @Override
        public final boolean needsFrame() {
            return true;
        }
    }
}
