package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNodeWithImage;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodesFactory.CalculcatePCOffsetNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodesFactory.GetCompiledMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodesFactory.GetNumAllArgumentsNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodesFactory.IsDoesNotUnderstandNodeGen;

public final class CompiledCodeNodes {

    public abstract static class GetCompiledMethodNode extends Node {

        public static GetCompiledMethodNode create() {
            return GetCompiledMethodNodeGen.create();
        }

        public abstract CompiledMethodObject execute(CompiledCodeObject object);

        @Specialization
        protected static final CompiledMethodObject doBlock(final CompiledBlockObject object) {
            return object.getMethod();
        }

        @Specialization
        protected static final CompiledMethodObject doMethod(final CompiledMethodObject object) {
            return object;
        }

        @Fallback
        protected static final CompiledMethodObject doFail(final CompiledCodeObject object) {
            throw new SqueakException("Unexpected value: ", object);
        }

    }

    public abstract static class GetNumAllArgumentsNode extends Node {

        public static GetNumAllArgumentsNode create() {
            return GetNumAllArgumentsNodeGen.create();
        }

        public abstract int execute(CompiledCodeObject object);

        @Specialization
        protected static final int doBlock(final CompiledBlockObject object) {
            return object.getNumArgs() + object.getNumCopiedValues();
        }

        @Specialization
        protected static final int doMethod(final CompiledMethodObject object) {
            return object.getNumArgs();
        }

        @Fallback
        protected static final int doFail(final CompiledCodeObject object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }

    public abstract static class IsDoesNotUnderstandNode extends AbstractNodeWithImage {
        public static IsDoesNotUnderstandNode create(final SqueakImageContext image) {
            return IsDoesNotUnderstandNodeGen.create(image);
        }

        public IsDoesNotUnderstandNode(final SqueakImageContext image) {
            super(image);
        }

        public abstract boolean execute(Object object);

        @Specialization(guards = "object.getNumLiterals() >= 2")
        protected final boolean doMethod(final CompiledMethodObject object) {
            final Object[] literals = object.getLiterals();
            return literals[literals.length - 2] == image.doesNotUnderstand;
        }

        @Fallback
        protected static final boolean doFallback(@SuppressWarnings("unused") final Object object) {
            return false;
        }
    }

    public abstract static class CalculcatePCOffsetNode extends Node {

        public static CalculcatePCOffsetNode create() {
            return CalculcatePCOffsetNodeGen.create();
        }

        public abstract int execute(Object object);

        @Specialization
        protected static final int doBlock(final CompiledBlockObject object) {
            return object.getInitialPC();
        }

        @Specialization
        protected static final int doMethod(final CompiledMethodObject object) {
            return object.getInitialPC();
        }

        @Fallback
        protected static final int doFail(final Object object) {
            throw new SqueakException("Unexpected value:", object);
        }
    }
}
