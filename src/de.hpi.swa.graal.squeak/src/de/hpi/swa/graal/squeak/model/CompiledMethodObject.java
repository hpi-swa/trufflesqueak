/*
 * Copyright (c) 2017-2019 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.model;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Cached.Exclusive;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.interop.WrapToSqueakNode;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.ADDITIONAL_METHOD_STATE;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CLASS_BINDING;
import de.hpi.swa.graal.squeak.nodes.DispatchUneagerlyNode;
import de.hpi.swa.graal.squeak.nodes.ObjectGraphNode.ObjectTracer;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectReadNode;
import de.hpi.swa.graal.squeak.nodes.accessing.AbstractPointersObjectNodes.AbstractPointersObjectWriteNode;

@ExportLibrary(InteropLibrary.class)
public final class CompiledMethodObject extends CompiledCodeObject {

    public CompiledMethodObject(final SqueakImageContext image, final int hash) {
        super(image, hash, 0);
    }

    public CompiledMethodObject(final SqueakImageContext image, final byte[] bc, final Object[] lits) {
        super(image, 0, 0);
        literals = lits;
        decodeHeader();
        bytes = bc;
    }

    private CompiledMethodObject(final int size, final SqueakImageContext image) {
        super(image, 0, 0);
        bytes = new byte[size];
    }

    private CompiledMethodObject(final CompiledMethodObject original) {
        super(original);
    }

    public static CompiledMethodObject newOfSize(final SqueakImageContext image, final int size) {
        return new CompiledMethodObject(size, image);
    }

    public Object at0(final long longIndex) {
        final int index = (int) longIndex;
        if (index < getBytecodeOffset()) {
            assert index % image.flags.wordSize() == 0;
            return literals[index / image.flags.wordSize()];
        } else {
            final int realIndex = index - getBytecodeOffset();
            assert realIndex >= 0;
            return Byte.toUnsignedLong(bytes[realIndex]);
        }
    }

    @Override
    public String toString() {
        CompilerAsserts.neverPartOfCompilation();
        String className = "UnknownClass";
        String selector = "unknownSelector";
        final ClassObject methodClass = getMethodClass();
        if (methodClass != null) {
            className = methodClass.getClassName();
        }
        final NativeObject selectorObj = getCompiledInSelector();
        if (selectorObj != null) {
            selector = selectorObj.asStringUnsafe();
        }
        return className + ">>" + selector;
    }

    @Override
    public CompiledMethodObject getMethod() {
        return this;
    }

    public NativeObject getCompiledInSelector() {
        /**
         *
         * By convention the penultimate literal of a method is either its selector or an instance
         * of AdditionalMethodState. AdditionalMethodState holds the method's selector and any
         * pragmas and properties of the method. AdditionalMethodState may also be used to add
         * instance variables to a method, albeit ones held in the method's AdditionalMethodState.
         * Subclasses of CompiledMethod that want to add state should subclass AdditionalMethodState
         * to add the state they want, and implement methodPropertiesClass on the class side of the
         * CompiledMethod subclass to answer the specialized subclass of AdditionalMethodState.
         * Enterprising programmers are encouraged to try and implement this support automatically
         * through suitable modifications to the compiler and class builder.
         */
        CompilerAsserts.neverPartOfCompilation("Do not use getCompiledInSelector() in compiled code");
        final Object penultimateLiteral = literals[literals.length - 2];
        if (penultimateLiteral instanceof NativeObject) {
            return (NativeObject) penultimateLiteral;
        } else if (penultimateLiteral instanceof VariablePointersObject) {
            final VariablePointersObject penultimateLiteralAsPointer = (VariablePointersObject) penultimateLiteral;
            assert penultimateLiteralAsPointer.size() >= ADDITIONAL_METHOD_STATE.SELECTOR;
            return (NativeObject) penultimateLiteralAsPointer.instVarAt0Slow(ADDITIONAL_METHOD_STATE.SELECTOR);
        } else {
            return null;
        }
    }

    /** CompiledMethod>>#methodClassAssociation. */
    private PointersObject getMethodClassAssociation() {
        /**
         * From the CompiledMethod class description:
         *
         * The last literal in a CompiledMethod must be its methodClassAssociation, a binding whose
         * value is the class the method is installed in. The methodClassAssociation is used to
         * implement super sends. If a method contains no super send then its methodClassAssociation
         * may be nil (as would be the case for example of methods providing a pool of inst var
         * accessors).
         */
        return (PointersObject) literals[literals.length - 1];
    }

    public boolean hasMethodClass(final AbstractPointersObjectReadNode readNode) {
        return readNode.execute(getMethodClassAssociation(), CLASS_BINDING.VALUE) != NilObject.SINGLETON;
    }

    /** CompiledMethod>>#methodClass. */
    public ClassObject getMethodClass() {
        return getMethodClass(AbstractPointersObjectReadNode.getUncached());
    }

    public ClassObject getMethodClass(final AbstractPointersObjectReadNode readNode) {
        return (ClassObject) readNode.execute(getMethodClassAssociation(), CLASS_BINDING.VALUE);
    }

    /** CompiledMethod>>#methodClass:. */
    public void setMethodClass(final AbstractPointersObjectWriteNode writeNode, final ClassObject newClass) {
        writeNode.execute(getMethodClassAssociation(), CLASS_BINDING.VALUE, newClass);
    }

    public void setHeader(final long header) {
        literals = new Object[]{header};
        decodeHeader();
        literals = new Object[1 + numLiterals];
        literals[0] = header;
        for (int i = 1; i < literals.length; i++) {
            literals[i] = NilObject.SINGLETON;
        }
    }

    public CompiledMethodObject shallowCopy() {
        return new CompiledMethodObject(this);
    }

    public boolean isExceptionHandlerMarked() {
        return hasPrimitive() && primitiveIndex() == 199;
    }

    /*
     * Answer the program counter for the receiver's first bytecode.
     *
     */
    public int getInitialPC() {
        // pc is offset by header + numLiterals, +1 for one-based addressing
        return getBytecodeOffset() + 1;
    }

    @Override
    public int size() {
        return getBytecodeOffset() + bytes.length;
    }

    public void traceObjects(final ObjectTracer tracer) {
        for (final Object literal : getLiterals()) {
            tracer.addIfUnmarked(literal);
        }
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
    public Object execute(final Object[] receiverAndArguments,
                    @Exclusive @Cached final WrapToSqueakNode wrapNode,
                    @Exclusive @Cached final DispatchUneagerlyNode dispatchNode) throws ArityException {
        final int actualArity = receiverAndArguments.length;
        final int expectedArity = 1 + getNumArgs(); // receiver + arguments
        if (actualArity == expectedArity) {
            return dispatchNode.executeDispatch(this, wrapNode.executeObjects(receiverAndArguments), NilObject.SINGLETON);
        } else {
            throw ArityException.create(expectedArity, actualArity);
        }
    }
}
