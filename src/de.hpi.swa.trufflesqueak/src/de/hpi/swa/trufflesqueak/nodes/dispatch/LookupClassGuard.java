/*
 * Copyright (c) 2017-2022 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2022 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerAsserts;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithHeader;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;

public abstract class LookupClassGuard {
    protected abstract boolean check(Object receiver);

    protected final int getClassIndex(final SqueakImageContext image) {
        CompilerAsserts.partialEvaluationConstant(image);
        return getClassIndexInternal(image);
    }

    protected abstract int getClassIndexInternal(SqueakImageContext image);

    protected Assumption getIsValidAssumption() {
        return Assumption.ALWAYS_VALID;
    }

    public static LookupClassGuard create(final Object receiver) {
        if (receiver == NilObject.SINGLETON) {
            return NilGuard.SINGLETON;
        } else if (receiver == Boolean.TRUE) {
            return TrueGuard.SINGLETON;
        } else if (receiver == Boolean.FALSE) {
            return FalseGuard.SINGLETON;
        } else if (receiver instanceof Long) {
            return SmallIntegerGuard.SINGLETON;
        } else if (receiver instanceof Character || receiver instanceof CharacterObject) {
            return CharacterGuard.SINGLETON;
        } else if (receiver instanceof Double) {
            return DoubleGuard.SINGLETON;
        } else if (receiver instanceof ContextObject) {
            return ContextObjectGuard.SINGLETON;
        } else if (receiver instanceof FloatObject) {
            return FloatObjectGuard.SINGLETON;
        } else if (receiver instanceof AbstractPointersObject) {
            return new AbstractPointersObjectGuard((AbstractPointersObject) receiver);
        } else if (receiver instanceof AbstractSqueakObjectWithHeader) {
            return new AbstractSqueakObjectWithClassAndHashGuard((AbstractSqueakObjectWithHeader) receiver);
        } else {
            assert !(receiver instanceof AbstractSqueakObject);
            return ForeignObjectGuard.SINGLETON;
        }
    }

    private static final class NilGuard extends LookupClassGuard {
        private static final NilGuard SINGLETON = new NilGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver == NilObject.SINGLETON;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.nilClassIndex;
        }
    }

    private static final class TrueGuard extends LookupClassGuard {
        private static final TrueGuard SINGLETON = new TrueGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver == Boolean.TRUE;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.trueClassIndex;
        }
    }

    private static final class FalseGuard extends LookupClassGuard {
        private static final FalseGuard SINGLETON = new FalseGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver == Boolean.FALSE;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.falseClassIndex;
        }
    }

    private static final class SmallIntegerGuard extends LookupClassGuard {
        private static final SmallIntegerGuard SINGLETON = new SmallIntegerGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof Long;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.smallIntegerClassIndex;
        }
    }

    private static final class CharacterGuard extends LookupClassGuard {
        private static final CharacterGuard SINGLETON = new CharacterGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof Character || receiver instanceof CharacterObject;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.characterClassIndex;
        }
    }

    private static final class DoubleGuard extends LookupClassGuard {
        private static final DoubleGuard SINGLETON = new DoubleGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof Double;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.smallFloatClassIndex;
        }
    }

    private static final class ContextObjectGuard extends LookupClassGuard {
        private static final ContextObjectGuard SINGLETON = new ContextObjectGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof ContextObject;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.methodContextClassIndex;
        }
    }

    private static final class FloatObjectGuard extends LookupClassGuard {
        private static final ForeignObjectGuard SINGLETON = new ForeignObjectGuard();

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof FloatObject;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.floatClassIndex;
        }
    }

    private static final class AbstractPointersObjectGuard extends LookupClassGuard {
        private final ObjectLayout expectedLayout;

        private AbstractPointersObjectGuard(final AbstractPointersObject receiver) {
            if (!receiver.getLayout().isValid()) {
                /* Ensure only valid layouts are cached. */
                receiver.updateLayout();
            }
            expectedLayout = receiver.getLayout();
            assert expectedLayout.isValid();
        }

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof AbstractPointersObject && ((AbstractPointersObject) receiver).getLayout() == expectedLayout;
        }

        @Override
        protected Assumption getIsValidAssumption() {
            return expectedLayout.getValidAssumption();
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return expectedLayout.getClassIndex();
        }
    }

    private static final class AbstractSqueakObjectWithClassAndHashGuard extends LookupClassGuard {
        private final int expectedClassIndex;

        private AbstractSqueakObjectWithClassAndHashGuard(final AbstractSqueakObjectWithHeader receiver) {
            expectedClassIndex = receiver.getSqueakClassIndex();
        }

        @Override
        protected boolean check(final Object receiver) {
            return receiver instanceof AbstractSqueakObjectWithHeader && ((AbstractSqueakObjectWithHeader) receiver).getSqueakClassIndex() == expectedClassIndex;
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return expectedClassIndex;
        }
    }

    private static final class ForeignObjectGuard extends LookupClassGuard {
        private static final ForeignObjectGuard SINGLETON = new ForeignObjectGuard();

        @Override
        protected boolean check(final Object receiver) {
            return !SqueakGuards.isAbstractSqueakObject(receiver) && !SqueakGuards.isUsedJavaPrimitive(receiver);
        }

        @Override
        protected int getClassIndexInternal(final SqueakImageContext image) {
            return image.getForeignObjectClassIndex();
        }
    }
}
