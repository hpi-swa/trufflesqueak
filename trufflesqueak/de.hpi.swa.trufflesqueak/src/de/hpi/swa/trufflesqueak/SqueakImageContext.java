package de.hpi.swa.trufflesqueak;

import java.awt.Dimension;
import java.awt.Point;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.Frame;
import com.oracle.truffle.api.frame.FrameInstance;
import com.oracle.truffle.api.frame.FrameInstanceVisitor;

import de.hpi.swa.trufflesqueak.exceptions.SqueakException;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SpecialSelectorObject;
import de.hpi.swa.trufflesqueak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.nodes.process.GetActiveProcessNode;
import de.hpi.swa.trufflesqueak.util.Display.AbstractDisplay;
import de.hpi.swa.trufflesqueak.util.Display.JavaDisplay;
import de.hpi.swa.trufflesqueak.util.Display.NullDisplay;
import de.hpi.swa.trufflesqueak.util.FrameAccess;
import de.hpi.swa.trufflesqueak.util.FrameMarker;
import de.hpi.swa.trufflesqueak.util.InterruptHandlerNode;
import de.hpi.swa.trufflesqueak.util.OSDetector;
import de.hpi.swa.trufflesqueak.util.SqueakImageFlags;
import de.hpi.swa.trufflesqueak.util.SqueakImageReader;

public class SqueakImageContext {
    // Special objects
    @CompilationFinal public final NilObject nil = new NilObject(this);
    @CompilationFinal public final boolean sqFalse = false;
    @CompilationFinal public final boolean sqTrue = true;
    @CompilationFinal public final ListObject specialObjectsArray = new ListObject(this);
    @CompilationFinal public final PointersObject schedulerAssociation = new PointersObject(this);
    @CompilationFinal public final ClassObject characterClass = new ClassObject(this);
    @CompilationFinal public final ClassObject smallIntegerClass = new ClassObject(this);
    @CompilationFinal public final ClassObject arrayClass = new ClassObject(this);
    @CompilationFinal public final PointersObject smalltalk = new PointersObject(this);
    @CompilationFinal public final NativeObject doesNotUnderstand = new NativeObject(this, (byte) 1);
    @CompilationFinal public final ListObject specialSelectors = new ListObject(this);
    @CompilationFinal public final NativeObject mustBeBoolean = new NativeObject(this, (byte) 1);
    @CompilationFinal public final ClassObject metaclass = new ClassObject(this);
    @CompilationFinal public final ClassObject methodContextClass = new ClassObject(this);
    @CompilationFinal public final ClassObject nilClass = new ClassObject(this);
    @CompilationFinal public final ClassObject trueClass = new ClassObject(this);
    @CompilationFinal public final ClassObject falseClass = new ClassObject(this);
    @CompilationFinal public final ClassObject stringClass = new ClassObject(this);
    @CompilationFinal public final ClassObject compiledMethodClass = new ClassObject(this);
    @CompilationFinal public final ClassObject blockClosureClass = new ClassObject(this);
    @CompilationFinal public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    @CompilationFinal public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    @CompilationFinal public final ClassObject floatClass = new ClassObject(this);

    @CompilationFinal private final SqueakLanguage language;
    @CompilationFinal private final PrintWriter output;
    @CompilationFinal private final PrintWriter error;
    @CompilationFinal private final SqueakLanguage.Env env;

    // Special selectors
    @CompilationFinal public final SpecialSelectorObject plus = new SpecialSelectorObject(this, 1, 1, 1);
    @CompilationFinal public final SpecialSelectorObject minus = new SpecialSelectorObject(this, 1, 1, 2);
    @CompilationFinal public final SpecialSelectorObject lt = new SpecialSelectorObject(this, 1, 1, 3);
    @CompilationFinal public final SpecialSelectorObject gt = new SpecialSelectorObject(this, 1, 1, 4);
    @CompilationFinal public final SpecialSelectorObject le = new SpecialSelectorObject(this, 1, 1, 5);
    @CompilationFinal public final SpecialSelectorObject ge = new SpecialSelectorObject(this, 1, 1, 6);
    @CompilationFinal public final SpecialSelectorObject eq = new SpecialSelectorObject(this, 1, 1, 7);
    @CompilationFinal public final SpecialSelectorObject ne = new SpecialSelectorObject(this, 1, 1, 8);
    @CompilationFinal public final SpecialSelectorObject times = new SpecialSelectorObject(this, 1, 1, 9);
    @CompilationFinal public final SpecialSelectorObject divide = new SpecialSelectorObject(this, 1, 1, 10);
    @CompilationFinal public final SpecialSelectorObject modulo = new SpecialSelectorObject(this, 1, 1, 11);
    @CompilationFinal public final SpecialSelectorObject pointAt = new SpecialSelectorObject(this, 1, 1);
    @CompilationFinal public final SpecialSelectorObject bitShift = new SpecialSelectorObject(this, 1, 1, 17);
    @CompilationFinal public final SpecialSelectorObject floorDivide = new SpecialSelectorObject(this, 1, 1, 12);
    @CompilationFinal public final SpecialSelectorObject bitAnd = new SpecialSelectorObject(this, 1, 1, 14);
    @CompilationFinal public final SpecialSelectorObject bitOr = new SpecialSelectorObject(this, 1, 1, 15);
    @CompilationFinal public final SpecialSelectorObject at = new SpecialSelectorObject(this, 1, 1/* , 63 */);
    @CompilationFinal public final SpecialSelectorObject atput = new SpecialSelectorObject(this, 1, 2/* , 64 */);
    @CompilationFinal public final SpecialSelectorObject size_ = new SpecialSelectorObject(this, 1, 0/* , 62 */);
    @CompilationFinal public final SpecialSelectorObject next = new SpecialSelectorObject(this, 1, 0);
    @CompilationFinal public final SpecialSelectorObject nextPut = new SpecialSelectorObject(this, 1, 1);
    @CompilationFinal public final SpecialSelectorObject atEnd = new SpecialSelectorObject(this, 1, 0);
    @CompilationFinal public final SpecialSelectorObject equivalent = new SpecialSelectorObject(this, 1, 1, 110);
    @CompilationFinal public final SpecialSelectorObject klass = new SpecialSelectorObject(this, 1, 0, 111);
    @CompilationFinal public final SpecialSelectorObject blockCopy = new SpecialSelectorObject(this, 1, 1);
    @CompilationFinal public final SpecialSelectorObject value_ = new SpecialSelectorObject(this, 1, 0, 201);
    @CompilationFinal public final SpecialSelectorObject valueWithArg = new SpecialSelectorObject(this, 1, 1, 202);
    @CompilationFinal public final SpecialSelectorObject do_ = new SpecialSelectorObject(this, 1, 1);
    @CompilationFinal public final SpecialSelectorObject new_ = new SpecialSelectorObject(this, 1, 0);
    @CompilationFinal public final SpecialSelectorObject newWithArg = new SpecialSelectorObject(this, 1, 1);
    @CompilationFinal public final SpecialSelectorObject x = new SpecialSelectorObject(this, 1, 0);
    @CompilationFinal public final SpecialSelectorObject y = new SpecialSelectorObject(this, 1, 0);

    @CompilationFinal(dimensions = 1) public final SpecialSelectorObject[] specialSelectorsArray = new SpecialSelectorObject[]{
                    plus, minus, lt, gt, le, ge, eq, ne, times, divide, modulo, pointAt, bitShift,
                    floorDivide, bitAnd, bitOr, at, atput, size_, next, nextPut, atEnd, equivalent,
                    klass, blockCopy, value_, valueWithArg, do_, new_, newWithArg, x, y
    };

    @CompilationFinal public final SqueakConfig config;
    @CompilationFinal public final AbstractDisplay display;
    @CompilationFinal public final ObjectGraph objects = new ObjectGraph(this);
    @CompilationFinal public final OSDetector os = new OSDetector();
    @CompilationFinal public final SqueakImageFlags flags = new SqueakImageFlags();
    @CompilationFinal public final InterruptHandlerNode interrupt;
    @CompilationFinal public final long startUpMillis = System.currentTimeMillis();

    public BaseSqueakObject asSymbol = nil; // for testing

    public SqueakImageContext(SqueakLanguage squeakLanguage, SqueakLanguage.Env environ,
                    PrintWriter out, PrintWriter err) {
        language = squeakLanguage;
        env = environ;
        output = out;
        error = err;
        if (env != null) {
            String[] applicationArguments = env.getApplicationArguments();
            config = new SqueakConfig(applicationArguments);
            display = config.isCustomContext() ? new NullDisplay() : new JavaDisplay();
        } else { // testing
            config = new SqueakConfig(new String[0]);
            display = new NullDisplay();
        }
        interrupt = InterruptHandlerNode.create(this, config);
    }

    public CallTarget getActiveContext() {
        // TODO: maybe there is a better way to do the below
        PointersObject activeProcess = GetActiveProcessNode.create(new CompiledMethodObject(this)).executeGet();
        ContextObject activeContext = (ContextObject) activeProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, nil);
        output.println("Resuming active context for " + activeContext.getMethod() + "...");
        return Truffle.getRuntime().createCallTarget(ExecuteTopLevelContextNode.create(language, activeContext));
    }

    public CallTarget getCustomContext() {
        Object receiver = config.getReceiver();
        String selector = config.getSelector();
        ClassObject receiverClass = receiver instanceof Long ? smallIntegerClass : nilClass;
        CompiledCodeObject lookupResult = (CompiledCodeObject) receiverClass.lookup(selector);
        if (lookupResult.getCompiledInSelector() == doesNotUnderstand) {
            throw new SqueakException(receiver + " >> " + selector + " could not be found!");
        }
        ContextObject customContext = ContextObject.create(this, lookupResult.frameSize());
        customContext.atput0(CONTEXT.METHOD, lookupResult);
        customContext.atput0(CONTEXT.INSTRUCTION_POINTER, (long) customContext.getCodeObject().getInitialPC());
        customContext.atput0(CONTEXT.RECEIVER, receiver);
        customContext.atput0(CONTEXT.STACKPOINTER, 1L);
        customContext.atput0(CONTEXT.CLOSURE_OR_NIL, nil);
        customContext.setSender(nil);
        customContext.setFrameMarker(new FrameMarker());
        // if there were arguments, they would need to be pushed before the temps
        long numTemps = lookupResult.getNumTemps() - lookupResult.getNumArgsAndCopiedValues();
        for (int i = 0; i < numTemps; i++) {
            customContext.push(nil);
        }

        output.println("Starting to evaluate " + receiver + " >> " + selector + "...");
        return Truffle.getRuntime().createCallTarget(ExecuteTopLevelContextNode.create(getLanguage(), customContext));
    }

    public void fillInFrom(FileInputStream inputStream) throws IOException {
        SqueakImageReader.readImage(this, inputStream);
    }

    public PrintWriter getOutput() {
        return output;
    }

    public PrintWriter getError() {
        return error;
    }

    public SqueakLanguage getLanguage() {
        return language;
    }

    public Object wrap(Object obj) {
        CompilerAsserts.neverPartOfCompilation();
        if (obj == null) {
            return nil;
        } else if (obj instanceof Boolean) {
            return wrap((boolean) obj);
        } else if (obj instanceof Integer) {
            return wrap((long) obj);
        } else if (obj instanceof Long) {
            return wrap((long) obj);
        } else if (obj instanceof BigInteger) {
            return wrap((BigInteger) obj);
        } else if (obj instanceof String) {
            return wrap((String) obj);
        } else if (obj instanceof Character) {
            return wrap((char) obj);
        } else if (obj instanceof Object[]) {
            return wrap((Object[]) obj);
        } else if (obj instanceof Point) {
            return wrap((Point) obj);
        } else if (obj instanceof Dimension) {
            return wrap((Dimension) obj);
        }
        throw new SqueakException("Don't know how to wrap " + obj);
    }

    public Object wrap(boolean value) {
        return value ? sqTrue : sqFalse;
    }

    public BaseSqueakObject wrap(long l) {
        return wrap(BigInteger.valueOf(l));
    }

    public BaseSqueakObject wrap(BigInteger i) {
        return new LargeIntegerObject(this, i);
    }

    public NativeObject wrap(String s) {
        return new NativeObject(this, this.stringClass, s.getBytes());
    }

    public char wrap(char character) {
        return character;
    }

    public ListObject wrap(Object... elements) {
        Object[] wrappedElements = new Object[elements.length];
        for (int i = 0; i < elements.length; i++) {
            wrappedElements[i] = wrap(elements[i]);
        }
        return newList(wrappedElements);
    }

    public PointersObject wrap(Point point) {
        return newPoint((long) point.getX(), (long) point.getY());
    }

    public PointersObject wrap(Dimension dimension) {
        return newPoint((long) dimension.getWidth(), (long) dimension.getHeight());
    }

    public ListObject newList(Object... elements) {
        return new ListObject(this, arrayClass, elements);
    }

    public PointersObject newPoint(long xPos, long yPos) {
        ClassObject pointClass = (ClassObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassPoint);
        PointersObject newPoint = (PointersObject) pointClass.newInstance();
        newPoint.atput0(POINT.X, xPos);
        newPoint.atput0(POINT.Y, yPos);
        return newPoint;
    }

    public NativeObject newSymbol(String value) {
        return new NativeObject(this, doesNotUnderstand.getSqClass(), value.getBytes());
    }

    public void registerSemaphore(BaseSqueakObject semaphore, long index) {
        specialObjectsArray.atput0(index, semaphore.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore) ? semaphore : nil);
    }

    /*
     * Helper function for debugging purposes.
     */
    @TruffleBoundary
    public void printSqStackTrace() {
        getOutput().println("== Squeak stack trace ===========================================================");
        Truffle.getRuntime().iterateFrames(new FrameInstanceVisitor<Object>() {
            @Override
            public Object visitFrame(FrameInstance frameInstance) {
                Frame current = frameInstance.getFrame(FrameInstance.FrameAccess.READ_ONLY);
                if (current.getArguments().length < FrameAccess.RCVR_AND_ARGS_START) {
                    return null;
                }
                Object method = FrameAccess.getMethod(current);
                Object sender = FrameAccess.getSender(current);
                BlockClosureObject closure = FrameAccess.getClosure(current);
                Object[] arguments = FrameAccess.getArguments(current);
                String[] argumentStrings = new String[arguments.length];
                for (int i = 0; i < arguments.length; i++) {
                    argumentStrings[i] = arguments[i].toString();
                }
                getOutput().println(String.format("%s #(%s) [sender: %s, closure: %s]", method, String.join(", ", argumentStrings), sender, closure));
                return null;
            }
        });
        getOutput().println("=================================================================================");
    }
}
