/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.nio.file.Path;

import org.graalvm.polyglot.Context;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import de.hpi.swa.trufflesqueak.interop.LookupMethodByStringNode;
import de.hpi.swa.trufflesqueak.image.SqueakImageContext;
import de.hpi.swa.trufflesqueak.image.SqueakImageReader;
import de.hpi.swa.trufflesqueak.shared.SqueakLanguageConfig;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.nodes.bytecodes.SmalltalkSistaV1Interpreter;
import de.hpi.swa.trufflesqueak.util.FrameAccess;

/** Differential checks against the established interpreter, using a SistaV1 development image. */
@SuppressWarnings("static-method")
public final class SistaV1BytecodeDSLImageTest extends AbstractSqueakTestCase {
    @BeforeClass
    public static void setUp() {
        final Path path = Path.of(System.getProperty("sista.image", "images/TruffleSqueak-25.0.1-dev.image"));
        Assume.assumeTrue("Specify -Dsista.image=<SistaV1 image> to run image regressions", path.toFile().isFile());
        context = Context.newBuilder().allowAllAccess(true).option("smalltalk.image-path", path.toAbsolutePath().toString()).option("smalltalk.headless", "true").build();
        context.initialize(SqueakLanguageConfig.ID);
        context.enter();
        image = SqueakImageContext.getSlow();
        SqueakImageReader.load(image);
    }

    @AfterClass
    public static void tearDown() {
        if (context != null) {
            context.leave();
            destroyImageContext();
        }
    }

    @Test
    public void smallIntegerMethods() {
        final long[] receivers = {0, 1, 2, 3, 7, 8, 15, 16, 127, 128, 255, 256, 257, 65535, 65536, 1073741823, 1L << 40, (1L << 60) - 1};
        for (final String selector : new String[]{"odd", "isPowerOfTwo", "bitCount", "hashMultiply"}) {
            final CompiledCodeObject method = (CompiledCodeObject) LookupMethodByStringNode.executeUncached(image.smallIntegerClass, selector);
            assertFalse("Expected SistaV1 bytecodes in " + selector, method.getHasV3PlusClosuresBytecodes());
            final var target = SmalltalkSistaV1Interpreter.build(method).getCallTarget();
            for (int iteration = 0; iteration < 2; iteration++) {
                for (final long receiver : receivers) {
                    final Object[] arguments = FrameAccess.newWith(NilObject.SINGLETON, null, receiver);
                    final Object expected = method.getCallTarget().call(arguments.clone());
                    assertEquals(selector + " for " + receiver, expected, target.call(arguments.clone()));
                }
            }
        }
    }
}
