/*
 * Copyright (c) 2017-2020 Software Architecture Group, Hasso Plattner Institute
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Paths;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Context.Builder;

import com.oracle.truffle.api.Truffle;
import com.oracle.truffle.api.frame.VirtualFrame;

import de.hpi.swa.trufflesqueak.SqueakImage;
import de.hpi.swa.trufflesqueak.SqueakLanguage;
import de.hpi.swa.trufflesqueak.exceptions.ProcessSwitch;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonLocalReturn;
import de.hpi.swa.trufflesqueak.exceptions.Returns.NonVirtualReturn;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.model.AbstractSqueakObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.nodes.ExecuteTopLevelContextNode;
import de.hpi.swa.trufflesqueak.shared.LogHandlerAccessor;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageOptions;
import de.hpi.swa.trufflesqueak.util.ArrayUtils;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

public abstract class AbstractSqueakTestCase {

    protected static Context context;
    protected static SqueakImageContext image;
    protected static PointersObject nilClassBinding;

    protected static CompiledCodeObject makeMethod(final byte[] bytes, final Object[] literals) {
        return new CompiledCodeObject(image, bytes, literals, image.compiledMethodClass);
    }

    protected static CompiledCodeObject makeMethod(final Object[] literals, final int... intbytes) {
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
        return CompiledCodeObject.makeHeader(true, numArgs, numTemps, numLiterals, hasPrimitive, needsLargeFrame);
    }

    protected CompiledCodeObject makeMethod(final int... intbytes) {
        return makeMethod(new Object[]{makeHeader(0, 5, 14, false, true), nilClassBinding}, intbytes);
    }

    protected static Object runMethod(final CompiledCodeObject code, final Object receiver, final Object... arguments) {
        final VirtualFrame frame = createTestFrame(code);
        Object result = null;
        try {
            result = createContext(code, receiver, arguments).execute(frame);
        } catch (NonLocalReturn | NonVirtualReturn | ProcessSwitch e) {
            fail("broken test");
        }
        return result;
    }

    protected ExecuteTopLevelContextNode createContext(final CompiledCodeObject code, final Object receiver) {
        return createContext(code, receiver, ArrayUtils.EMPTY_ARRAY);
    }

    protected static ExecuteTopLevelContextNode createContext(final CompiledCodeObject code, final Object receiver, final Object[] arguments) {
        final ContextObject testContext = ContextObject.create(image, code.getSqueakContextSize());
        testContext.setReceiver(receiver);
        testContext.setCodeObject(code);
        testContext.setInstructionPointer(code.getInitialPC());
        testContext.setStackPointer(0);
        testContext.removeSender();
        for (final Object argument : arguments) {
            testContext.push(argument);
        }
        // Initialize temporary variables with nil in newContext.
        final int numTemps = code.getNumTemps();
        for (int i = 0; i < numTemps - arguments.length; i++) {
            testContext.push(NilObject.SINGLETON);
        }
        return ExecuteTopLevelContextNode.create(image, null, testContext, false);
    }

    protected Object runMethod(final Object receiver, final int... intbytes) {
        return runMethod(receiver, new AbstractSqueakObject[0], intbytes);
    }

    protected Object runMethod(final Object receiver, final Object[] arguments, final int... intbytes) {
        final CompiledCodeObject method = makeMethod(intbytes);
        return runMethod(method, receiver, arguments);
    }

    protected static VirtualFrame createTestFrame(final CompiledCodeObject code) {
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
            contextBuilder.logHandler(LogHandlerAccessor.createLogHandler(System.getProperty("log.mode", "out")));
        }
        context = contextBuilder.build();
        context.initialize(SqueakLanguageConfig.ID);
        context.enter();
        try {
            image = SqueakLanguage.getContext();
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
