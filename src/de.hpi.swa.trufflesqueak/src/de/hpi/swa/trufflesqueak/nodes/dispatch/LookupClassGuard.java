/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;

public abstract class LookupClassGuard {
    public abstract boolean check(Object receiver);

    public static LookupClassGuard create(final Object receiver) {
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
        } else {
            return ForeignObjectGuard.SINGLETON;
        }
    }

    private static final class NilGuard extends LookupClassGuard {
        private static final NilGuard SINGLETON = new NilGuard();

        private NilGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver == NilObject.SINGLETON;
        }
    }

    private static final class TrueGuard extends LookupClassGuard {
        private static final TrueGuard SINGLETON = new TrueGuard();

        private TrueGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver == Boolean.TRUE;
        }
    }

    private static final class FalseGuard extends LookupClassGuard {
        private static final FalseGuard SINGLETON = new FalseGuard();

        private FalseGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver == Boolean.FALSE;
        }
    }

    private static final class SmallIntegerGuard extends LookupClassGuard {
        private static final SmallIntegerGuard SINGLETON = new SmallIntegerGuard();

        private SmallIntegerGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Long;
        }
    }

    private static final class CharacterGuard extends LookupClassGuard {
        private static final CharacterGuard SINGLETON = new CharacterGuard();

        private CharacterGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Character;
        }
    }

    private static final class DoubleGuard extends LookupClassGuard {
        private static final DoubleGuard SINGLETON = new DoubleGuard();

        private DoubleGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof Double;
        }
    }

    private static final class BlockClosureObjectGuard extends LookupClassGuard {
        private static final BlockClosureObjectGuard SINGLETON = new BlockClosureObjectGuard();

        private BlockClosureObjectGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof BlockClosureObject;
        }
    }

    private static final class CharacterObjectGuard extends LookupClassGuard {
        private static final CharacterObjectGuard SINGLETON = new CharacterObjectGuard();

        private CharacterObjectGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof CharacterObject;
        }
    }

    private static final class ContextObjectGuard extends LookupClassGuard {
        private static final ContextObjectGuard SINGLETON = new ContextObjectGuard();

        private ContextObjectGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof ContextObject;
        }
    }

    private static final class FloatObjectGuard extends LookupClassGuard {
        private static final ForeignObjectGuard SINGLETON = new ForeignObjectGuard();

        private FloatObjectGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof FloatObject;
        }
    }

    private static final class AbstractPointersObjectGuard extends LookupClassGuard {
        private final ObjectLayout expectedLayout;

        private AbstractPointersObjectGuard(final AbstractPointersObject receiver) {
            expectedLayout = receiver.getLayout();
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof AbstractPointersObject && ((AbstractPointersObject) receiver).getLayout() == expectedLayout;
        }
    }

    private static final class AbstractSqueakObjectWithClassAndHashGuard extends LookupClassGuard {
        private final ClassObject expectedClass;

        private AbstractSqueakObjectWithClassAndHashGuard(final AbstractSqueakObjectWithClassAndHash receiver) {
            expectedClass = receiver.getSqueakClass();
        }

        @Override
        public boolean check(final Object receiver) {
            return receiver instanceof AbstractSqueakObjectWithClassAndHash && ((AbstractSqueakObjectWithClassAndHash) receiver).getSqueakClass() == expectedClass;
        }
    }

    private static final class ForeignObjectGuard extends LookupClassGuard {
        private static final ForeignObjectGuard SINGLETON = new ForeignObjectGuard();

        private ForeignObjectGuard() {
        }

        @Override
        public boolean check(final Object receiver) {
            return !SqueakGuards.isAbstractSqueakObject(receiver) && !SqueakGuards.isUsedJavaPrimitive(receiver);
        }
    }
}
