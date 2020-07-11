/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.HashSet;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.InvalidAssumptionException;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;

public abstract class CachedDispatchGuard {
    private static final HashSet<Assumption> ASSUMPTION_SET = new HashSet<>();

    @CompilationFinal(dimensions = 1) protected final Assumption[] assumptions;

    public CachedDispatchGuard(final Assumption[] assumptions) {
        this.assumptions = assumptions;
    }

    public final boolean check(final Object receiver) throws InvalidAssumptionException {
        checkAssumptions();
        return guardCheck(receiver);
    }

    @ExplodeLoop
    private void checkAssumptions() throws InvalidAssumptionException {
        for (final Assumption assumption : assumptions) {
            assumption.check();
        }
    }

    protected abstract boolean guardCheck(Object receiver);

    public final boolean checkMustSucceed(final Object receiver) {
        try {
            return check(receiver);
        } catch (final InvalidAssumptionException e) {
            return false;
        }
    }

    public static CachedDispatchGuard create(final Object receiver, final CompiledCodeObject method) {
        final Assumption[] assumptions = getMethodDictAssumptions(receiver, method);
        if (receiver == NilObject.SINGLETON) {
            return new NilGuard(assumptions);
        } else if (receiver == Boolean.TRUE) {
            return new TrueGuard(assumptions);
        } else if (receiver == Boolean.FALSE) {
            return new FalseGuard(assumptions);
        } else if (receiver instanceof Long) {
            return new SmallIntegerGuard(assumptions);
        } else if (receiver instanceof Character) {
            return new CharacterGuard(assumptions);
        } else if (receiver instanceof Double) {
            return new DoubleGuard(assumptions);
        } else if (receiver instanceof BlockClosureObject) {
            return new BlockClosureObjectGuard(assumptions);
        } else if (receiver instanceof CharacterObject) {
            return new CharacterObjectGuard(assumptions);
        } else if (receiver instanceof ContextObject) {
            return new ContextObjectGuard(assumptions);
        } else if (receiver instanceof FloatObject) {
            return new FloatObjectGuard(assumptions);
        } else if (receiver instanceof AbstractPointersObject) {
            return new AbstractPointersObjectGuard(assumptions, (AbstractPointersObject) receiver);
        } else if (receiver instanceof AbstractSqueakObjectWithClassAndHash) {
            return new AbstractSqueakObjectWithClassAndHashGuard(assumptions, (AbstractSqueakObjectWithClassAndHash) receiver);
        } else if (receiver instanceof TruffleObject) {
            return new ForeignObjectGuard(assumptions);
        } else {
            throw SqueakException.create("Should not be reached");
        }
    }

    /*
     * Returns all methodDict assumptions until the class defining the method. If any of these
     * methodDicts change, the dispatch may no longer be valid.
     */
    private static Assumption[] getMethodDictAssumptions(final Object receiver, final CompiledCodeObject method) {
        final ClassObject methodClass = method.getMethodClassSlow();
        final ClassObject receiverClass = SqueakObjectClassNode.getUncached().executeLookup(receiver);
        ASSUMPTION_SET.clear();
        ClassObject currentClass = receiverClass;
        while (currentClass != methodClass) {
            ASSUMPTION_SET.add(currentClass.getMethodDictStable());
            currentClass = currentClass.getSuperclassOrNull();
            assert currentClass != null;
        }
        ASSUMPTION_SET.add(currentClass.getMethodDictStable());
        return ASSUMPTION_SET.toArray(new Assumption[0]);
    }

    private static final class NilGuard extends CachedDispatchGuard {
        private NilGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver == NilObject.SINGLETON;
        }
    }

    private static final class TrueGuard extends CachedDispatchGuard {
        private TrueGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver == Boolean.TRUE;
        }
    }

    private static final class FalseGuard extends CachedDispatchGuard {
        private FalseGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver == Boolean.FALSE;
        }
    }

    private static final class SmallIntegerGuard extends CachedDispatchGuard {
        private SmallIntegerGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof Long;
        }
    }

    private static final class CharacterGuard extends CachedDispatchGuard {
        private CharacterGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof Character;
        }
    }

    private static final class DoubleGuard extends CachedDispatchGuard {
        private DoubleGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof Double;
        }
    }

    private static final class BlockClosureObjectGuard extends CachedDispatchGuard {
        private BlockClosureObjectGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof BlockClosureObject;
        }
    }

    private static final class CharacterObjectGuard extends CachedDispatchGuard {
        private CharacterObjectGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof CharacterObject;
        }
    }

    private static final class ContextObjectGuard extends CachedDispatchGuard {
        private ContextObjectGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof ContextObject;
        }
    }

    private static final class FloatObjectGuard extends CachedDispatchGuard {
        private FloatObjectGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof FloatObject;
        }
    }

    private static final class AbstractPointersObjectGuard extends CachedDispatchGuard {
        private final ObjectLayout expectedLayout;

        private AbstractPointersObjectGuard(final Assumption[] assumptions, final AbstractPointersObject receiver) {
            super(assumptions);
            expectedLayout = receiver.getLayout();
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof AbstractPointersObject && ((AbstractPointersObject) receiver).getLayout() == expectedLayout;
        }
    }

    private static final class AbstractSqueakObjectWithClassAndHashGuard extends CachedDispatchGuard {
        private final ClassObject expectedClass;

        private AbstractSqueakObjectWithClassAndHashGuard(final Assumption[] assumptions, final AbstractSqueakObjectWithClassAndHash receiver) {
            super(assumptions);
            expectedClass = receiver.getSqueakClass();
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return receiver instanceof AbstractSqueakObjectWithClassAndHash && ((AbstractSqueakObjectWithClassAndHash) receiver).getSqueakClass() == expectedClass;
        }
    }

    private static final class ForeignObjectGuard extends CachedDispatchGuard {
        private ForeignObjectGuard(final Assumption[] assumptions) {
            super(assumptions);
        }

        @Override
        protected boolean guardCheck(final Object receiver) {
            return !SqueakGuards.isAbstractSqueakObject(receiver) && !SqueakGuards.isUsedJavaPrimitive(receiver);
        }
    }
}
