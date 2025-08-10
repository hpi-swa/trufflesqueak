/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.primitives;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.nodes.AbstractNode;
import de.hpi.swa.trufflesqueak.nodes.plugins.LargeIntegers;
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

@ImportStatic(LargeIntegers.class)
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
        return switch (this) {
            case Primitive0 primitive0 -> 0;
            case Primitive1 primitive1 -> 1;
            case Primitive2 primitive2 -> 2;
            case Primitive3 primitive3 -> 3;
            case Primitive4 primitive4 -> 4;
            case Primitive5 primitive5 -> 5;
            case Primitive6 primitive6 -> 6;
            case Primitive7 primitive7 -> 7;
            case Primitive8 primitive8 -> 8;
            case Primitive9 primitive9 -> 9;
            case Primitive10 primitive10 -> 10;
            case Primitive11 primitive11 -> 11;
            default -> throw SqueakException.create(this + " has unexpected number of arguments");
        };
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
