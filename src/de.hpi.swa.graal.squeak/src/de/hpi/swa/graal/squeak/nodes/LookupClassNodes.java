package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.CachedContext;
import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.CharacterObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
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

    @GenerateUncached
    public abstract static class LookupClassNode extends AbstractLookupClassNode {
        public static LookupClassNode create() {
            return LookupClassNodeGen.create();
        }

        public static LookupClassNode getUncached() {
            return LookupClassNodeGen.getUncached();
        }

        @Specialization
        protected static final ClassObject doNil(@SuppressWarnings("unused") final NilObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.nilClass;
        }

        @Specialization(guards = "value == image.sqTrue")
        protected static final ClassObject doTrue(@SuppressWarnings("unused") final boolean value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.trueClass;
        }

        @Specialization(guards = "value != image.sqTrue")
        protected static final ClassObject doFalse(@SuppressWarnings("unused") final boolean value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.falseClass;
        }

        @Specialization
        protected static final ClassObject doSmallInteger(@SuppressWarnings("unused") final long value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.smallIntegerClass;
        }

        @Specialization
        protected static final ClassObject doChar(@SuppressWarnings("unused") final char value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.characterClass;
        }

        @Specialization
        protected static final ClassObject doDouble(@SuppressWarnings("unused") final double value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
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
        protected static final ClassObject doClosure(@SuppressWarnings("unused") final BlockClosureObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.blockClosureClass;
        }

        @Specialization
        protected static final ClassObject doCharacter(@SuppressWarnings("unused") final CharacterObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.characterClass;
        }

        @Specialization
        protected static final ClassObject doClass(@SuppressWarnings("unused") final ClassObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected static final ClassObject doMethod(@SuppressWarnings("unused") final CompiledMethodObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.compiledMethodClass;
        }

        @Specialization
        protected static final ClassObject doContext(@SuppressWarnings("unused") final ContextObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            return image.methodContextClass;
        }

        @Specialization
        protected static final ClassObject doEmpty(final EmptyObject value) {
            return value.getSqueakClass();
        }

        @Specialization
        protected static final ClassObject doFloat(@SuppressWarnings("unused") final FloatObject value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
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

        @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
        protected static final ClassObject doTruffleObject(@SuppressWarnings("unused") final Object value,
                        @CachedContext(SqueakLanguage.class) final SqueakImageContext image) {
            assert image.supportsTruffleObject();
            return image.truffleObjectClass;
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
            final ClassObject methodClass = getMethodNode.execute(code).getMethodClass();
            final ClassObject superclass = methodClass.getSuperclassOrNull();
            return superclass == null ? methodClass : superclass;
        }
    }
}
