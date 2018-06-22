package de.hpi.swa.graal.squeak.nodes.bytecodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.debug.DebuggerTags;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrumentation.GenerateWrapper;
import com.oracle.truffle.api.instrumentation.InstrumentableNode;
import com.oracle.truffle.api.instrumentation.ProbeNode;
import com.oracle.truffle.api.instrumentation.StandardTags;
import com.oracle.truffle.api.instrumentation.Tag;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.graal.squeak.exceptions.PrimitiveExceptions.PrimitiveWithoutResultException;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.nodes.DispatchSendNode;
import de.hpi.swa.graal.squeak.nodes.LookupNode;
import de.hpi.swa.graal.squeak.nodes.accessing.CompiledCodeNodes.GetCompiledMethodNode;
import de.hpi.swa.graal.squeak.nodes.context.SqueakLookupClassNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPopNReversedNode;
import de.hpi.swa.graal.squeak.nodes.context.stack.StackPushNode;

public final class SendBytecodes {

    @GenerateWrapper
    public abstract static class AbstractSendNode extends AbstractBytecodeNode implements InstrumentableNode {
        @CompilationFinal protected final NativeObject selector;
        @CompilationFinal private final int argumentCount;
        @Child protected SqueakLookupClassNode lookupClassNode;
        @Child private LookupNode lookupNode = LookupNode.create();
        @Child private DispatchSendNode dispatchSendNode;
        @Child private StackPopNReversedNode popNReversedNode;
        @Child private StackPushNode pushNode = StackPushNode.create();
        @CompilationFinal private SourceSection sourceSection;

        private static class Invocation {
            long totalTime;
            long num;

            public Invocation(final long time) {
                totalTime = time;
                num = 1;
            }

            public void add(final long time) {
                num++;
                totalTime += Math.max(time, 1);
            }

            public double relativeTime() {
                return totalTime / (double) num;
            }

            public int compare(final Invocation other) {
                return relativeTime() < other.relativeTime() ? -1 : relativeTime() == other.relativeTime() ? 0 : 1;
            }
        }

        private static HashMap<String, Invocation> calls = new HashMap<>();
        private static long lastReport = 0;

        private static void report(final CompiledCodeObject lookupResult, final long time) {
            final long now = System.currentTimeMillis();

            if (now - lastReport > 8000) {
                displayReport(true);
                lastReport = now;
            }

            final String s = lookupResult.toString();
            if (calls.containsKey(s)) {
                calls.get(s).add(time);
            } else {
                calls.put(s, new Invocation(time));
            }
        }

        private static void displayReport(final boolean clearAfter) {
            System.err.println(">> " + calls.size() + " Entries.");
            List<Entry<String, Invocation>> list = new ArrayList<>(calls.entrySet());
            list = list.stream().filter((a) -> a.getValue().num > 100).sorted((a, b) -> b.getValue().compare(a.getValue())).collect(Collectors.toList());

            // Collections.sort(list, (a, b) -> b.getValue().compare(a.getValue()));
            int num = 0;
            for (Entry<String, Invocation> entry : list) {
                if (num > 30)
                    break;
                System.err.println(" --- " + entry.getKey() + " : " + entry.getValue().totalTime + " / " + entry.getValue().num + " (" + entry.getValue().relativeTime() + ")");
                num++;
            }
            System.err.println("");

            if (clearAfter)
                calls.clear();
        }

        private AbstractSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object sel, final int argcount) {
            super(code, index, numBytecodes);
            selector = sel instanceof NativeObject ? (NativeObject) sel : code.image.doesNotUnderstand;
            argumentCount = argcount;
            lookupClassNode = SqueakLookupClassNode.create(code.image);
            popNReversedNode = StackPopNReversedNode.create(code, 1 + argumentCount);
            dispatchSendNode = DispatchSendNode.create(code.image);
        }

        protected AbstractSendNode(final AbstractSendNode original) {
            this(original.code, original.index, original.numBytecodes, original.selector, original.argumentCount);
        }

        @Override
        public final void executeVoid(final VirtualFrame frame) {
            final Object result;
            try {
                result = executeSend(frame);
            } catch (PrimitiveWithoutResultException e) {
                return; // ignoring result
            }
            pushNode.executeWrite(frame, result);
        }

        public final Object executeSend(final VirtualFrame frame) {
            final Object[] rcvrAndArgs = (Object[]) popNReversedNode.executeRead(frame);
            final ClassObject rcvrClass = lookupClassNode.executeLookup(rcvrAndArgs[0]);
            final Object lookupResult = lookupNode.executeLookup(rcvrClass, selector);
            final Object contextOrMarker = getContextOrMarker(frame);

            // long start = System.currentTimeMillis();
            final Object ret = dispatchSendNode.executeSend(frame, selector, lookupResult, rcvrClass, rcvrAndArgs, contextOrMarker);
            // report((CompiledCodeObject) lookupResult, System.currentTimeMillis() - start);
            return ret;
        }

        public final Object getSelector() {
            return selector;
        }

        @Override
        public String toString() {
            return "send: " + selector.asString();
        }

        @Override
        public final boolean hasTag(final Class<? extends Tag> tag) {
            return (tag == StandardTags.CallTag.class) || (tag == DebuggerTags.AlwaysHalt.class);
        }

        @Override
        public boolean isInstrumentable() {
            return true;
        }

        public WrapperNode createWrapper(final ProbeNode probe) {
            return new AbstractSendNodeWrapper(this, this, probe);
        }
    }

    public static final class SecondExtendedSendNode extends AbstractSendNode {
        public SecondExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int i) {
            super(code, index, numBytecodes, code.getLiteral(i & 63), i >> 6);
        }
    }

    public static final class SendLiteralSelectorNode extends AbstractSendNode {
        public static AbstractBytecodeNode create(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int argCount) {
            final Object selector = code.getLiteral(literalIndex);
            return new SendLiteralSelectorNode(code, index, numBytecodes, selector, argCount);
        }

        public SendLiteralSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argCount) {
            super(code, index, numBytecodes, selector, argCount);
        }
    }

    public static final class SendSelectorNode extends AbstractSendNode {
        public static SendSelectorNode createForSpecialSelector(final CompiledCodeObject code, final int index, final int selectorIndex) {
            final NativeObject specialSelector = code.image.specialSelectorsArray[selectorIndex];
            final int numArguments = code.image.specialSelectorsNumArgs[selectorIndex];
            return new SendSelectorNode(code, index, 1, specialSelector, numArguments);
        }

        public SendSelectorNode(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int argcount) {
            super(code, index, numBytecodes, selector, argcount);
        }
    }

    public static final class SendSelfSelector extends AbstractSendNode {
        public SendSelfSelector(final CompiledCodeObject code, final int index, final int numBytecodes, final Object selector, final int numArgs) {
            super(code, index, numBytecodes, selector, numArgs);
        }
    }

    public static final class SingleExtendedSendNode extends AbstractSendNode {
        public SingleExtendedSendNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int param) {
            super(code, index, numBytecodes, code.getLiteral(param & 31), param >> 5);
        }
    }

    public static final class SingleExtendedSuperNode extends AbstractSendNode {

        protected static class SqueakLookupClassSuperNode extends SqueakLookupClassNode {
            @Child private GetCompiledMethodNode getMethodNode = GetCompiledMethodNode.create();
            @CompilationFinal private final CompiledCodeObject code;

            public SqueakLookupClassSuperNode(final CompiledCodeObject code) {
                super(code.image);
                this.code = code; // storing both, image and code, because of class hierarchy
            }

            @Override
            public ClassObject executeLookup(final Object receiver) {
                final ClassObject compiledInClass = getMethodNode.execute(code).getCompiledInClass();
                final Object superclass = compiledInClass.getSuperclass();
                if (superclass == code.image.nil) {
                    return compiledInClass;
                } else {
                    return (ClassObject) superclass;
                }
            }
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int rawByte) {
            this(code, index, numBytecodes, rawByte & 31, rawByte >> 5);
        }

        public SingleExtendedSuperNode(final CompiledCodeObject code, final int index, final int numBytecodes, final int literalIndex, final int numArgs) {
            super(code, index, numBytecodes, code.getLiteral(literalIndex), numArgs);
            lookupClassNode = new SqueakLookupClassSuperNode(code);
        }

        @Override
        public String toString() {
            return "sendSuper: " + selector.asString();
        }
    }
}
