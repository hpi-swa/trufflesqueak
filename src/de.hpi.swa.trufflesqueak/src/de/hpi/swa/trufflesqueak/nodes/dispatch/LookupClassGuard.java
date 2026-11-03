/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.NeverDefault;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;

public abstract class LookupClassGuard {
    public abstract boolean check(Object receiver);

    public final ClassObject getSqueakClass(final Node node) {
        CompilerAsserts.partialEvaluationConstant(node);
        return getSqueakClassInternal(node);
    }

    protected abstract ClassObject getSqueakClassInternal(Node node);

    /**
     * Creates a specialized guard for the given receiver.
     * <p>
     * Note: The order of the if-else chain is strictly optimized for interpreter
     * warmup performance using Smith's Rule (Probability / Cost). Because these
     * checks evaluate sequentially during guard creation, we rank them by their
     * expected CPU yield rather than pure statistical frequency.
     * <p>
     * Empirical data collected across standard Squeak and Cuis test suites
     * (N = 683,434 creation calls) shows that while constants like NilObject
     * are rare, their ultra-cheap reference equality checks (cost ~1) push
     * them higher in the optimal evaluation sequence than slightly more
     * frequent, but heavier, instanceof checks (cost ~5).
     *
     * <pre>
     * | Receiver Type                        |      Count | Frequency | Cost |  Yield |
     * |--------------------------------------|------------|-----------|------|--------|
     * | AbstractSqueakObjectWithClassAndHash |    627,928 |    91.88% |    5 | 18.38% |
     * | Long (SmallInteger)                  |     26,126 |     3.82% |    5 |  0.76% |
     * | NilObject                            |      2,894 |     0.42% |    1 |  0.42% |
     * | Double                               |     10,934 |     1.60% |    5 |  0.32% |
     * | Boolean.FALSE                        |      1,856 |     0.27% |    1 |  0.27% |
     * | Boolean.TRUE                         |      1,802 |     0.26% |    1 |  0.26% |
     * | BlockClosureObject                   |      3,394 |     0.50% |    5 |  0.10% |
     * | Character / CharacterObject          |      3,054 |     0.45% |    5 |  0.09% |
     * | ContextObject                        |      2,675 |     0.39% |    5 |  0.08% |
     * | FloatObject                          |      2,587 |     0.38% |    5 |  0.08% |
     * | ForeignObject                        |        184 |     0.03% |    5 |  0.01% |
     * </pre>
     *
     * @param receiver The object requiring a class guard
     * @return A specialized LookupClassGuard instance
     */
    @NeverDefault
    public static LookupClassGuard create(final Object receiver) {
        if (receiver instanceof final AbstractSqueakObjectWithClassAndHash o) {
            return new AbstractSqueakObjectWithClassAndHashGuard((AbstractSqueakObjectWithClassAndHash) o.resolveForwardingPointer());
        } else if (receiver instanceof Long) {
            return SmallIntegerGuard.SINGLETON;
        } else if (receiver == NilObject.SINGLETON) {
            return NilGuard.SINGLETON;
        } else if (receiver instanceof Double) {
            return DoubleGuard.SINGLETON;
        } else if (receiver == Boolean.FALSE) {
            return FalseGuard.SINGLETON;
        } else if (receiver == Boolean.TRUE) {
            return TrueGuard.SINGLETON;
        } else if (receiver instanceof final BlockClosureObject closure) {
            return closure.isABlockClosure() ? BlockClosureGuard.SINGLETON : FullBlockClosureGuard.SINGLETON;
        } else if (receiver instanceof Character || receiver instanceof CharacterObject) {
            return CharacterGuard.SINGLETON;
        } else if (receiver instanceof ContextObject) {
            return ContextObjectGuard.SINGLETON;
        } else if (receiver instanceof FloatObject) {
            return FloatObjectGuard.SINGLETON;
        } else {
            assert !(receiver instanceof AbstractSqueakObject);
            return ForeignObjectGuard.SINGLETON;
        }
    }

    private static final class NilGuard extends LookupClassGuard {
        private static final NilGuard SINGLETON = new NilGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver == NilObject.SINGLETON;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).nilClass;
        }
    }

    private static final class TrueGuard extends LookupClassGuard {
        private static final TrueGuard SINGLETON = new TrueGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver == Boolean.TRUE;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).trueClass;
        }
    }

    private static final class FalseGuard extends LookupClassGuard {
        private static final FalseGuard SINGLETON = new FalseGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver == Boolean.FALSE;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).falseClass;
        }
    }

    private static final class SmallIntegerGuard extends LookupClassGuard {
        private static final SmallIntegerGuard SINGLETON = new SmallIntegerGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Long;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).smallIntegerClass;
        }
    }

    private static final class CharacterGuard extends LookupClassGuard {
        private static final CharacterGuard SINGLETON = new CharacterGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Character || receiver instanceof CharacterObject;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).characterClass;
        }
    }

    private static final class DoubleGuard extends LookupClassGuard {
        private static final DoubleGuard SINGLETON = new DoubleGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Double;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).smallFloatClass;
        }
    }

    private static final class ContextObjectGuard extends LookupClassGuard {
        private static final ContextObjectGuard SINGLETON = new ContextObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof ContextObject;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).methodContextClass;
        }
    }

    private static final class BlockClosureGuard extends LookupClassGuard {
        private static final BlockClosureGuard SINGLETON = new BlockClosureGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof final BlockClosureObject closure && closure.isABlockClosure();
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).blockClosureClass;
        }
    }

    private static final class FullBlockClosureGuard extends LookupClassGuard {
        private static final FullBlockClosureGuard SINGLETON = new FullBlockClosureGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof final BlockClosureObject closure && closure.isAFullBlockClosure();
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).getFullBlockClosureClass();
        }
    }

    private static final class FloatObjectGuard extends LookupClassGuard {
        private static final FloatObjectGuard SINGLETON = new FloatObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof FloatObject;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return SqueakImageContext.get(node).floatClass;
        }
    }

    private static final class AbstractSqueakObjectWithClassAndHashGuard extends LookupClassGuard {
        private final ClassObject expectedClass;

        private AbstractSqueakObjectWithClassAndHashGuard(final AbstractSqueakObjectWithClassAndHash receiver) {
            expectedClass = receiver.getSqueakClass();
            assert expectedClass.assertNotForwarded();
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof final AbstractSqueakObjectWithClassAndHash o && o.getSqueakClass() == expectedClass;
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            return expectedClass;
        }
    }

    private static final class ForeignObjectGuard extends LookupClassGuard {
        private static final ForeignObjectGuard SINGLETON = new ForeignObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return !SqueakGuards.isAbstractSqueakObject(receiver) && !SqueakGuards.isUsedJavaPrimitive(receiver);
        }

        @Override
        protected ClassObject getSqueakClassInternal(final Node node) {
            final SqueakImageContext image = SqueakImageContext.get(node);
            if (!image.getForeignObjectClassStableAssumption().isValid()) {
                CompilerDirectives.transferToInterpreterAndInvalidate();
            }
            return image.getForeignObjectClass();
        }
    }
}
