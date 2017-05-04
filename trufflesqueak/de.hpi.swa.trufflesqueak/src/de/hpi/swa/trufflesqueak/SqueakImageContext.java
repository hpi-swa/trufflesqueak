package de.hpi.swa.trufflesqueak;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.utilities.CyclicAssumption;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.ImmediateCharacter;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.SqueakObject;
import de.hpi.swa.trufflesqueak.model.TrueObject;
import de.hpi.swa.trufflesqueak.nodes.roots.SqueakContextNode;
import de.hpi.swa.trufflesqueak.util.ImageReader;

public class SqueakImageContext {
    // Special objects
    public final NilObject nil = new NilObject(this);
    public final FalseObject sqFalse = new FalseObject(this);
    public final TrueObject sqTrue = new TrueObject(this);
    public final ListObject specialObjectsArray = new ListObject(this);
    public final PointersObject schedulerAssociation = new PointersObject(this);
    public final ClassObject characterClass = new ClassObject(this);
    public final ClassObject smallIntegerClass = new ClassObject(this);
    public final ClassObject arrayClass = new ClassObject(this);
    public final PointersObject smalltalk = new PointersObject(this);
    public final NativeObject doesNotUnderstand = new NativeObject(this);
    public final ListObject specialSelectors = new ListObject(this);
    public final NativeObject mustBeBoolean = new NativeObject(this);
    public final ClassObject metaclass = new ClassObject(this);
    public final SqueakObject methodContextClass = new ClassObject(this);
    public final ClassObject nilClass = new ClassObject(this);
    public final ClassObject trueClass = new ClassObject(this);
    public final ClassObject falseClass = new ClassObject(this);

    private final SqueakLanguage language;
    private final BufferedReader input;
    private final PrintWriter output;
    private final SqueakLanguage.Env env;

    // Special selectors
    public final NativeObject plus = new NativeObject(this);
    public final NativeObject minus = new NativeObject(this);
    public final NativeObject lt = new NativeObject(this);
    public final NativeObject gt = new NativeObject(this);
    public final NativeObject le = new NativeObject(this);
    public final NativeObject ge = new NativeObject(this);
    public final NativeObject eq = new NativeObject(this);
    public final NativeObject ne = new NativeObject(this);
    public final NativeObject times = new NativeObject(this);
    public final NativeObject modulo = new NativeObject(this);
    public final NativeObject pointAt = new NativeObject(this);
    public final NativeObject bitShift = new NativeObject(this);
    public final NativeObject divide = new NativeObject(this);
    public final NativeObject bitAnd = new NativeObject(this);
    public final NativeObject bitOr = new NativeObject(this);
    public final NativeObject at = new NativeObject(this);
    public final NativeObject atput = new NativeObject(this);
    public final NativeObject size_ = new NativeObject(this);
    public final NativeObject next = new NativeObject(this);
    public final NativeObject nextPut = new NativeObject(this);
    public final NativeObject atEnd = new NativeObject(this);
    public final NativeObject equivalent = new NativeObject(this);
    public final NativeObject klass = new NativeObject(this);
    public final NativeObject blockCopy = new NativeObject(this);
    public final NativeObject value = new NativeObject(this);
    public final NativeObject valueWithArg = new NativeObject(this);
    public final NativeObject do_ = new NativeObject(this);
    public final NativeObject new_ = new NativeObject(this);
    public final NativeObject newWithArg = new NativeObject(this);
    public final NativeObject x = new NativeObject(this);
    public final NativeObject y = new NativeObject(this);
    public final NativeObject div = new NativeObject(this);

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

    public CallTarget getEntryPoint() {
        String[] args = (String[]) env.getConfig().get("args");
        BaseSqueakObject receiver = nil;
        String selector = "testSum";
        switch (args.length) {
            case 1:
                receiver = nil;
                selector = args[0];
            case 2:
                switch (args[0]) {
                    case "nil":
                        receiver = nil;
                        break;
                    default:
                        receiver = new SmallInteger(this, Integer.parseInt(args[0]));
                }
                selector = args[1];
                break;
        }
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

    public SqueakLanguage getLanguage() {
        return language;
    }

    public SmallInteger wrapInt(int i) {
        return new SmallInteger(this, i);
    }

    public ImmediateCharacter wrapChar(int i) {
        return new ImmediateCharacter(this, i);
    }
}
