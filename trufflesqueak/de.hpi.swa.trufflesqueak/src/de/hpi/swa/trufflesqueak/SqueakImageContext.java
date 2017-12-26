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

import de.hpi.swa.trufflesqueak.io.AbstractDisplay;
import de.hpi.swa.trufflesqueak.io.Display;
import de.hpi.swa.trufflesqueak.io.NullDisplay;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SpecialSelector;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakContextNode;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakMainNode;
import de.hpi.swa.trufflesqueak.util.Constants.ASSOCIATION;
import de.hpi.swa.trufflesqueak.util.Constants.CONTEXT;
import de.hpi.swa.trufflesqueak.util.Constants.POINT_LAYOUT;
import de.hpi.swa.trufflesqueak.util.Constants.PROCESS;
import de.hpi.swa.trufflesqueak.util.Constants.PROCESS_SCHEDULER;
import de.hpi.swa.trufflesqueak.util.Constants.SPECIAL_OBJECT_INDEX;
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

    public SqueakImageContext(SqueakLanguage squeakLanguage, SqueakLanguage.Env environ,
                    PrintWriter out, PrintWriter err) {
        language = squeakLanguage;
        env = environ;
        output = out;
        error = err;
        if (env != null) {
            String[] applicationArguments = env.getApplicationArguments();
            config = new SqueakConfig(applicationArguments);
            if (config.getSelector() == null) {
                display = new Display();
            } else {
                display = new NullDisplay();
            }
        } else { // testing
            config = new SqueakConfig(new String[0]);
            display = new NullDisplay();
        }
    }

    public CallTarget getActiveContext() {
        PointersObject scheduler = (PointersObject) schedulerAssociation.at0(1);
        PointersObject activeProcess = (PointersObject) scheduler.at0(1);
        ContextObject activeContext = (ContextObject) activeProcess.at0(1);
        activeProcess.atput0(1, null);
        return Truffle.getRuntime().createCallTarget(new SqueakContextNode(language, activeContext));
    }

    public CallTarget getEntryPoint() {
        Object receiver = config.getReceiver();
        String selector = config.getSelector();
        SqueakMainNode mainNode;
        if (selector != null) {
            ClassObject receiverClass = receiver instanceof Integer ? smallIntegerClass : nilClass;
            CompiledCodeObject lookupResult = (CompiledCodeObject) receiverClass.lookup(selector);
            // Push literal 1, send literal 2 selector, return top
            byte[] bytes = new byte[]{32, (byte) 209, 124};
            Object[] literals = new Object[]{
                            0, receiver, lookupResult.getCompiledInSelector(), // selector
                            receiverClass // compiled in class
            };
            CompiledCodeObject entryPoint = new CompiledMethodObject(this, bytes, literals);
            output.println(String.format("Starting to evaluate %s >> %s:\n", receiver, selector));
            mainNode = new SqueakMainNode(getLanguage(), entryPoint);
        } else {
            display.open();
            PointersObject scheduler = (PointersObject) schedulerAssociation.at0(ASSOCIATION.VALUE);
            PointersObject process = (PointersObject) scheduler.at0(PROCESS_SCHEDULER.ACTIVE_PROCESS);
            ContextObject activeContext = (ContextObject) process.at0(PROCESS.SUSPENDED_CONTEXT);
            CompiledCodeObject code = (CompiledCodeObject) activeContext.at0(CONTEXT.METHOD);
            mainNode = new SqueakMainNode(getLanguage(), code, activeContext);
        }
        return Truffle.getRuntime().createCallTarget(mainNode);
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
        PointersObject newPoint = (PointersObject) pointClass.newInstance(POINT_LAYOUT.SIZE);
        newPoint.atput0(POINT_LAYOUT.X, xPos);
        newPoint.atput0(POINT_LAYOUT.Y, yPos);
        return newPoint;
    }

    public void registerSemaphore(BaseSqueakObject semaphore, int index) {
        specialObjectsArray.atput0(index, semaphore.isSpecialKindAt(SPECIAL_OBJECT_INDEX.ClassSemaphore) ? semaphore : nil);
    }
}
