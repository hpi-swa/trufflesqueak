package de.hpi.swa.trufflesqueak;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.TrueObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakContextNode;
import de.hpi.swa.trufflesqueak.util.ImageReader;

public class SqueakImageContext {
    // Special objects
    public final NilObject nil = new NilObject();
    public final FalseObject sqFalse = new FalseObject();
    public final TrueObject sqTrue = new TrueObject();
    public final ListObject specialObjectsArray = new ListObject();
    public final PointersObject schedulerAssociation = new PointersObject();
    public final ClassObject characterClass = new ClassObject();
    public final ClassObject smallIntegerClass = new ClassObject();
    public final ClassObject arrayClass = new ClassObject();
    public final PointersObject smalltalk = new PointersObject();
    public final NativeObject doesNotUnderstand = new NativeObject();
    public final ListObject specialSelectors = new ListObject();
    public final NativeObject mustBeBoolean = new NativeObject();
    public final ClassObject metaclass = new ClassObject();

    private final SqueakLanguage language;
    private final BufferedReader input;
    private final PrintWriter output;
    private final SqueakLanguage.Env env;

    // Special selectors
    public final NativeObject plus = new NativeObject();
    public final NativeObject minus = new NativeObject();
    public final NativeObject lt = new NativeObject();
    public final NativeObject gt = new NativeObject();
    public final NativeObject le = new NativeObject();
    public final NativeObject ge = new NativeObject();
    public final NativeObject eq = new NativeObject();
    public final NativeObject ne = new NativeObject();
    public final NativeObject times = new NativeObject();
    public final NativeObject modulo = new NativeObject();
    public final NativeObject pointAt = new NativeObject();
    public final NativeObject bitShift = new NativeObject();
    public final NativeObject divide = new NativeObject();
    public final NativeObject bitAnd = new NativeObject();
    public final NativeObject bitOr = new NativeObject();
    public final NativeObject at = new NativeObject();
    public final NativeObject atput = new NativeObject();
    public final NativeObject size_ = new NativeObject();
    public final NativeObject next = new NativeObject();
    public final NativeObject nextPut = new NativeObject();
    public final NativeObject atEnd = new NativeObject();
    public final NativeObject equivalent = new NativeObject();
    public final NativeObject klass = new NativeObject();
    public final NativeObject blockCopy = new NativeObject();
    public final NativeObject value = new NativeObject();
    public final NativeObject valueWithArg = new NativeObject();
    public final NativeObject do_ = new NativeObject();
    public final NativeObject new_ = new NativeObject();
    public final NativeObject newWithArg = new NativeObject();
    public final NativeObject x = new NativeObject();
    public final NativeObject y = new NativeObject();
    public final NativeObject div = new NativeObject();

    public SqueakImageContext(SqueakLanguage squeakLanguage, SqueakLanguage.Env environ, BufferedReader in, PrintWriter out) {
        language = squeakLanguage;
        env = environ;
        input = in;
        output = out;
    }

    public CallTarget getActiveContext() {
        PointersObject scheduler = (PointersObject) schedulerAssociation.at0(1);
        PointersObject activeProcess = (PointersObject) scheduler.at0(1);
        ListObject activeContext = (ListObject) activeProcess.at0(1);
        activeProcess.atput0(1, nil);
        return Truffle.getRuntime().createCallTarget(new SqueakContextNode(language, activeContext));
    }

    public CallTarget getEntryPoint(BaseSqueakObject receiver, String selector) {
        ClassObject sqClass = (ClassObject) receiver.getSqClass();
        CompiledMethodObject lookup = (CompiledMethodObject) sqClass.lookup(selector);
        return lookup.getCallTarget();
    }

    public void fillInFrom(FileInputStream inputStream) throws IOException {
        ImageReader.readImage(this, inputStream);
    }

    public PrintWriter getOutput() {
        return output;
    }

    public BooleanObject wrapBool(boolean flag) {
        if (flag) {
            return sqTrue;
        } else {
            return sqFalse;
        }
    }

    public BaseSqueakObject wrapInt(int i) {
        return new SmallInteger(i);
    }

    public SqueakLanguage getLanguage() {
        return language;
    }
}
