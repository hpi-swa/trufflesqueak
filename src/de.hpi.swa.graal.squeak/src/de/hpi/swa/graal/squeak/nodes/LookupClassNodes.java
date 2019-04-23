package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.EmptyObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodesFactory.LookupClassNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetCompiledMethodNode;

public final class LookupClassNodes {

    public abstract static class AbstractLookupClassNode extends AbstractNode {
        public abstract ClassObject executeLookup(Object receiver);
    }

    public abstract static class LookupClassNode extends AbstractLookupClassNode {
        protected final SqueakImageContext image;

        protected LookupClassNode(final SqueakImageContext image) {
            this.image = image;
        }

        public static LookupClassNode create(final SqueakImageContext image) {
            return LookupClassNodeGen.create(image);
        }

        @Specialization(guards = "value == image.sqTrue")
        protected final ClassObject doTrue(@SuppressWarnings("unused") final boolean value) {
            return image.trueClass;
        }

        @Specialization(guards = "value != image.sqTrue")
        protected final ClassObject doFalse(@SuppressWarnings("unused") final boolean value) {
            return image.falseClass;
        }

        @Specialization
        protected final ClassObject doSmallInteger(@SuppressWarnings("unused") final long value) {
            return image.smallIntegerClass;
        }

        @Specialization
        protected final ClassObject doChar(@SuppressWarnings("unused") final char value) {
            return image.characterClass;
        }

        @Specialization
        protected final ClassObject doDouble(@SuppressWarnings("unused") final double value) {
            return image.smallFloatClass;
        }

        @Specialization
        protected static final ClassObject doAbstractPointers(final AbstractPointersObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected static final ClassObject doArray(final ArrayObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected final ClassObject doClosure(@SuppressWarnings("unused") final BlockClosureObject value) {
            return image.blockClosureClass;
        }

        @Specialization
        protected final ClassObject doCharacter(@SuppressWarnings("unused") final CharacterObject value) {
            return image.characterClass;
        }

        @Specialization
        protected static final ClassObject doClass(@SuppressWarnings("unused") final ClassObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected static final ClassObject doBlock(@SuppressWarnings("unused") final CompiledBlockObject value) {
            throw SqueakException.create("Should never happen?");
        }

        @Specialization
        protected final ClassObject doMethod(@SuppressWarnings("unused") final CompiledMethodObject value) {
            return image.compiledMethodClass;
        }

        @Specialization
        protected final ClassObject doContext(@SuppressWarnings("unused") final ContextObject value) {
            return image.methodContextClass;
        }

        @Specialization
        protected static final ClassObject doEmpty(final EmptyObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected final ClassObject doFloat(@SuppressWarnings("unused") final FloatObject value) {
            return image.floatClass;
        }

        @Specialization
        protected static final ClassObject doLargeInteger(final LargeIntegerObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected static final ClassObject doNative(final NativeObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected final ClassObject doNil(@SuppressWarnings("unused") final NilObject value) {
            return image.nilClass;
        }

        @Specialization(guards = {"!isAbstractSqueakObject(value)"})
        protected final ClassObject doTruffleObject(@SuppressWarnings("unused") final TruffleObject value) {
            assert image.supportsTruffleObject();
            return image.truffleObjectClass;
        }

        @Fallback
        protected static final ClassObject doFail(final Object value) {
            throw SqueakException.create("Unexpected value: " + value);
        }
    }

    public static final class LookupSuperClassNode extends AbstractLookupClassNode {
        private final CompiledCodeObject code;
        @Child private GetCompiledMethodNode getMethodNode = GetCompiledMethodNode.create();

        protected LookupSuperClassNode(final CompiledCodeObject code) {
            this.code = code;
        }

        public static LookupSuperClassNode create(final CompiledCodeObject code) {
            return new LookupSuperClassNode(code);
        }

        @Override
        public ClassObject executeLookup(final Object receiver) {
            final ClassObject compiledInClass = getMethodNode.execute(code).getCompiledInClass();
            final Object superclass = compiledInClass.getSuperclass();
            if (superclass == code.image.nil) {
                return compiledInClass;
            } else {
                return (ClassObject) superclass;
            }
        }
    }
}
