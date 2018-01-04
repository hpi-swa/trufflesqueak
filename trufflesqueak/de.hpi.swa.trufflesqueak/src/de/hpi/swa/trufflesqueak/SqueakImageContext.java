package de.hpi.swa.trufflesqueak;

import java.awt.Dimension;
import java.awt.Point;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.Truffle;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SpecialSelector;
import de.hpi.swa.trufflesqueak.nodes.TopLevelContextNode;
import de.hpi.swa.trufflesqueak.nodes.context.ObjectGraph;
import de.hpi.swa.trufflesqueak.util.Constants.SPECIAL_OBJECT_INDEX;
import de.hpi.swa.trufflesqueak.util.Display.AbstractDisplay;
import de.hpi.swa.trufflesqueak.util.Display.JavaDisplay;
import de.hpi.swa.trufflesqueak.util.Display.NullDisplay;
import de.hpi.swa.trufflesqueak.util.KnownClasses.CONTEXT;
import de.hpi.swa.trufflesqueak.util.KnownClasses.POINT;
import de.hpi.swa.trufflesqueak.util.KnownClasses.PROCESS;
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
    public final SpecialSelector plus = new SpecialSelector(this, 1, 1, 1);
    public final SpecialSelector minus = new SpecialSelector(this, 1, 1, 2);
    public final SpecialSelector lt = new SpecialSelector(this, 1, 1, 3);
    public final SpecialSelector gt = new SpecialSelector(this, 1, 1, 4);
    public final SpecialSelector le = new SpecialSelector(this, 1, 1, 5);
    public final SpecialSelector ge = new SpecialSelector(this, 1, 1, 6);
    public final SpecialSelector eq = new SpecialSelector(this, 1, 1, 7);
    public final SpecialSelector ne = new SpecialSelector(this, 1, 1, 8);
    public final SpecialSelector times = new SpecialSelector(this, 1, 1, 9);
    public final SpecialSelector divide = new SpecialSelector(this, 1, 1, 10);
    public final SpecialSelector modulo = new SpecialSelector(this, 1, 1, 11);
    public final SpecialSelector pointAt = new SpecialSelector(this, 1, 1);
    public final SpecialSelector bitShift = new SpecialSelector(this, 1, 1, 17);
    public final SpecialSelector floorDivide = new SpecialSelector(this, 1, 1, 12);
    public final SpecialSelector bitAnd = new SpecialSelector(this, 1, 1, 14);
    public final SpecialSelector bitOr = new SpecialSelector(this, 1, 1, 15);
    public final SpecialSelector at = new SpecialSelector(this, 1, 1/* , 63 */);
    public final SpecialSelector atput = new SpecialSelector(this, 1, 2/* , 64 */);
    public final SpecialSelector size_ = new SpecialSelector(this, 1, 0/* , 62 */);
    public final SpecialSelector next = new SpecialSelector(this, 1, 0);
    public final SpecialSelector nextPut = new SpecialSelector(this, 1, 1);
    public final SpecialSelector atEnd = new SpecialSelector(this, 1, 0);
    public final SpecialSelector equivalent = new SpecialSelector(this, 1, 1, 110);
    public final SpecialSelector klass = new SpecialSelector(this, 1, 0, 111);
    public final SpecialSelector blockCopy = new SpecialSelector(this, 1, 1);
    public final SpecialSelector value = new SpecialSelector(this, 1, 0, 201);
    public final SpecialSelector valueWithArg = new SpecialSelector(this, 1, 1, 202);
    public final SpecialSelector do_ = new SpecialSelector(this, 1, 1);
    public final SpecialSelector new_ = new SpecialSelector(this, 1, 0);
    public final SpecialSelector newWithArg = new SpecialSelector(this, 1, 1);
    public final SpecialSelector x = new SpecialSelector(this, 1, 0);
    public final SpecialSelector y = new SpecialSelector(this, 1, 0);

    public final SpecialSelector[] specialSelectorsArray = new SpecialSelector[]{
                    plus, minus, lt, gt, le, ge, eq, ne, times, divide, modulo, pointAt, bitShift,
                    floorDivide, bitAnd, bitOr, at, atput, size_, next, nextPut, atEnd, equivalent,
                    klass, blockCopy, value, valueWithArg, do_, new_, newWithArg, x, y
    };

    public final SqueakConfig config;
    public final AbstractDisplay display;
    public final ObjectGraph objects = new ObjectGraph(this);
    public final OSDetector os = new OSDetector();
    public final ProcessManager process = new ProcessManager(this);
    public final SqueakImageFlags flags = new SqueakImageFlags();

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
        ContextObject activeContext = (ContextObject) activeProcess.at0(PROCESS.SUSPENDED_CONTEXT);
        activeProcess.atput0(PROCESS.SUSPENDED_CONTEXT, nil);
        output.println(String.format("Resuming active context for %s...", activeContext.at0(CONTEXT.METHOD)));
        return Truffle.getRuntime().createCallTarget(TopLevelContextNode.create(language, activeContext));
    }

    public CallTarget getCustomContext() {
        Object receiver = config.getReceiver();
        String selector = config.getSelector();
        ClassObject receiverClass = receiver instanceof Integer ? smallIntegerClass : nilClass;
        CompiledCodeObject lookupResult = (CompiledCodeObject) receiverClass.lookup(selector);

        ContextObject customContext = ContextObject.createWriteableContextObject(this, lookupResult.frameSize());
        customContext.atput0(CONTEXT.METHOD, lookupResult);
        customContext.atput0(CONTEXT.INSTRUCTION_POINTER, customContext.getCodeObject().getBytecodeOffset() + 1);
        customContext.atput0(CONTEXT.RECEIVER, receiver);
        customContext.atput0(CONTEXT.SENDER, nil);
        // newContext.atput0(CONTEXT.STACKPOINTER, 0); // not needed

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

    public BaseSqueakObject wrap(Object obj) {
        CompilerAsserts.neverPartOfCompilation();
        if (obj instanceof Integer) {
            return wrap(BigInteger.valueOf(((Integer) obj).intValue()));
        } else if (obj instanceof BigInteger) {
            return wrap((BigInteger) obj);
        } else if (obj instanceof Long) {
            return wrap(BigInteger.valueOf((long) obj).intValue());
        } else if (obj instanceof String) {
            return wrap((String) obj);
        } else if (obj instanceof Object[]) {
            return wrap((Object[]) obj);
        } else if (obj == null) {
            return nil;
        }
        throw new RuntimeException("Don't know how to wrap " + obj);
    }

    public BaseSqueakObject wrap(BigInteger i) {
        return new LargeInteger(this, i);
    }

    public NativeObject wrap(String s) {
        return new NativeObject(this, this.stringClass, s.getBytes());
    }

    public char wrap(char character) {
        return character;
    }

    public ListObject wrap(Object... elements) {
        return new ListObject(this, arrayClass, elements);
    }

    public PointersObject wrap(Point point) {
        return newPoint((int) point.getX(), (int) point.getY());
    }

    public PointersObject wrap(Dimension dimension) {
        return newPoint((int) dimension.getWidth(), (int) dimension.getHeight());
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
}
