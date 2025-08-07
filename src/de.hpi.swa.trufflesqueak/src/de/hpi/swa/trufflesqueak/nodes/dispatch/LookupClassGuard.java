/*
 * Copyright (c) 2017-2025 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2025 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakGuards;

public abstract class LookupClassGuard {
    public abstract boolean check(Object receiver);

    public final ClassObject getSqueakClass(final Node node) {
        CompilerAsserts.partialEvaluationConstant(node);
        return getSqueakClassInternal(node);
    }

    protected abstract ClassObject getSqueakClassInternal(Node node);

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
        } else if (receiver instanceof final AbstractSqueakObjectWithClassAndHash o) {
            return new AbstractSqueakObjectWithClassAndHashGuard(o.resolveForwardingPointer());
        } else {
            assert !(receiver instanceof AbstractSqueakObject);
            return ForeignObjectGuard.SINGLETON;
        }
    }

    public static LookupClassGuard create(final Object receiver, final NativeObject selector) {
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
        } else if (receiver instanceof final AbstractSqueakObjectWithClassAndHash o) {
            final AbstractSqueakObjectWithClassAndHash resolvedObject = o.resolveForwardingPointer();
            final ClassObject resolvedObjectClassObject = resolvedObject.getSqueakClass();
            final Object lookupResult = resolvedObjectClassObject.lookupInMethodDictSlow(selector);
            if (lookupResult instanceof final CompiledCodeObject code && code.getMethodClassSlow() != resolvedObjectClassObject) {
                return new AbstractSqueakObjectWithClassAndHashHierarchyGuard(selector, code.getMethodClassSlow());
            }
            return new AbstractSqueakObjectWithClassAndHashGuard(resolvedObject);
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

    private static final class FloatObjectGuard extends LookupClassGuard {
        private static final ForeignObjectGuard SINGLETON = new ForeignObjectGuard();

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

    private static final class AbstractSqueakObjectWithClassAndHashHierarchyGuard extends LookupClassGuard {
        private final ClassObject expectedClass;
        private final NativeObject selector;

        private AbstractSqueakObjectWithClassAndHashHierarchyGuard(final NativeObject selector, final ClassObject expectedClass) {
            this.expectedClass = expectedClass;
            this.selector = selector;
            assert expectedClass.assertNotForwarded();
        }

        @Override
        public boolean check(final Object receiver) {
            if (receiver instanceof final AbstractSqueakObjectWithClassAndHash o) {
                ClassObject currentClass = o.getSqueakClass();
                while (currentClass != null) {
                    if (currentClass == expectedClass) {
                        return true;
                    }
                    if (currentClass.lookupInMethodDictSlow(selector) != null) {
                        return false;
                    }
                    currentClass = currentClass.getSuperclassOrNull();
                }
                return false;
            } else {
                return false;
            }
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
