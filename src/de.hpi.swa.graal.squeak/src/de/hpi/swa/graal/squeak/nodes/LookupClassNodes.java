package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.GenerateUncached;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.nodes.LookupClassNodesFactory.LookupClassNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetCompiledMethodNode;

public final class LookupClassNodes {

    public abstract static class AbstractLookupClassNode extends AbstractNode {
        public abstract ClassObject executeLookup(SqueakImageContext image, Object receiver);
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
        protected static final ClassObject doNil(final SqueakImageContext image, @SuppressWarnings("unused") final NilObject value) {
            return image.nilClass;
        }

        @Specialization(guards = "value == TRUE")
        protected static final ClassObject doTrue(final SqueakImageContext image, @SuppressWarnings("unused") final boolean value) {
            return image.trueClass;
        }

        @Specialization(guards = "value != TRUE")
        protected static final ClassObject doFalse(final SqueakImageContext image, @SuppressWarnings("unused") final boolean value) {
            return image.falseClass;
        }

        @Specialization
        protected static final ClassObject doSmallInteger(final SqueakImageContext image, @SuppressWarnings("unused") final long value) {
            return image.smallIntegerClass;
        }

        @Specialization
        protected static final ClassObject doChar(final SqueakImageContext image, @SuppressWarnings("unused") final char value) {
            return image.characterClass;
        }

        @Specialization
        protected static final ClassObject doDouble(final SqueakImageContext image, @SuppressWarnings("unused") final double value) {
            return image.smallFloatClass;
        }

        @Specialization
        protected static final ClassObject doAbstractSqueakObjectWithClassAndHash(@SuppressWarnings("unused") final SqueakImageContext image, final AbstractSqueakObjectWithClassAndHash value) {
            return value.getSqueakClass();
        }

        @Specialization(guards = {"!isAbstractSqueakObject(value)", "!isUsedJavaPrimitive(value)"})
        protected static final ClassObject doTruffleObject(final SqueakImageContext image, @SuppressWarnings("unused") final Object value) {
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
        public ClassObject executeLookup(final SqueakImageContext image, final Object receiver) {
            final ClassObject methodClass = getMethodNode.execute(code).getMethodClass();
            final ClassObject superclass = methodClass.getSuperclassOrNull();
            return superclass == null ? methodClass : superclass;
        }
    }
}
