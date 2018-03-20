package de.hpi.swa.trufflesqueak.nodes;

import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledBlockObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.LargeIntegerObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;

@TypeSystem({boolean.class,
                char.class,
                long.class,
                double.class,
                String.class,
                LargeIntegerObject.class,
                FloatObject.class,
                ClassObject.class,
                ListObject.class,
                PointersObject.class,
                BlockClosureObject.class,
                NativeObject.class,
                CompiledBlockObject.class,
                CompiledMethodObject.class,
                BaseSqueakObject.class})

public abstract class SqueakTypes {
}
