package de.hpi.swa.trufflesqueak;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;

import com.oracle.truffle.api.nodes.RootNode;

import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.TrueObject;
import de.hpi.swa.trufflesqueak.nodes.SqueakMethodNode;

public class SqueakImageContext {
    public final NilObject nil = new NilObject();
    public final FalseObject sqFalse = new FalseObject();
    public final TrueObject sqTrue = new TrueObject();
    public final ListObject specialObjectsArray = new ListObject();
    public final PointersObject schedulerAssociation = new PointersObject();
    public final PointersObject characterClass = new PointersObject();
    public final PointersObject smallIntegerClass = new PointersObject();
    public final PointersObject smalltalk = new PointersObject();

    public PointersObject metaclass = new PointersObject();

    private final BufferedReader input;
    private final PrintWriter output;
    private final SqueakLanguage.Env env;

    public SqueakImageContext(SqueakLanguage.Env env, BufferedReader in, PrintWriter out) {
        this.input = in;
        this.output = out;
        this.env = env;
    }

    public RootNode getActiveContext(SqueakLanguage language) {
        try {
            PointersObject scheduler = (PointersObject) schedulerAssociation.at0(1);
            PointersObject activeProcess = (PointersObject) scheduler.at0(1);
            ListObject activeContext = (ListObject) activeProcess.at0(1);
            activeProcess.atput0(1, nil);
            SmallInteger pc = (SmallInteger) activeContext.at0(1);
            CompiledMethodObject method = (CompiledMethodObject) activeContext.at0(3);
            return new SqueakMethodNode(language, method);
        } catch (InvalidIndex e) {
            output.println("Could not find active context");
        }
        return null;
    }

    public void fillInFrom(FileInputStream inputStream) throws IOException {
        ImageReader.readImage(this, inputStream);
    }
}
