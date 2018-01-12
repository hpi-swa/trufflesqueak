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
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.MethodContextObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.CONTEXT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.POINT;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.PROCESS;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SEMAPHORE;
import de.hpi.swa.trufflesqueak.model.ObjectLayouts.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SpecialSelectorObject;
import de.hpi.swa.trufflesqueak.nodes.TopLevelContextNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.util.Display.AbstractDisplay;
import de.hpi.swa.trufflesqueak.util.Display.JavaDisplay;
import de.hpi.swa.trufflesqueak.util.Display.NullDisplay;
import de.hpi.swa.trufflesqueak.util.InterruptHandler;
import de.hpi.swa.trufflesqueak.util.OSDetector;
import de.hpi.swa.trufflesqueak.util.ProcessManager;
import de.hpi.swa.trufflesqueak.util.SqueakImageFlags;
import de.hpi.swa.trufflesqueak.util.SqueakImageReader;

public class SqueakImageContext {
    // Special objects
    public final NilObject nil = new NilObject(this);
    public final boolean sqFalse = false;
    public final boolean sqTrue = true;
    public final ListObject specialObjectsArray = new ListObject(this);
    public final PointersObject schedulerAssociation = new PointersObject(this);
    public final ClassObject characterClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject arrayClass = new ClassObject(this);
    public final PointersObject smalltalk = new PointersObject(this);
    public final NativeObject doesNotUnderstand = new NativeObject(this, (byte) 1);
    public final ListObject specialSelectors = new ListObject(this);
    public final NativeObject mustBeBoolean = new NativeObject(this, (byte) 1);
    public final ClassObject metaclass = new ClassObject(this);
    public final ClassObject methodContextClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final ClassObject stringClass = new ClassObject(this);
    public final ClassObject compiledMethodClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);
    public final ClassObject floatClass = new ClassObject(this);

    private final SqueakLanguage language;
    private final PrintWriter output;
    private final PrintWriter error;
    private final SqueakLanguage.Env env;

    // Special selectors
    public final SpecialSelectorObject plus = new SpecialSelectorObject(this, 1, 1, 1);
    public final SpecialSelectorObject minus = new SpecialSelectorObject(this, 1, 1, 2);
    public final SpecialSelectorObject lt = new SpecialSelectorObject(this, 1, 1, 3);
    public final SpecialSelectorObject gt = new SpecialSelectorObject(this, 1, 1, 4);
    public final SpecialSelectorObject le = new SpecialSelectorObject(this, 1, 1, 5);
    public final SpecialSelectorObject ge = new SpecialSelectorObject(this, 1, 1, 6);
    public final SpecialSelectorObject eq = new SpecialSelectorObject(this, 1, 1, 7);
    public final SpecialSelectorObject ne = new SpecialSelectorObject(this, 1, 1, 8);
    public final SpecialSelectorObject times = new SpecialSelectorObject(this, 1, 1, 9);
    public final SpecialSelectorObject divide = new SpecialSelectorObject(this, 1, 1, 10);
    public final SpecialSelectorObject modulo = new SpecialSelectorObject(this, 1, 1, 11);
    public final SpecialSelectorObject pointAt = new SpecialSelectorObject(this, 1, 1);
    public final SpecialSelectorObject bitShift = new SpecialSelectorObject(this, 1, 1, 17);
    public final SpecialSelectorObject floorDivide = new SpecialSelectorObject(this, 1, 1, 12);
    public final SpecialSelectorObject bitAnd = new SpecialSelectorObject(this, 1, 1, 14);
    public final SpecialSelectorObject bitOr = new SpecialSelectorObject(this, 1, 1, 15);
    public final SpecialSelectorObject at = new SpecialSelectorObject(this, 1, 1/* , 63 */);
    public final SpecialSelectorObject atput = new SpecialSelectorObject(this, 1, 2/* , 64 */);
    public final SpecialSelectorObject size_ = new SpecialSelectorObject(this, 1, 0/* , 62 */);
    public final SpecialSelectorObject next = new SpecialSelectorObject(this, 1, 0);
    public final SpecialSelectorObject nextPut = new SpecialSelectorObject(this, 1, 1);
    public final SpecialSelectorObject atEnd = new SpecialSelectorObject(this, 1, 0);
    public final SpecialSelectorObject equivalent = new SpecialSelectorObject(this, 1, 1, 110);
    public final SpecialSelectorObject klass = new SpecialSelectorObject(this, 1, 0, 111);
    public final SpecialSelectorObject blockCopy = new SpecialSelectorObject(this, 1, 1);
    public final SpecialSelectorObject value_ = new SpecialSelectorObject(this, 1, 0, 201);
    public final SpecialSelectorObject valueWithArg = new SpecialSelectorObject(this, 1, 1, 202);
    public final SpecialSelectorObject do_ = new SpecialSelectorObject(this, 1, 1);
    public final SpecialSelectorObject new_ = new SpecialSelectorObject(this, 1, 0);
    public final SpecialSelectorObject newWithArg = new SpecialSelectorObject(this, 1, 1);
    public final SpecialSelectorObject x = new SpecialSelectorObject(this, 1, 0);
    public final SpecialSelectorObject y = new SpecialSelectorObject(this, 1, 0);

    @CompilationFinal(dimensions = 1) public final SpecialSelectorObject[] specialSelectorsArray = new SpecialSelectorObject[]{
                    plus, minus, lt, gt, le, ge, eq, ne, times, divide, modulo, pointAt, bitShift,
                    floorDivide, bitAnd, bitOr, at, atput, size_, next, nextPut, atEnd, equivalent,
                    klass, blockCopy, value_, valueWithArg, do_, new_, newWithArg, x, y
    };

    @CompilationFinal public final SqueakConfig config;
    @CompilationFinal public final AbstractDisplay display;
    @CompilationFinal public final ObjectGraph objects = new ObjectGraph(this);
    @CompilationFinal public final OSDetector os = new OSDetector();
    @CompilationFinal public final ProcessManager process = new ProcessManager(this);
    @CompilationFinal public final SqueakImageFlags flags = new SqueakImageFlags();
    @CompilationFinal public final InterruptHandler interrupt = new InterruptHandler(this);
    @CompilationFinal public final long startUpMillis = System.currentTimeMillis();

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
    }

    public CallTarget getActiveContext() {
        PointersObject activeProcess = process.activeProcess();
        MethodContextObject activeContext = (MethodContextObject) activeProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, nil);
        output.println(String.format("Resuming active context for %s...", activeContext.at0(CONTEXT.METHOD)));
        return Truffle.getRuntime().createCallTarget(TopLevelContextNode.create(language, activeContext));
    }

    public CallTarget getCustomContext() {
        Object receiver = config.getReceiver();
        String selector = config.getSelector();
        ClassObject receiverClass = receiver instanceof Integer ? smallIntegerClass : nilClass;
        CompiledCodeObject lookupResult = (CompiledCodeObject) receiverClass.lookup(wrap(selector));
        if (lookupResult == null) {
            throw new RuntimeException(String.format("%s >> %s could not be found!", receiver, selector));
        }
        MethodContextObject customContext = MethodContextObject.createWriteableContextObject(this, lookupResult.frameSize());
        customContext.atput0(CONTEXT.METHOD, lookupResult);
        customContext.atput0(CONTEXT.INSTRUCTION_POINTER, customContext.getCodeObject().getBytecodeOffset() + 1);
        customContext.atput0(CONTEXT.RECEIVER, receiver);
        customContext.atput0(CONTEXT.STACKPOINTER, 1);
        customContext.atput0(CONTEXT.CLOSURE_OR_NIL, nil);
        customContext.setSender(nil);
        // if there were arguments, they would need to be pushed before the temps
        int numTemps = lookupResult.getNumTemps() - lookupResult.getNumArgsAndCopiedValues();
        for (int i = 0; i < numTemps; i++) {
            customContext.push(nil);
        }

        output.println(String.format("Starting to evaluate %s >> %s...", receiver, selector));
        return Truffle.getRuntime().createCallTarget(TopLevelContextNode.create(getLanguage(), customContext));
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
            return wrap((int) obj);
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
        throw new RuntimeException("Don't know how to wrap " + obj);
    }

    public Object wrap(boolean value) {
        return value ? sqTrue : sqFalse;
    }

    public BaseSqueakObject wrap(int i) {
        return wrap(BigInteger.valueOf(((Integer) i).intValue()));
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
        return newPoint((int) point.getX(), (int) point.getY());
    }

    public PointersObject wrap(Dimension dimension) {
        return newPoint((int) dimension.getWidth(), (int) dimension.getHeight());
    }

    public ListObject newList(Object... elements) {
        return new ListObject(this, arrayClass, elements);
    }

    public PointersObject newPoint(int xPos, int yPos) {
        ClassObject pointClass = (ClassObject) specialObjectsArray.at0(SPECIAL_OBJECT_INDEX.ClassPoint);
        PointersObject newPoint = (PointersObject) pointClass.newInstance(POINT.SIZE);
        newPoint.atput0(POINT.X, xPos);
        newPoint.atput0(POINT.Y, yPos);
        return newPoint;
    }

    public void registerSemaphore(BaseSqueakObject semaphore, int index) {
        specialObjectsArray.atput0(index, semaphore.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore) ? semaphore : nil);
    }

    public void synchronousSignal(VirtualFrame frame, PointersObject semaphore) {
        if (process.isEmptyList(semaphore)) { // no process is waiting on this semaphore
            semaphore.atput0(SEMAPHORE.EXCESS_SIGNALS, (int) semaphore.at0(SEMAPHORE.EXCESS_SIGNALS) + 1);
        } else {
            process.resumeProcess(frame, process.removeFirstLinkOfList(semaphore));
        }
    }
}
