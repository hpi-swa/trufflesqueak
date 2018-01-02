package de.hpi.swa.trufflesqueak.nodes.context;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;

import de.hpi.swa.trufflesqueak.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractPointersObject;
import de.hpi.swa.trufflesqueak.model.BaseSqueakObject;
import de.hpi.swa.trufflesqueak.model.BlockClosure;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledMethodObject;
import de.hpi.swa.trufflesqueak.model.ListObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;

public class ObjectGraph {
    @CompilationFinal private final HashSet<BaseSqueakObject> classesWithNoInstances;
    @CompilationFinal private final ListObject specialObjectsArray;

    public ObjectGraph(SqueakImageContext image) {
        specialObjectsArray = image.specialObjectsArray;
        // TODO: BlockContext missing.
        BaseSqueakObject[] classes = new BaseSqueakObject[]{image.smallIntegerClass, image.characterClass, image.floatClass};
        classesWithNoInstances = new HashSet<>(Arrays.asList(classes));
    }

    public HashSet<BaseSqueakObject> getClassesWithNoInstances() {
        return classesWithNoInstances;
    }

    public List<BaseSqueakObject> allInstances() {
        return traceInstances(null, false);
    }

    public List<BaseSqueakObject> allInstances(ClassObject classObj) {
        return traceInstances(classObj, false);
    }

    public List<BaseSqueakObject> someInstance(ClassObject classObj) {
        return traceInstances(classObj, true);
    }

    private List<BaseSqueakObject> traceInstances(ClassObject classObj, boolean isSomeInstance) {
        List<BaseSqueakObject> result = new ArrayList<>();
        Set<BaseSqueakObject> seen = new HashSet<>(1000000);
        Deque<BaseSqueakObject> pending = new ArrayDeque<>(256);
        pending.add(specialObjectsArray);
        while (!pending.isEmpty()) {
            BaseSqueakObject currentObject = pending.pop();
            if (!seen.contains(currentObject)) {
                seen.add(currentObject);
                ClassObject sqClass = currentObject.getSqClass();
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

    private static List<BaseSqueakObject> tracePointers(BaseSqueakObject currentObject) {
        List<BaseSqueakObject> result = new ArrayList<>(32);
        ClassObject sqClass = currentObject.getSqClass();
        if (sqClass != null) {
            result.add(sqClass);
        }
        if (currentObject instanceof CompiledMethodObject) {
            addBaseSqueakObjects(result, ((CompiledMethodObject) currentObject).getLiterals());
        } else if (currentObject instanceof AbstractPointersObject) {
            addBaseSqueakObjects(result, ((AbstractPointersObject) currentObject).getPointers());
        } else if (currentObject instanceof BlockClosure) {
            addBaseSqueakObjects(result, ((BlockClosure) currentObject).getTraceableObjects());
        }
        return result;
    }

    private static void addBaseSqueakObjects(List<BaseSqueakObject> list, Object[] objects) {
        for (Object object : objects) {
            if (object instanceof BaseSqueakObject && !(object instanceof NativeObject)) {
                list.add((BaseSqueakObject) object);
            }
        }
    }
}
