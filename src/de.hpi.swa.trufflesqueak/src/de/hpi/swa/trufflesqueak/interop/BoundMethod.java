/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.interop;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.InteropSenderMarker;
import de.hpi.swa.trufflesqueak.nodes.dispatch.DispatchUneagerlyNode;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;

@ExportLibrary(InteropLibrary.class)
public final class BoundMethod implements TruffleObject {
    private final CompiledCodeObject method;
    private final AbstractSqueakObject receiver;

    public BoundMethod(final CompiledCodeObject method, final AbstractSqueakObject receiver) {
        this.method = method;
        this.receiver = receiver;
    }

    /*
     * INTEROPERABILITY
     */

    @SuppressWarnings("static-method")
    @ExportMessage
    public boolean isExecutable() {
        return true;
    }

    @ExportMessage
    public Object execute(final Object[] arguments,
                    @Cached final WrapToSqueakNode wrapNode,
                    @Cached final DispatchUneagerlyNode dispatchNode) throws ArityException {
        final int actualArity = arguments.length;
        final int expectedArity = method.getNumArgs(); // receiver + arguments
        if (actualArity == expectedArity) {
            return dispatchNode.executeDispatch(method, wrapNode.executeObjects(ArrayUtils.copyWithFirst(arguments, receiver)), InteropSenderMarker.SINGLETON);
        } else {
            throw ArityException.create(expectedArity, actualArity);
        }
    }
}
