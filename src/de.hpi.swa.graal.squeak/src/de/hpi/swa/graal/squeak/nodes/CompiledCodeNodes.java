package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodesFactory.CalculcatePCOffsetNodeGen;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodesFactory.GetCompiledMethodNodeGen;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodesFactory.GetNumAllArgumentsNodeGen;
import de.hpi.swa.graal.squeak.nodes.CompiledCodeNodesFactory.IsDoesNotUnderstandNodeGen;

public final class CompiledCodeNodes {

    public abstract static class GetCompiledMethodNode extends Node {

        public static GetCompiledMethodNode create() {
            return GetCompiledMethodNodeGen.create();
        }

        public abstract CompiledMethodObject execute(CompiledCodeObject obj);

        @Specialization
        protected static final CompiledMethodObject doBlock(final CompiledBlockObject obj) {
            return obj.getMethod();
        }

        @Specialization
        protected static final CompiledMethodObject doMethod(final CompiledMethodObject obj) {
            return obj;
        }

    }

    public abstract static class GetNumAllArgumentsNode extends Node {

        public static GetNumAllArgumentsNode create() {
            return GetNumAllArgumentsNodeGen.create();
        }

        public abstract int execute(CompiledCodeObject obj);

        @Specialization
        protected static final int doBlock(final CompiledBlockObject obj) {
            return obj.getNumArgs() + obj.getNumCopiedValues();
        }

        @Specialization
        protected static final int doMethod(final CompiledMethodObject obj) {
            return obj.getNumArgs();
        }

    }

    public abstract static class IsDoesNotUnderstandNode extends AbstractNodeWithImage {
        public static IsDoesNotUnderstandNode create(final SqueakImageContext image) {
            return IsDoesNotUnderstandNodeGen.create(image);
        }

        public IsDoesNotUnderstandNode(final SqueakImageContext image) {
            super(image);
        }

        public abstract boolean execute(Object obj);

        @Specialization
        protected static final boolean doBlock(@SuppressWarnings("unused") final CompiledBlockObject obj) {
            return false; // block cannot be doesNotUnderstand
        }

        @Specialization
        protected final boolean doMethod(final CompiledMethodObject obj) {
            return obj.getCompiledInSelector() == image.doesNotUnderstand;
        }

        @Fallback
        protected static final boolean doFallback(@SuppressWarnings("unused") final Object obj) {
            return false;
        }

    }

    public abstract static class CalculcatePCOffsetNode extends Node {

        public static CalculcatePCOffsetNode create() {
            return CalculcatePCOffsetNodeGen.create();
        }

        public abstract int execute(CompiledCodeObject obj);

        @Specialization
        protected static final int doBlock(final CompiledBlockObject obj) {
            return obj.getInitialPC();
        }

        @Specialization
        protected static final int doMethod(final CompiledMethodObject obj) {
            return obj.getInitialPC();
        }

    }
}
