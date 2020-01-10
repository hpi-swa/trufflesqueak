/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.graal.squeak.test;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;
import org.junit.ClassRule;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.graal.squeak.SqueakImage;
import de.hpi.swa.graal.squeak.SqueakLanguage;
import de.hpi.swa.graal.squeak.exceptions.ProcessSwitch;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.graal.squeak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.graal.squeak.image.SqueakImageContext;
import de.hpi.swa.graal.squeak.model.AbstractSqueakObject;
import de.hpi.swa.graal.squeak.model.CompiledCodeObject;
import de.hpi.swa.graal.squeak.model.CompiledMethodObject;
import de.hpi.swa.graal.squeak.model.ContextObject;
import de.hpi.swa.graal.squeak.model.NilObject;
import de.hpi.swa.graal.squeak.model.PointersObject;
import de.hpi.swa.graal.squeak.model.layout.ObjectLayouts.CONTEXT;
import de.hpi.swa.graal.squeak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageConfig;
import de.hpi.swa.graal.squeak.shared.SqueakLanguageOptions;
import de.hpi.swa.graal.squeak.util.ArrayUtils;
import de.hpi.swa.graal.squeak.util.FrameAccess;
import de.hpi.swa.graal.squeak.util.LogHandlerAccessor;

public abstract class AbstractSqueakTestCase {

    @ClassRule public static final TestLog.Rule TEST_LOG = new TestLog.Rule();

    protected static Context context;
    protected static SqueakImageContext image;
    protected static PointersObject nilClassBinding;

    protected static CompiledMethodObject makeMethod(final byte[] bytes, final Object[] literals) {
        return new CompiledMethodObject(image, bytes, literals);
    }

    protected static CompiledMethodObject makeMethod(final Object[] literals, final int... intbytes) {
        final byte[] bytes = new byte[intbytes.length + 1];
        for (int i = 0; i < intbytes.length; i++) {
            bytes[i] = (byte) intbytes[i];
        }
        bytes[intbytes.length] = 0; // Set flagByte = 0 for no method trailer.
        if (literals.length == 0 || literals[literals.length - 1] != nilClassBinding) {
            return makeMethod(bytes, ArrayUtils.copyWithLast(literals, nilClassBinding));
        }
        return makeMethod(bytes, literals);
    }

    protected static long makeHeader(final int numArgs, final int numTemps, final int numLiterals, final boolean hasPrimitive, final boolean needsLargeFrame) { // shortcut
        return CompiledCodeObject.makeHeader(numArgs, numTemps, numLiterals, hasPrimitive, needsLargeFrame);
    }

    protected CompiledMethodObject makeMethod(final int... intbytes) {
        return makeMethod(new Object[]{makeHeader(0, 5, 14, false, true), nilClassBinding}, intbytes);
    }

    protected static Object runMethod(final CompiledMethodObject code, final Object receiver, final Object... arguments) {
        final VirtualFrame frame = createTestFrame(code);
        Object result = null;
        try {
            result = createContext(code, receiver, arguments).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            fail("broken test");
        }
        return result;
    }

    protected ExecuteTopLevelContextNode createContext(final CompiledMethodObject code, final Object receiver) {
        return createContext(code, receiver, ArrayUtils.EMPTY_ARRAY);
    }

    protected static ExecuteTopLevelContextNode createContext(final CompiledMethodObject code, final Object receiver, final Object[] arguments) {
        final ContextObject testContext = ContextObject.create(image, code.getSqueakContextSize());
        testContext.atput0(CONTEXT.METHOD, code);
        testContext.atput0(CONTEXT.RECEIVER, receiver);
        testContext.atput0(CONTEXT.INSTRUCTION_POINTER, (long) code.getInitialPC());
        testContext.atput0(CONTEXT.STACKPOINTER, 0L);
        testContext.atput0(CONTEXT.CLOSURE_OR_NIL, NilObject.SINGLETON);
        testContext.atput0(CONTEXT.SENDER_OR_NIL, NilObject.SINGLETON);
        for (int i = 0; i < arguments.length; i++) {
            testContext.push(arguments[i]);
        }
        // Initialize temporary variables with nil in newContext.
        final int numTemps = code.getNumTemps();
        for (int i = 0; i < numTemps - arguments.length; i++) {
            testContext.push(NilObject.SINGLETON);
        }
        return ExecuteTopLevelContextNode.create(null, testContext, false);
    }

    protected Object runMethod(final Object receiver, final int... intbytes) {
        return runMethod(receiver, new AbstractSqueakObject[0], intbytes);
    }

    protected Object runMethod(final Object receiver, final Object[] arguments, final int... intbytes) {
        final CompiledMethodObject method = makeMethod(intbytes);
        return runMethod(method, receiver, arguments);
    }

    protected Object runBinaryPrimitive(final int primCode, final Object rcvr, final Object... arguments) {
        return runPrim(new Object[]{17104899L}, primCode, rcvr, arguments);
    }

    protected Object runQuinaryPrimitive(final int primCode, final Object rcvr, final Object... arguments) {
        return runPrim(new Object[]{68222979L}, primCode, rcvr, arguments);
    }

    protected Object runPrim(final Object[] literals, final int primCode, final Object rcvr, final Object... arguments) {
        final CompiledMethodObject method = makeMethod(literals, new int[]{139, primCode & 0xFF, (primCode & 0xFF00) >> 8});
        return runMethod(method, rcvr, arguments);
    }

    protected static VirtualFrame createTestFrame(final CompiledMethodObject code) {
        final Object[] arguments = FrameAccess.newWith(code, NilObject.SINGLETON, null, new Object[]{NilObject.SINGLETON});
        return Truffle.getRuntime().createVirtualFrame(arguments, code.getFrameDescriptor());
    }

    protected static SqueakImage loadImageContext(final String imagePath) {
        assert context == null && image == null;
        final Builder contextBuilder = Context.newBuilder();
        contextBuilder.allowAllAccess(true);
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.IMAGE_PATH, imagePath);
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.HEADLESS, "true");
        contextBuilder.option(SqueakLanguageConfig.ID + "." + SqueakLanguageOptions.TESTING, "true");
        final String logLevel = System.getProperty("log.level");
        if (logLevel != null) {
            contextBuilder.option("log." + SqueakLanguageConfig.ID + ".level", logLevel);
            contextBuilder.logHandler(LogHandlerAccessor.createLogHandler());
        }
        context = contextBuilder.build();
        context.initialize(SqueakLanguageConfig.ID);
        context.enter();
        try {
            image = SqueakLanguage.getContext();
            image.setImagePath(imagePath);
            if (Files.exists(Paths.get(imagePath))) {
                image.ensureLoaded();
            }
            return image.getSqueakImage(); // Pretend image has been loaded.
        } finally {
            context.leave();
        }
    }

    protected static void destroyImageContext() {
        // Close context if existing (for reloading mechanism).
        context.close(true);
        context = null;
        image = null;
        System.gc();
    }
}
