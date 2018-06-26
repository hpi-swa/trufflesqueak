package de.hpi.swa.graal.squeak.nodes.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;

public final class ObjectGraph {
    private final HashSet<AbstractSqueakObject> classesWithNoInstances;
    private final PointersObject specialObjectsArray;

    public ObjectGraph(final SqueakImageContext image) {
        specialObjectsArray = image.specialObjectsArray;
        // TODO: BlockContext missing.
        final AbstractSqueakObject[] classes = new AbstractSqueakObject[]{image.smallIntegerClass, image.characterClass, image.floatClass};
        classesWithNoInstances = new HashSet<>(Arrays.asList(classes));
    }

    public HashSet<AbstractSqueakObject> getClassesWithNoInstances() {
        return classesWithNoInstances;
    }

    public List<AbstractSqueakObject> allInstances() {
        return traceInstances(null, false);
    }

    public List<AbstractSqueakObject> allInstances(final ClassObject classObj) {
        return traceInstances(classObj, false);
    }

    public List<AbstractSqueakObject> someInstance(final ClassObject classObj) {
        return traceInstances(classObj, true);
    }

    @TruffleBoundary
    private List<AbstractSqueakObject> traceInstances(final ClassObject classObj, final boolean isSomeInstance) {
        final List<AbstractSqueakObject> result = new ArrayList<>();
        final Set<AbstractSqueakObject> seen = new HashSet<>(1000000);
        final Deque<AbstractSqueakObject> pending = new ArrayDeque<>(256);
        pending.add(specialObjectsArray);
        while (!pending.isEmpty()) {
            final AbstractSqueakObject currentObject = pending.pop();
            if (!seen.contains(currentObject)) {
                seen.add(currentObject);
                final ClassObject sqClass = currentObject.getSqClass();
                if (classObj == null || classObj == sqClass) {
                    result.add(currentObject);
                    if (isSomeInstance) {
                        break;
                    }
                }
                pending.addAll(tracePointers(currentObject));
            }
        }
        return result;
    }

    private static List<AbstractSqueakObject> tracePointers(final AbstractSqueakObject currentObject) {
        final List<AbstractSqueakObject> result = new ArrayList<>(32);
        final ClassObject sqClass = currentObject.getSqClass();
        if (sqClass != null) {
            result.add(sqClass);
        }
        if (currentObject instanceof CompiledMethodObject) {
            addBaseSqueakObjects(result, ((CompiledMethodObject) currentObject).getLiterals());
        } else if (currentObject instanceof PointersObject) {
            addBaseSqueakObjects(result, ((PointersObject) currentObject).getPointers());
        } else if (currentObject instanceof ClassObject) {
            addBaseSqueakObjects(result, ((ClassObject) currentObject).getPointers());
        } else if (currentObject instanceof ContextObject) {
            addBaseSqueakObjects(result, ((ContextObject) currentObject).getPointers());
        } else if (currentObject instanceof WeakPointersObject) {
            addBaseSqueakObjects(result, ((WeakPointersObject) currentObject).getPointers());
        } else if (currentObject instanceof BlockClosureObject) {
            addBaseSqueakObjects(result, ((BlockClosureObject) currentObject).getTraceableObjects());
        }
        return result;
    }

    private static void addBaseSqueakObjects(final List<AbstractSqueakObject> list, final Object[] objects) {
        for (Object object : objects) {
            if (object instanceof AbstractSqueakObject && !(object instanceof NativeObject)) {
                list.add((AbstractSqueakObject) object);
            }
        }
    }
}
