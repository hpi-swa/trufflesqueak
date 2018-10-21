package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.TypeSystem;

import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledBlockObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.FloatObject;
import de.hpi.swa.graal.squeak.model.LargeIntegerObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

@TypeSystem({boolean.class,
                char.class,
                long.class,
                double.class,
                ArrayObject.class,
                BlockClosureObject.class,
                ClassObject.class,
                CompiledBlockObject.class,
                CompiledMethodObject.class,
                ContextObject.class,
                FloatObject.class,
                LargeIntegerObject.class,
                NativeObject.class,
                PointersObject.class,
                WeakPointersObject.class,
                AbstractSqueakObject.class})
public abstract class SqueakTypes {
}
