/*
 * Copyright (c) 2017-2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2021-2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import com.oracle.truffle.api.nodes.IndirectCallNode;

import de.hpi.swa.trufflesqueak.SqueakImage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive1;
import de.hpi.swa.trufflesqueak.nodes.primitives.Primitive.Primitive4;
import de.hpi.swa.trufflesqueak.nodes.primitives.PrimitiveNodeFactory;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageOptions;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class AbstractSqueakTestCase {
    protected static Context context;
    protected static SqueakImageContext image;
    protected static PointersObject nilClassBinding;

    protected static final CompiledCodeObject makeMethod(final byte[] bytes, final long header, final Object[] literals) {
        return new CompiledCodeObject(bytes, header, literals, image.compiledMethodClass);
    }

    protected static final CompiledCodeObject makeMethod(final long header, final Object[] literals, final int... intbytes) {
        final byte[] bytes = new byte[intbytes.length + 1];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        bytes[intbytes.length] = 0; // Set flagByte = 0 for no method trailer.
        final int numLiterals = (int) header & 0x7FFF;
        final Object[] allLiterals = Arrays.copyOf(literals, numLiterals);
        allLiterals[numLiterals - 2] = image.asByteString("DoIt"); // compiledInSelector
        allLiterals[numLiterals - 1] = nilClassBinding; // methodClassAssociation
        return makeMethod(bytes, header, allLiterals);
    }

    protected static final long makeHeader(final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) { // shortcut
        return CompiledCodeObject.CompiledCodeHeaderUtils.makeHeader(true, numArgs, numTemps, numLiterals, hasPrimitive, needsLargeFrame);
    }

    private static CompiledCodeObject makeMethod(final int... intbytes) {
        return makeMethod(makeHeader(0, 5, 14, false, true), new Object[0], intbytes);
    }

    protected static final Object runMethod(final CompiledCodeObject code, final Object receiver, final Object... arguments) {
        Object result = null;
        try {
            result = IndirectCallNode.getUncached().call(code.getCallTarget(), FrameAccess.newWith(NilObject.SINGLETON, null, receiver, arguments));
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            fail("broken test");
        }
        return result;
    }

    protected static final Object runMethod(final Object receiver, final int... intbytes) {
        return runMethod(receiver, new AbstractSqueakObject[0], intbytes);
    }

    protected static final Object runMethod(final Object receiver, final Object[] arguments, final int... intbytes) {
        return runMethod(makeMethod(intbytes), receiver, arguments);
    }

    protected static final Object runPrimitive(final int primCode, final Object rcvr, final Object arg1) {
        return ((Primitive1) PrimitiveNodeFactory.getOrCreateIndexed(primCode, 2)).execute(null, rcvr, arg1);
    }

    protected static final Object runPrimitive(final int primCode, final Object rcvr, final Object arg1, final Object arg2, final Object arg3, final Object arg4) {
        return ((Primitive4) PrimitiveNodeFactory.getOrCreateIndexed(primCode, 5)).execute(null, rcvr, arg1, arg2, arg3, arg4);
    }

    protected record TestImageSpec(String imagePath, boolean showStatistics) {
    }

    protected static final SqueakImage loadImageContext(final TestImageSpec spec) {
        assert context == null && image == null;
        final Builder contextBuilder = Context.newBuilder();
        contextBuilder.allowAllAccess(true);
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.IMAGE_PATH, spec.imagePath);
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.HEADLESS, "true");
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.TESTING, "true");
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.RESOURCE_SUMMARY, Boolean.toString(spec.showStatistics));

        final String logLevel = System.getProperty("log.level");
        if (logLevel != null) {
            contextBuilder.option("log." + SqueakLanguageConfig.ID + ".level", logLevel);
        }
        contextBuilder.option(// Log missing primitives
                        "log." + SqueakLanguageConfig.ID + ".primitives.level", "FINE");
        context = contextBuilder.build();
        context.initialize(SqueakLanguageConfig.ID);
        context.enter();
        try {
            image = SqueakImageContext.getSlow();
            if (Files.exists(Paths.get(spec.imagePath))) {
                image.ensureLoaded();
            }
            return image.getSqueakImage(); // Pretend image has been loaded.
        } finally {
            context.leave();
        }
    }

    protected static final void destroyImageContext() {
        // Close context if existing (for reloading mechanism).
        context.close(true);
        context = null;
        image = null;
        System.gc();
    }

    protected static final void println(final String text) {
        // Checkstyle: stop
        System.out.println(text);
        // Checkstyle: resume
    }
}
