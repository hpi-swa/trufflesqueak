package de.hpi.swa.graal.squeak.nodes.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.graal.squeak.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractPointersObject;
import de.hpi.swa.graal.squeak.model.BaseSqueakObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ListObject;
import de.hpi.swa.graal.squeak.model.NativeObject;

public class ObjectGraph {
    @CompilationFinal private final HashSet<BaseSqueakObject> classesWithNoInstances;
    @CompilationFinal private final ListObject specialObjectsArray;

    public ObjectGraph(final SqueakImageContext image) {
        specialObjectsArray = image.specialObjectsArray;
        // TODO: BlockContext missing.
        final BaseSqueakObject[] classes = new BaseSqueakObject[]{image.smallIntegerClass, image.characterClass, image.floatClass};
        classesWithNoInstances = new HashSet<>(Arrays.asList(classes));
    }

    public HashSet<BaseSqueakObject> getClassesWithNoInstances() {
        return classesWithNoInstances;
    }

    public List<BaseSqueakObject> allInstances() {
        return traceInstances(null, false);
    }

    public List<BaseSqueakObject> allInstances(final ClassObject classObj) {
        return traceInstances(classObj, false);
    }

    public List<BaseSqueakObject> someInstance(final ClassObject classObj) {
        return traceInstances(classObj, true);
    }

    private List<BaseSqueakObject> traceInstances(final ClassObject classObj, final boolean isSomeInstance) {
        final List<BaseSqueakObject> result = new ArrayList<>();
        final Set<BaseSqueakObject> seen = new HashSet<>(1000000);
        final Deque<BaseSqueakObject> pending = new ArrayDeque<>(256);
        pending.add(specialObjectsArray);
        while (!pending.isEmpty()) {
            final BaseSqueakObject currentObject = pending.pop();
            if (!seen.contains(currentObject)) {
                seen.add(currentObject);
                final ClassObject sqClass = currentObject.getSqClass();
                if (classObj == null || classObj.equals(sqClass)) {
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

    private static List<BaseSqueakObject> tracePointers(final BaseSqueakObject currentObject) {
        final List<BaseSqueakObject> result = new ArrayList<>(32);
        final ClassObject sqClass = currentObject.getSqClass();
        if (sqClass != null) {
            result.add(sqClass);
        }
        if (currentObject instanceof CompiledMethodObject) {
            addBaseSqueakObjects(result, ((CompiledMethodObject) currentObject).getLiterals());
        } else if (currentObject instanceof AbstractPointersObject) {
            addBaseSqueakObjects(result, ((AbstractPointersObject) currentObject).getPointers());
        } else if (currentObject instanceof BlockClosureObject) {
            addBaseSqueakObjects(result, ((BlockClosureObject) currentObject).getTraceableObjects());
        }
        return result;
    }

    private static void addBaseSqueakObjects(final List<BaseSqueakObject> list, final Object[] objects) {
        for (Object object : objects) {
            if (object instanceof BaseSqueakObject && !(object instanceof NativeObject)) {
                list.add((BaseSqueakObject) object);
            }
        }
    }
}
