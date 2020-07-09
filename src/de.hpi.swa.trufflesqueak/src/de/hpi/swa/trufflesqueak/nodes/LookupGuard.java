/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.interop.TruffleObject;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;

public abstract class LookupGuard {

    public abstract boolean check(Object receiver);

    public static LookupGuard create(final Object receiver) {
        if (receiver == NilObject.SINGLETON) {
            return NilGuard.SINGLETON;
        } else if (receiver == Boolean.TRUE) {
            return TrueGuard.SINGLETON;
        } else if (receiver == Boolean.FALSE) {
            return FalseGuard.SINGLETON;
        } else if (receiver instanceof Long) {
            return SmallIntegerGuard.SINGLETON;
        } else if (receiver instanceof Character) {
            return CharacterGuard.SINGLETON;
        } else if (receiver instanceof Double) {
            return DoubleGuard.SINGLETON;
        } else if (receiver instanceof BlockClosureObject) {
            return BlockClosureObjectGuard.SINGLETON;
        } else if (receiver instanceof CharacterObject) {
            return CharacterObjectGuard.SINGLETON;
        } else if (receiver instanceof ContextObject) {
            return ContextObjectGuard.SINGLETON;
        } else if (receiver instanceof FloatObject) {
            return FloatObjectGuard.SINGLETON;
        } else if (receiver instanceof AbstractPointersObject) {
            return new AbstractPointersObjectGuard((AbstractPointersObject) receiver);
        } else if (receiver instanceof AbstractSqueakObjectWithClassAndHash) {
            return new AbstractSqueakObjectWithClassAndHashGuard((AbstractSqueakObjectWithClassAndHash) receiver);
        } else if (receiver instanceof TruffleObject) {
            return new ForeignObjectGuard();
        } else {
            throw SqueakException.create("Should not be reached");
        }
    }

    private static final class NilGuard extends LookupGuard {
        private static final LookupGuard.NilGuard SINGLETON = new NilGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver == NilObject.SINGLETON;
        }
    }

    private static final class TrueGuard extends LookupGuard {
        private static final LookupGuard.TrueGuard SINGLETON = new TrueGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver == Boolean.TRUE;
        }
    }

    private static final class FalseGuard extends LookupGuard {
        private static final LookupGuard.FalseGuard SINGLETON = new FalseGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver == Boolean.FALSE;
        }
    }

    private static final class SmallIntegerGuard extends LookupGuard {
        private static final LookupGuard.SmallIntegerGuard SINGLETON = new SmallIntegerGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Long;
        }
    }

    private static final class CharacterGuard extends LookupGuard {
        private static final LookupGuard.CharacterGuard SINGLETON = new CharacterGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Character;
        }
    }

    private static final class DoubleGuard extends LookupGuard {
        private static final LookupGuard.DoubleGuard SINGLETON = new DoubleGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Double;
        }
    }

    private static final class BlockClosureObjectGuard extends LookupGuard {
        private static final LookupGuard.BlockClosureObjectGuard SINGLETON = new BlockClosureObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof BlockClosureObject;
        }
    }

    private static final class CharacterObjectGuard extends LookupGuard {
        private static final LookupGuard.CharacterObjectGuard SINGLETON = new CharacterObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof CharacterObject;
        }
    }

    private static final class ContextObjectGuard extends LookupGuard {
        private static final LookupGuard.ContextObjectGuard SINGLETON = new ContextObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof ContextObject;
        }
    }

    private static final class FloatObjectGuard extends LookupGuard {
        private static final LookupGuard.FloatObjectGuard SINGLETON = new FloatObjectGuard();

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof FloatObject;
        }
    }

    private static final class AbstractPointersObjectGuard extends LookupGuard {
        private final ObjectLayout expectedLayout;

        private AbstractPointersObjectGuard(final AbstractPointersObject receiver) {
            expectedLayout = receiver.getLayout();
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof AbstractPointersObject && ((AbstractPointersObject) receiver).getLayout() == expectedLayout;
        }
    }

    private static final class AbstractSqueakObjectWithClassAndHashGuard extends LookupGuard {
        private final ClassObject expectedClass;

        private AbstractSqueakObjectWithClassAndHashGuard(final AbstractSqueakObjectWithClassAndHash receiver) {
            expectedClass = receiver.getSqueakClass();
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof AbstractSqueakObjectWithClassAndHash && ((AbstractSqueakObjectWithClassAndHash) receiver).getSqueakClass() == expectedClass;
        }
    }

    private static final class ForeignObjectGuard extends LookupGuard {
        @Override
        public boolean check(final Object receiver) {
            return !SqueakGuards.isAbstractSqueakObject(receiver) && !SqueakGuards.isUsedJavaPrimitive(receiver);
        }
    }
}
