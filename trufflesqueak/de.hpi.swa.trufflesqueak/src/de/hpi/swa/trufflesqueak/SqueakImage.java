package de.hpi.swa.trufflesqueak;

import de.hpi.swa.trufflesqueak.exceptions.InvalidIndex;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FalseObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.SmallInteger;
import de.hpi.swa.trufflesqueak.model.TrueObject;

public class SqueakImage {
    public static final NilObject nil = NilObject.SINGLETON;
    public static final FalseObject sqFalse = FalseObject.SINGLETON;
    public static final TrueObject sqTrue = TrueObject.SINGLETON;
    public static final ListObject specialObjectsArray = new ListObject();
    public static final PointersObject schedulerAssociation = new PointersObject();
    public static final PointersObject characterClass = new PointersObject();
    public static final PointersObject smallIntegerClass = new PointersObject();

    public void run() {
        try {
            PointersObject scheduler = (PointersObject) schedulerAssociation.at0(1);
            PointersObject activeProcess = (PointersObject) scheduler.at0(1);
            ListObject activeContext = (ListObject) activeProcess.at0(1);
            activeProcess.atput0(1, nil);
            SmallInteger pc = (SmallInteger) activeContext.at0(1);
            CompiledMethodObject method = (CompiledMethodObject) activeContext.at0(3);
        } catch (InvalidIndex e) {
            System.err.println("Could not find active context");
        }
    }
}
