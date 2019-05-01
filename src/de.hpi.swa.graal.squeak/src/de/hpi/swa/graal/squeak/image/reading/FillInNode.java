package de.hpi.swa.graal.squeak.image.reading;

import java.util.Arrays;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.Fallback;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.Node;

import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.ArrayObject;
import de.hpi.swa.graal.squeak.model.BlockClosureObject;
import de.hpi.swa.graal.squeak.model.ClassObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NativeObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.WeakPointersObject;
import de.hpi.swa.graal.squeak.nodes.accessing.ArrayObjectNodes.ArrayObjectWriteNode;
import de.hpi.swa.graal.squeak.nodes.accessing.WeakPointersObjectNodes.WeakPointersObjectWriteNode;

public abstract class FillInNode extends Node {
    private final SqueakImageContext image;

    protected FillInNode(final SqueakImageContext image) {
        this.image = image;
    }

    public static FillInNode create(final SqueakImageContext image) {
        return FillInNodeGen.create(image);
    }

    public abstract void execute(Object obj, SqueakImageChunk chunk);

    @Specialization
    protected static final void doBlockClosure(final BlockClosureObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @Specialization
    protected final void doClassObj(final ClassObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
        if (obj.size() > 6) {
            final String className = obj.getClassName();
            obj.setInstancesAreClasses(className);
            if ("Compiler".equals(className)) {
                obj.image.setCompilerClass(obj);
            } else if ("Parser".equals(className)) {
                obj.image.setParserClass(obj);
            } else if (!image.flags.is64bit() && "SmallFloat64".equals(className)) {
                obj.image.setSmallFloat(obj);
            }
        }
    }

    @Specialization
    protected static final void doCompiledCodeObj(final CompiledCodeObject obj, final SqueakImageChunk chunk) {
        obj.fillin(chunk);
    }

    @SuppressWarnings("unused")
    @Specialization
    protected static final void doContext(final ContextObject obj, final SqueakImageChunk chunk) {
        /** {@link ContextObject}s are filled in at a later stage by a {@link FillInContextNode}. */
    }

    @Specialization(guards = "obj.isShortType()")
    protected static final void doNativeShort(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.setStorage(chunk.getShorts());
    }

    @Specialization(guards = "obj.isIntType()")
    protected static final void doNativeInt(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.setStorage(chunk.getInts());
    }

    @Specialization(guards = "obj.isLongType()")
    protected static final void doNativeLong(final NativeObject obj, final SqueakImageChunk chunk) {
        obj.setStorage(chunk.getLongs());
    }

    @Specialization(guards = {"obj.isByteType()"})
    protected final void doNativeByteTesting(final NativeObject obj, final SqueakImageChunk chunk) {
        final byte[] stringBytes = chunk.getBytes();
        obj.setStorage(stringBytes);
        if (image.getDebugErrorSelector() == null && Arrays.equals(SqueakImageContext.DEBUG_ERROR_SELECTOR_NAME, stringBytes)) {
            image.setDebugErrorSelector(obj);
        } else if (image.getDebugSyntaxErrorSelector() == null && Arrays.equals(SqueakImageContext.DEBUG_SYNTAX_ERROR_SELECTOR_NAME, stringBytes)) {
            image.setDebugSyntaxErrorSelector(obj);
        }
    }

    @Specialization
    protected static final void doArrays(final ArrayObject obj, final SqueakImageChunk chunk,
                    @Cached final ArrayObjectWriteNode writeNode) {
        obj.setStorageAndSpecialize(chunk.getPointers(), writeNode);
    }

    @Specialization
    protected static final void doPointers(final PointersObject obj, final SqueakImageChunk chunk) {
        obj.setPointers(chunk.getPointers());
    }

    @Specialization
    protected static final void doWeakPointers(final WeakPointersObject obj, final SqueakImageChunk chunk,
                    @Cached final WeakPointersObjectWriteNode writeNode) {
        final Object[] pointers = chunk.getPointers();
        final int length = pointers.length;
        obj.setPointers(new Object[length]);
        for (int i = 0; i < length; i++) {
            writeNode.execute(obj, i, pointers[i]);
        }
    }

    @SuppressWarnings("unused")
    @Fallback
    protected static final void doNothing(final Object obj, final SqueakImageChunk chunk) {
        // do nothing
    }
}
