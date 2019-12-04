/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.nodes;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;

import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.VariablePointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.METHOD_DICT;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;

@ReportPolymorphism
public abstract class LookupMethodWithSelectorAndBreakpointNode extends AbstractLookupMethodWithSelectorNode {

    private final NativeObject selector;
    private final ClassObject breakpointClass;

    public static LookupMethodWithSelectorAndBreakpointNode create(final NativeObject selector, final ClassObject breakpointClass) {
        return LookupMethodWithSelectorAndBreakpointNodeGen.create(selector, breakpointClass);
    }

    protected LookupMethodWithSelectorAndBreakpointNode(final NativeObject selector, final ClassObject breakpointClass) {
        super();
        this.selector = selector;
        this.breakpointClass = breakpointClass;
    }

    @Override
    @Specialization(replaces = "doCached")
    protected final Object doUncached(final ClassObject classObject,
                    /**
                     * An AbstractPointersObjectReadNode is sufficient for accessing `values`
                     * instance variable here.
                     */
                    @Cached final AbstractPointersObjectReadNode readValuesNode) {
        ClassObject lookupClass = classObject;
        while (lookupClass != null) {
            final VariablePointersObject methodDict = lookupClass.getMethodDict();
            final Object[] methodDictVariablePart = methodDict.getVariablePart();
            for (int i = 0; i < methodDictVariablePart.length; i++) {
                if (selector == methodDictVariablePart[i]) {
                    if (lookupClass == breakpointClass) {
                        System.out.println("Reached breakpoint");
                    }
                    return readValuesNode.executeArray(methodDict, METHOD_DICT.VALUES).getObjectStorage()[i];
                }
            }
            lookupClass = lookupClass.getSuperclassOrNull();
        }
        assert !selector.isDoesNotUnderstand() : "Could not find does not understand method";
        return null; // Signals a doesNotUnderstand.
    }
}
