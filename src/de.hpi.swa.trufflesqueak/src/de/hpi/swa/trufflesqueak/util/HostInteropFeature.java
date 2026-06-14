/*
 * Copyright (c) 2026 Software Architecture Group, Hasso Plattner Institute
 * Copyright (c) 2026 Oracle and/or its affiliates
 *
 * Licensed under the MIT License.
 */
package de.hpi.swa.trufflesqueak.util;

import org.graalvm.nativeimage.dynamicaccess.AccessCondition;
import org.graalvm.nativeimage.dynamicaccess.ReflectiveAccess;
import org.graalvm.nativeimage.hosted.Feature;

import com.oracle.truffle.api.TruffleOptions;
import com.oracle.truffle.api.nodes.LanguageInfo;
import com.oracle.truffle.api.source.SourceSection;

import de.hpi.swa.trufflesqueak.interop.JavaObjectWrapper;
import de.hpi.swa.trufflesqueak.model.ArrayObject;
import de.hpi.swa.trufflesqueak.model.BlockClosureObject;
import de.hpi.swa.trufflesqueak.model.BooleanObject;
import de.hpi.swa.trufflesqueak.model.CharacterObject;
import de.hpi.swa.trufflesqueak.model.ClassObject;
import de.hpi.swa.trufflesqueak.model.CompiledCodeObject;
import de.hpi.swa.trufflesqueak.model.ContextObject;
import de.hpi.swa.trufflesqueak.model.EmptyObject;
import de.hpi.swa.trufflesqueak.model.FloatObject;
import de.hpi.swa.trufflesqueak.model.NativeObject;
import de.hpi.swa.trufflesqueak.model.NilObject;
import de.hpi.swa.trufflesqueak.model.PointersObject;
import de.hpi.swa.trufflesqueak.model.VariablePointersObject;
import de.hpi.swa.trufflesqueak.model.WeakVariablePointersObject;
import de.hpi.swa.trufflesqueak.model.layout.ObjectLayout;
import de.hpi.swa.trufflesqueak.model.layout.SlotLocation;

/** Registers Java types for reflection which is required for host interop. */
public class HostInteropFeature implements Feature {
    @Override
    public void afterRegistration(final AfterRegistrationAccess access) {
        final ReflectiveAccess reflection = access.getReflectiveAccess();
        for (final Class<?> cls : new Class<?>[]{
                        // General types
                        boolean.class, byte.class, char.class, short.class, int.class, long.class, float.class, double.class,
                        Boolean.class, Byte.class, Character.class, Short.class, Integer.class, Long.class, Float.class, Double.class,
                        Class.class, Object.class, String.class, RuntimeException.class,
                        // Common data structures
                        java.util.ArrayList.class, java.util.HashMap.class, java.util.HashSet.class, java.util.TreeSet.class, java.util.Arrays.class,
                        // Truffle types exposed by PolyglotPlugin
                        LanguageInfo.class, SourceSection.class,
                        // Truffle types
                        TruffleOptions.class,
                        // Non-abstract classes of TruffleSqueak model
                        ArrayObject.class, BlockClosureObject.class, BooleanObject.class, CharacterObject.class, ClassObject.class, CompiledCodeObject.class, ContextObject.class,
                        EmptyObject.class, FloatObject.class, NativeObject.class, NilObject.class, PointersObject.class, VariablePointersObject.class,
                        WeakVariablePointersObject.class,
                        // TruffleSqueak's object layout
                        ObjectLayout.class, SlotLocation.class,
                        // TruffleSqueak utils
                        MiscUtils.class,
                        // Java types used by interop
                        java.net.InetAddress.class,
                        java.nio.ByteBuffer.class,
                        java.time.Duration.class, java.time.LocalDate.class, java.time.LocalTime.class, java.time.Instant.class, java.time.ZoneId.class, java.time.ZoneOffset.class,
                        java.time.format.TextStyle.class, java.time.zone.ZoneRules.class,
                        java.util.Locale.class,
        }) {
            JavaObjectWrapper.populateAOT(cls);
            reflection.register(AccessCondition.unconditional(), cls);
            reflection.register(AccessCondition.unconditional(), cls.getConstructors());
            reflection.register(AccessCondition.unconditional(), cls.getMethods());
            reflection.register(AccessCondition.unconditional(), cls.getFields());
        }
        /*
         * System class needs special handling. In particular, its fields hold user-specific data
         * that should never be in the image heap.
         */
        reflection.register(AccessCondition.unconditional(), java.lang.System.class.getMethods());
    }
}
