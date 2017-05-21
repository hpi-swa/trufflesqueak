package de.hpi.swa.trufflesqueak;

import java.io.BufferedReader;
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
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.LargeInteger;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SqueakObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakContextNode;
import de.hpi.swa.trufflesqueak.util.ImageReader;

public class SqueakImageContext {
    // Special objects
    public final Object nil = null;
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
    public final SqueakObject methodContextClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);
    public final ClassObject stringClass = new ClassObject(this);
    public final ClassObject compiledMethodClass = new ClassObject(this);
    public final ClassObject blockClosureClass = new ClassObject(this);
    public final ClassObject largePositiveIntegerClass = new ClassObject(this);
    public final ClassObject largeNegativeIntegerClass = new ClassObject(this);

    private final SqueakLanguage language;
    private final BufferedReader input;
    private final PrintWriter output;
    private final SqueakLanguage.Env env;

    // Special selectors
    public final NativeObject plus = new NativeObject(this, (byte) 1);
    public final NativeObject minus = new NativeObject(this, (byte) 1);
    public final NativeObject lt = new NativeObject(this, (byte) 1);
    public final NativeObject gt = new NativeObject(this, (byte) 1);
    public final NativeObject le = new NativeObject(this, (byte) 1);
    public final NativeObject ge = new NativeObject(this, (byte) 1);
    public final NativeObject eq = new NativeObject(this, (byte) 1);
    public final NativeObject ne = new NativeObject(this, (byte) 1);
    public final NativeObject times = new NativeObject(this, (byte) 1);
    public final NativeObject modulo = new NativeObject(this, (byte) 1);
    public final NativeObject pointAt = new NativeObject(this, (byte) 1);
    public final NativeObject bitShift = new NativeObject(this, (byte) 1);
    public final NativeObject divide = new NativeObject(this, (byte) 1);
    public final NativeObject bitAnd = new NativeObject(this, (byte) 1);
    public final NativeObject bitOr = new NativeObject(this, (byte) 1);
    public final NativeObject at = new NativeObject(this, (byte) 1);
    public final NativeObject atput = new NativeObject(this, (byte) 1);
    public final NativeObject size_ = new NativeObject(this, (byte) 1);
    public final NativeObject next = new NativeObject(this, (byte) 1);
    public final NativeObject nextPut = new NativeObject(this, (byte) 1);
    public final NativeObject atEnd = new NativeObject(this, (byte) 1);
    public final NativeObject equivalent = new NativeObject(this, (byte) 1);
    public final NativeObject klass = new NativeObject(this, (byte) 1);
    public final NativeObject blockCopy = new NativeObject(this, (byte) 1);
    public final NativeObject value = new NativeObject(this, (byte) 1);
    public final NativeObject valueWithArg = new NativeObject(this, (byte) 1);
    public final NativeObject do_ = new NativeObject(this, (byte) 1);
    public final NativeObject new_ = new NativeObject(this, (byte) 1);
    public final NativeObject newWithArg = new NativeObject(this, (byte) 1);
    public final NativeObject x = new NativeObject(this, (byte) 1);
    public final NativeObject y = new NativeObject(this, (byte) 1);
    public final NativeObject div = new NativeObject(this, (byte) 1);
    private final CompiledCodeObject entryPoint;
    public final SqueakConfig config;

    private static final Object[] ENTRY_POINT_LITERALS = new Object[]{
                    0,
                    null, new NativeObject(null, null, "SendSelector".getBytes()),
                    new NativeObject(null, null, "TruffleSqueakEntryPoint".getBytes()), // selector
                    null // compiled in class
    };
    // Push literal 1, send literal 2 selector, return top
    private static final byte[] ENTRY_POINT_BYTES = new byte[]{32, (byte) 209, 124};

    public SqueakImageContext(SqueakLanguage squeakLanguage, SqueakLanguage.Env environ, BufferedReader in,
                    PrintWriter out) {
        language = squeakLanguage;
        env = environ;
        input = in;
        output = out;
        entryPoint = new CompiledMethodObject(this, ENTRY_POINT_BYTES, ENTRY_POINT_LITERALS);
        if (env != null) {
            config = (SqueakConfig) env.getConfig().get("config");
        } else {
            config = new SqueakConfig(new String[0]);
        }
    }

    public CallTarget getActiveContext() {
        PointersObject scheduler = (PointersObject) schedulerAssociation.at0(1);
        PointersObject activeProcess = (PointersObject) scheduler.at0(1);
        ListObject activeContext = (ListObject) activeProcess.at0(1);
        activeProcess.atput0(1, null);
        return Truffle.getRuntime().createCallTarget(new SqueakContextNode(language, activeContext));
    }

    public CallTarget getEntryPoint() {
        Object receiver = config.getReceiver();
        String selector = config.getSelector();
        ClassObject receiverClass = nilClass;
        if (receiver instanceof Integer) {
            receiverClass = smallIntegerClass;
        }
        CompiledCodeObject lookupResult = (CompiledCodeObject) receiverClass.lookup(selector);
        entryPoint.setLiteral(1, receiver);
        entryPoint.setLiteral(2, lookupResult.getCompiledInSelector());
        return entryPoint.getCallTarget();
    }

    public void fillInFrom(FileInputStream inputStream) throws IOException {
        ImageReader.readImage(this, inputStream);
    }

    public PrintWriter getOutput() {
        return output;
    }

    public SqueakLanguage getLanguage() {
        return language;
    }

    public BaseSqueakObject wrap(Object obj) {
        CompilerAsserts.neverPartOfCompilation();
        if (obj instanceof BigInteger) {
            return wrap((BigInteger) obj);
        } else if (obj instanceof String) {
            return wrap((String) obj);
        } else if (obj instanceof Object[]) {
            return wrap((Object[]) obj);
        }
        throw new RuntimeException("Don't know how to wrap " + obj);
    }

    public BaseSqueakObject wrap(BigInteger i) {
        return new LargeInteger(this, i);
    }

    public NativeObject wrap(String s) {
        return new NativeObject(this, this.stringClass, s.getBytes());
    }

    public ListObject wrap(Object[] elements) {
        return new ListObject(this, arrayClass, elements);
    }
}
