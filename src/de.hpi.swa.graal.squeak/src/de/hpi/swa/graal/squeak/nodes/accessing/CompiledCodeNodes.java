package de.hpi.swa.graal.squeak.nodes.accessing;

import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.nodes.AbstractNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodesFactory.CalculcatePCOffsetNodeGen;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodesFactory.GetCompiledMethodNodeGen;

public final class CompiledCodeNodes {

    public abstract static class GetCompiledMethodNode extends AbstractNode {

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

    public abstract static class CalculcatePCOffsetNode extends AbstractNode {

        public static CalculcatePCOffsetNode create() {
            return CalculcatePCOffsetNodeGen.create();
        }

        public abstract int execute(CompiledCodeObject object);

        @Specialization
        protected static final int doBlock(final CompiledBlockObject object) {
            return object.getInitialPC();
        }

        @Specialization
        protected static final int doMethod(final CompiledMethodObject object) {
            return object.getInitialPC();
        }

        @Fallback
        protected static final int doFail(final CompiledCodeObject object) {
            throw new SqueakException("Should never happen", object);
        }
    }
}
