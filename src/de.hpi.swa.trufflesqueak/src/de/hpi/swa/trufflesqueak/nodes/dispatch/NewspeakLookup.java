/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.nodes.dispatch;

import java.util.ArrayList;
import java.util.LinkedHashSet;

import com.oracle.truffle.api.Assumption;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.trufflesqueak.exceptions.SqueakExceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObjectWithClassAndHash;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.CLASS_DESCRIPTION;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.NEWSPEAK_CLASS;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayouts.NEWSPEAK_MIXIN;
import de.hpi.swa.trufflesqueak.nodes.accessing.SqueakObjectClassNode;

/**
 * Method lookup for Newspeak's absent receiver sends (implicit receiver, self, outer, and super
 * sends) and for enclosing objects. All lookups only depend on the class of the receiver of the
 * method that performs the send, the method itself, and the selector, which allows caching them
 * per receiver class.
 *
 * See StackInterpreter>>lookupImplicitReceiverSend, #lookupOuterSend:, #lookupDynamicSuperSend,
 * #lookupLexical:from:rule:, #lookupProtected:startingAt:rule:, and
 * #enclosingObjectAt:withObject:withMixin: in VMMaker.
 */
public final class NewspeakLookup {
    /** Implicit receiver send. */
    public static final int KIND_IMPLICIT = -1;
    /** Super send (the superclass is determined dynamically through the mixin of the method). */
    public static final int KIND_DYNAMIC_SUPER = -2;
    /** Self send (outer send with depth 0), values larger than zero are depths of outer sends. */
    public static final int KIND_SELF = 0;

    private NewspeakLookup() {
    }

    /**
     * Result of a lookup.
     *
     * @param lookupResult the method (or object as method) to invoke, or null for
     *            doesNotUnderstand:
     * @param lookupClass the class at which lookup of doesNotUnderstand: starts
     * @param enclosingObjectHolder the mixin application that holds the enclosing object to be used
     *            as receiver, or null if the receiver of the method is the receiver of the send
     */
    public static final class Result {
        public final Object lookupResult;
        public final ClassObject lookupClass;
        public final ClassObject enclosingObjectHolder;
        @CompilationFinal(dimensions = 1) public final Assumption[] assumptions;

        Result(final Object lookupResult, final ClassObject lookupClass, final ClassObject enclosingObjectHolder, final Assumption[] assumptions) {
            this.lookupResult = lookupResult;
            this.lookupClass = lookupClass;
            this.enclosingObjectHolder = enclosingObjectHolder;
            this.assumptions = assumptions;
        }

        /** Answers the receiver of the send ("absent receiver"). */
        public Object getReceiver(final Object methodReceiver) {
            if (enclosingObjectHolder == null) {
                return methodReceiver;
            } else {
                return enclosingObjectHolder.getOtherPointer(NEWSPEAK_CLASS.ENCLOSING_OBJECT);
            }
        }

        public Assumption[] getAssumptions() {
            return assumptions;
        }

        public boolean isValid() {
            for (final Assumption assumption : assumptions) {
                if (!assumption.isValid()) {
                    return false;
                }
            }
            return true;
        }
    }

    /** Collects the classes visited by a lookup, on which the result of the lookup depends. */
    private static final class Dependencies {
        private final LinkedHashSet<ClassObject> classes = new LinkedHashSet<>();

        private ClassObject add(final ClassObject classObject) {
            classes.add(classObject);
            return classObject;
        }

        private void addWithSuperclasses(final ClassObject classObject) {
            ClassObject current = classObject;
            while (current != null && classes.add(current)) {
                current = current.getSuperclassOrNull();
            }
        }

        private Assumption[] toAssumptions(final NativeObject selector, final Object lookupResult) {
            final ArrayList<Assumption> assumptions = new ArrayList<>(classes.size() + 1);
            if (lookupResult instanceof final CompiledCodeObject method) {
                assumptions.add(method.getCallTargetStable());
            } else if (selector != null) {
                assumptions.add(selector.getSqueakClass().getImage().getFailedLookupAssumption(selector));
            }
            for (final ClassObject classObject : classes) {
                assumptions.add(classObject.getClassHierarchyAndMethodDictStable());
            }
            return assumptions.toArray(new Assumption[0]);
        }
    }

    /**
     * Global cache for megamorphic send sites, in the spirit of the VM's method cache. Entries are
     * validated through the assumptions of their results.
     */
    public static final class Cache {
        private static final int SIZE = 1 << 10;
        private static final int MASK = SIZE - 1;

        private final Entry[] entries = new Entry[SIZE];

        private record Entry(ClassObject receiverClass, CompiledCodeObject method, NativeObject selector, int kind, Result result) {
        }

        /**
         * @param selector the selector of the send, or null to look up the enclosing object at
         *            depth kind
         */
        @TruffleBoundary
        public Result lookup(final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject selector, final int kind) {
            final int index = (System.identityHashCode(receiverClass) ^ System.identityHashCode(method) * 31 ^ System.identityHashCode(selector) * 961 ^ kind) & MASK;
            final Entry entry = entries[index];
            if (entry != null && entry.receiverClass == receiverClass && entry.method == method && entry.selector == selector && entry.kind == kind && entry.result.isValid()) {
                return entry.result;
            }
            final Result result = selector == null ? lookupEnclosingObject(receiverClass, method, kind) : NewspeakLookup.lookup(receiverClass, method, selector, kind);
            entries[index] = new Entry(receiverClass, method, selector, kind, result);
            return result;
        }
    }

    @TruffleBoundary
    public static Result lookup(final ClassObject receiverClass, final CompiledCodeObject method, final NativeObject selector, final int kind) {
        final Dependencies dependencies = new Dependencies();
        final Object methodMixin = method.getMethodClassOrMixinSlow();
        if (kind == KIND_IMPLICIT) {
            return lookupImplicitReceiverSend(dependencies, receiverClass, methodMixin, selector);
        } else if (kind == KIND_DYNAMIC_SUPER) {
            return lookupDynamicSuperSend(dependencies, receiverClass, methodMixin, selector);
        } else {
            assert kind >= KIND_SELF;
            return lookupOuterSend(dependencies, receiverClass, methodMixin, selector, kind);
        }
    }

    /**
     * Answers the mixin application that holds the enclosing object at the given depth (in a
     * {@link Result} without lookup result), see
     * StackInterpreter>>enclosingObjectAt:withObject:withMixin:.
     */
    @TruffleBoundary
    public static Result lookupEnclosingObject(final ClassObject receiverClass, final CompiledCodeObject method, final int depth) {
        final Dependencies dependencies = new Dependencies();
        ClassObject currentClass = receiverClass;
        ClassObject holder = null;
        Object targetMixin = method.getMethodClassOrMixinSlow();
        for (int i = 0; i < depth; i++) {
            holder = findApplicationOfTargetMixin(dependencies, targetMixin, currentClass);
            currentClass = classOfEnclosingObject(holder);
            targetMixin = enclosingMixinOf(targetMixin);
        }
        return new Result(null, currentClass, holder, dependencies.toAssumptions(null, null));
    }

    /** See StackInterpreter>>lookupImplicitReceiverSend. */
    private static Result lookupImplicitReceiverSend(final Dependencies dependencies, final ClassObject receiverClass, final Object methodMixin, final NativeObject selector) {
        ClassObject candidateReceiverClass = receiverClass;
        ClassObject holder = null;
        Object candidateMixin = methodMixin;
        while (true) {
            final ClassObject candidateMixinApplication = findApplicationOfTargetMixin(dependencies, candidateMixin, candidateReceiverClass);
            if (candidateMixinApplication.lookupInOwnMethodDictSlow(selector) != null) {
                return lookupLexical(dependencies, candidateReceiverClass, holder, candidateMixin, selector);
            }
            candidateMixin = enclosingMixinOf(candidateMixin);
            if (candidateMixin == NilObject.SINGLETON) {
                break;
            }
            holder = candidateMixinApplication;
            candidateReceiverClass = classOfEnclosingObject(candidateMixinApplication);
        }
        /* There is no lexically visible method, so the implicit receiver is the method receiver. */
        return lookupProtected(dependencies, receiverClass, null, receiverClass, selector);
    }

    /** See StackInterpreter>>lookupOuterSend:. */
    private static Result lookupOuterSend(final Dependencies dependencies, final ClassObject receiverClass, final Object methodMixin, final NativeObject selector, final int depth) {
        ClassObject currentClass = receiverClass;
        ClassObject holder = null;
        Object targetMixin = methodMixin;
        for (int i = 0; i < depth; i++) {
            holder = findApplicationOfTargetMixin(dependencies, targetMixin, currentClass);
            currentClass = classOfEnclosingObject(holder);
            targetMixin = enclosingMixinOf(targetMixin);
        }
        return lookupLexical(dependencies, currentClass, holder, targetMixin, selector);
    }

    /** See StackInterpreter>>lookupDynamicSuperSend. */
    private static Result lookupDynamicSuperSend(final Dependencies dependencies, final ClassObject receiverClass, final Object methodMixin, final NativeObject selector) {
        final ClassObject methodMixinApplication = findApplicationOfTargetMixin(dependencies, methodMixin, receiverClass);
        final ClassObject superclass = methodMixinApplication.getSuperclassOrNull();
        if (superclass == null) {
            throw SqueakException.create("Newspeak super send of", selector, "in class without superclass:", methodMixinApplication);
        }
        return lookupProtected(dependencies, receiverClass, null, superclass, selector);
    }

    /**
     * A shared part of the lookup for implicit receiver sends that found a lexically visible
     * method, and self and outer sends. See StackInterpreter>>lookupLexical:from:rule:.
     */
    private static Result lookupLexical(final Dependencies dependencies, final ClassObject receiverClass, final ClassObject holder, final Object mixin, final NativeObject selector) {
        final ClassObject mixinApplication = findApplicationOfTargetMixin(dependencies, mixin, receiverClass);
        final Object lookupResult = mixinApplication.lookupInOwnMethodDictSlow(selector);
        if (lookupResult instanceof final CompiledCodeObject method && method.isPrivate()) {
            return new Result(method, receiverClass, holder, dependencies.toAssumptions(selector, method));
        }
        return lookupProtected(dependencies, receiverClass, holder, receiverClass, selector);
    }

    /**
     * A shared part of the lookup for self, outer, or implicit receiver sends that did not find a
     * private lexically visible method, and super sends. See
     * StackInterpreter>>lookupProtected:startingAt:rule:.
     */
    private static Result lookupProtected(final Dependencies dependencies, final ClassObject receiverClass, final ClassObject holder, final ClassObject startClass, final NativeObject selector) {
        dependencies.addWithSuperclasses(receiverClass);
        dependencies.addWithSuperclasses(startClass);
        final Object lookupResult = startClass.lookupProtectedInMethodDictSlow(selector);
        /* The lookup of doesNotUnderstand: starts at startClass. */
        return new Result(lookupResult, startClass, holder, dependencies.toAssumptions(selector, lookupResult));
    }

    /**
     * Finds the mixin application of which the given behavior is a subclass that is an
     * application of targetMixin. See StackInterpreter>>findApplicationOfTargetMixin:startingAtBehavior:.
     */
    private static ClassObject findApplicationOfTargetMixin(final Dependencies dependencies, final Object targetMixin, final ClassObject behavior) {
        ClassObject mixinApplication = behavior;
        while (mixinApplication != null) {
            dependencies.add(mixinApplication);
            if (mixinApplication == targetMixin || mixinOf(mixinApplication) == targetMixin) {
                return mixinApplication;
            }
            mixinApplication = mixinApplication.getSuperclassOrNull();
        }
        throw SqueakException.create("Unable to find application of mixin", targetMixin, "in behavior", behavior);
    }

    private static Object mixinOf(final ClassObject mixinApplication) {
        if (mixinApplication.size() > NEWSPEAK_CLASS.MIXIN) {
            return resolve(mixinApplication.getOtherPointer(NEWSPEAK_CLASS.MIXIN));
        } else {
            return NilObject.SINGLETON;
        }
    }

    private static Object enclosingMixinOf(final Object mixin) {
        if (mixin instanceof final ClassObject c) {
            return c.size() > NEWSPEAK_MIXIN.ENCLOSING_MIXIN ? resolve(c.getOtherPointer(NEWSPEAK_MIXIN.ENCLOSING_MIXIN)) : NilObject.SINGLETON;
        } else if (mixin instanceof final AbstractPointersObject p && p.size() > NEWSPEAK_MIXIN.ENCLOSING_MIXIN) {
            return resolve(p.instVarAt0Slow(NEWSPEAK_MIXIN.ENCLOSING_MIXIN));
        } else {
            return NilObject.SINGLETON;
        }
    }

    private static ClassObject classOfEnclosingObject(final ClassObject mixinApplication) {
        assert mixinApplication.size() > NEWSPEAK_CLASS.ENCLOSING_OBJECT && NEWSPEAK_CLASS.ENCLOSING_OBJECT >= CLASS_DESCRIPTION.INLINE_POINTERS;
        return SqueakObjectClassNode.executeUncached(resolve(mixinApplication.getOtherPointer(NEWSPEAK_CLASS.ENCLOSING_OBJECT)));
    }

    private static Object resolve(final Object object) {
        return object instanceof final AbstractSqueakObjectWithClassAndHash o ? o.resolveForwardingPointer() : object;
    }
}
